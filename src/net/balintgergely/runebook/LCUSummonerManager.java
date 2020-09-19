package net.balintgergely.runebook;

import java.awt.EventQueue;
import java.net.http.HttpResponse;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.table.AbstractTableModel;

import net.balintgergely.runebook.LCUManager.LCUDataRoute;
import net.balintgergely.runebook.LCUManager.LCUModule;
import net.balintgergely.util.JSList;
import net.balintgergely.util.JSMap;
import net.balintgergely.util.JSON;
import net.balintgergely.util.JSONBodySubscriber;

/**
 * LCUSummonerManager maintains a small database of summoners encountered in-game
 * as part of a premade group or teammates with drafting. It is responsible for
 * fetching and storing their relationships with champions like mastery or what champions
 * do they own. This class has two modes: Detailed and non-detailed. In detailed mode
 * the class makes additional client callbacks to fill out information not received through events.
 * In non-detailed mode it does not.
 */

public class LCUSummonerManager extends AbstractTableModel implements Comparator<Champion>,LCUModule{
	private static final long serialVersionUID = 1L;
	/**
	 * State space:
	 * Mastery 0,1,2,3,4: Not owned
	 * 
	 */
	public static final int		MASTERY_MASK =		0x7,
								MASTERY_CHEST =		0x8,
								KNOWN_OWNED =		0x10,
								KNOWN_NOT_OWNED =	0x20,
								HOVERED	=			0x40,
								
								FREE_ROTATION =		0x100,
								BANNED =			0x200,
								OWNED_LOCALLY =		0x400,
								PICKED_BY_ENEMY =	0x800,
								
								UNAVAILABLE =		0xA00;
	
	private static final int	GS_FREE_ROTATION =	0x1,
								GS_BANNED =			0x2,
								GS_OWNED =			0x4,
								GS_PICKED_BY_ENEMY =0x8,
								
								GS_UNAVAILABLE =	0xA;

	private static int indexOfChampion(List<? extends Champion> championList,int key){
		int min = 0;
		int max = championList.size() - 1;
		while (min <= max) {
			int mid = (min + max) >>> 1;
			int midKey = championList.get(mid).key;
			if(midKey < key){
			    min = mid + 1;
			}else if(midKey > key){
			    max = mid - 1;
			}else{
			    return mid;
			}
		}
		return -1;
	}
	/*
	 * Information about who owns which champions is difficult to get. For the current summoner
	 * we are able to just fetch the list of owned champions. For other summoners it is trickier.
	 * In order to present a fine table of possible trades we do the following:
	 * 
	 * -Mark the champions part of the weekly free rotation as available to summoners above level 10.
	 * -For each summoner, mark all champions on at least mastery level 5 or with the chest granted flag as owned.
	 * -If a summoner hovers any champion not part of the weekly free rotation, we know they own that champion.
	 * 
	 * This thing below might look like a bunch of spaghetti but it actually has a systematic flow.
	 * A fundamental concept when understanding how this thing works is followed.
	 * We process information on effectively two separate threads.
	 * -EDT Thread is the EventDispatchThread of the AWT event queue.
	 * -LCU Thread is always the thread that holds the monitor lock on summonerCache.
	 * Since we can only invoke updates on the event queue thread, we perform structural changes on it as well.
	 * 
	 * The list of champions and therefore the rows never change. They can be freely updated on the LCU
	 * thread and only the notification of their update is passed to the EDT.
	 * The global states of champions can be treated as volatile and they can change while the EDT is processing an event we generated.
	 * 
	 * The list of summoners do however change. Since this list dictates the columns, it is always
	 * modified on the EDT.
	 * 
	 * In practice during an active session the summoner logged in does not change.
	 * WHO is the local summoner is updated on the LCU threads.
	 * WHERE in the list is the current summoner is of course managed by EDT.
	 * The "pghl" algorythm treats the local summoner specially and should be fairly tolerant of it concurrently changing.
	 */
	private static class Summoner{
		private byte[] rt;
		private String name;
		private final int summonerId;
		private int hoveredChampionIndex = -1;
		private boolean isDetailed = false;
		private boolean isLockedIn = false;
		private boolean isExcludedFromFreeRotation = false;
		private int pghl = 0;
		private Summoner(int id){
			this.summonerId = id;
		}
		private boolean pghlEqualsZero(){
			return pghl == 0;
		}
	}
	private LCUManager theManager;
	LCUSummonerManager(LCUManager md){
		this.theManager = md;
	}
	@Override
	public Map<LCUDataRoute, Consumer<Object>> getDataRoutes() {
		return Map.of(
				new LCUDataRoute("lol-lobby/v2/lobby","OnJsonApiEvent_lol-lobby_v2_lobby",true),
				this::lobbyChanged,
				new LCUDataRoute("lol-champ-select/v1/session","OnJsonApiEvent_lol-champ-select_v1_session",true),
				this::sessionChanged,
				new LCUDataRoute("lol-champions/v1/owned-champions-minimal","OnJsonApiEvent_lol-champions_v1_owned-champions-minimal",false),
				this::championsChanged
				);
	}
	private List<Champion> championList;
	//MODIFIED ON LCU THREAD
	private byte[] globalStates;
	private volatile Summoner mySummoner;
	//MODIFIED ON EDT THREAD
	private boolean detailedMode;
	private JSMap sessionData;
	private JSList championsData;
	//MODIFIED ON LCU THREAD BUT ALSO ACCESSED BY EDT THREAD
	private ConcurrentHashMap<Integer,Summoner> summonerCache = new ConcurrentHashMap<>();
	/**
	 * We have exactly 10 slots. First N slots are always the last active team of size N.
	 */
	private Summoner[] myTeam = new Summoner[10];
	private int sessionId = Integer.MIN_VALUE;
	private int pghlProgress;
	private int teamSize;
	
	/**
	 * Updates the summoner list based on the JSList of summoners provided.
	 */
	private void updateTeam(JSList lst,boolean forceEvent){
		EventQueue.invokeLater(() -> {
			teamSize = lst.size();
			boolean pghlChanged = false;
			for(int summonerIndex = 0;summonerIndex < teamSize;summonerIndex++){
				JSMap summonerData = lst.getJSMap(summonerIndex);
				int id = summonerData.peekInt("summonerId");
				if(id != 0){
					Summoner sm = summonerCache.get(id);
					//All low and behold the 'pghl' algorythm is now fully embedded in one single "updateTeam" method!
					//What this does is update the myTeam array by placing the specified summoner on the specified index.
					sm.pghl = ++pghlProgress;
					Summoner sm0 = myTeam[summonerIndex];
					if(sm0 != sm){
						for(int s = 0;s < myTeam.length;s++){
							if(myTeam[s] == sm || myTeam[s] == null){
								myTeam[s] = sm0;
								sm0 = null;
							}
						}
						int index1 = teamSize;
						if(sm0 != null && index1 < myTeam.length){
							int minIndex = -1;
							int minPghl = sm0 == mySummoner ? Integer.MAX_VALUE : sm0.pghl;
							while(index1 < myTeam.length){
								Summoner sm1 = myTeam[index1];
								if(sm1.pghl < minPghl){
									minPghl = sm1.pghl;
									minIndex = index1;
								}
								index1++;
							}
							if(minIndex >= 0){
								myTeam[minIndex] = sm0;
							}else if(sm0 != mySummoner){
								sm0.pghl = 0;
								sm0.isDetailed = false;
							}
						}
						myTeam[summonerIndex] = sm;
						if(detailedMode){
							sm.isDetailed = true;
							fetchMastery(sm.summonerId);
							if(sm.name == null){
								fetchSummonerData(sm.summonerId);
							}
						}
						pghlChanged = true;
					}
				}
			}
			if(pghlChanged || forceEvent){
				fireTableRowsUpdated();
			}
			if(pghlChanged){
				fireStateChanged();
			}
		});
	}
	private Summoner newSummoner(int id){
		Summoner sm = new Summoner(id);
		if(championList != null){
			sm.rt = new byte[globalStates.length];
		}
		return sm;
	}
	public void setChampionList(List<Champion> champions){
		synchronized(summonerCache){
			if(championList == null){
				int length = champions.size();
				championList = champions;
				globalStates = new byte[length];
				for(Summoner sm : summonerCache.values()){
					sm.rt = new byte[length];
				}
				championsChanged();
				sessionChanged();
			}else{
				throw new IllegalStateException("Champion list already set!");
			}
		}
	}
	public void setDetailed(boolean enabled){
		assert EventQueue.isDispatchThread();
		if(enabled != detailedMode){
			if(enabled){
				if(championList == null){
					throw new IllegalStateException("Model not ready!");
				}
				detailedMode = true;
				for(Summoner sm : myTeam){
					if(sm != null){
						if(!sm.isDetailed){
							sm.isDetailed = true;
							fetchMastery(sm.summonerId);
							if(sm.name == null){
								fetchSummonerData(sm.summonerId);
							}
						}
					}
				}
			}else{
				detailedMode = false;
			}
		}
	}
	public int getNPreferredChampions(int[] array){
		assert EventQueue.isDispatchThread();
		int index = 0;
		if(array.length == index || mySummoner == null){
			return index;
		}
		boolean imbl = mySummoner.isExcludedFromFreeRotation;
		if(mySummoner.hoveredChampionIndex >= 0){
			array[index++] = mySummoner.hoveredChampionIndex;
			if(array.length == index){
				return index;
			}
		}
		for(int i = 0;i < teamSize;i++){
			Summoner sm = myTeam[i];
			a: if(sm != null && sm != mySummoner){
				int hoverIndex = sm.hoveredChampionIndex;
				if(hoverIndex >= 0 && (
						 ((mySummoner.rt[hoverIndex] & KNOWN_OWNED) != 0) ||
						!(imbl || (globalStates[hoverIndex] & GS_FREE_ROTATION) == 0)
				)){
					for(int dex = 0;dex < index;dex++){
						if(array[dex] == hoverIndex){
							break a;
						}
					}
					array[index++] = hoverIndex;
					if(array.length == index){
						return index;
					}
				}
			}
		}
		return index;
	}
	public int getTeamSize(){
		return teamSize;
	}
	//Updates the current summoner on the local thread.
	void updateCurrentSummoner(Object obj){
		synchronized(summonerCache){
			JSMap jsm = JSON.toJSMap(obj);
			int id = jsm.peekInt("summonerId");
			if(id != 0 && (mySummoner == null || mySummoner.summonerId != id)){
				mySummoner = summonerCache.computeIfAbsent(id, this::newSummoner);
				if(mySummoner.pghl == 0){
					mySummoner.pghl = 1;
				}
				mySummoner.name = jsm.getString("displayName");
				mySummoner.isExcludedFromFreeRotation = (jsm.getInt("summonerLevel") <= 10);
			}
		}
	}
	//Updates the availability of champions on the local thread.
	void championsChanged(Object obj){
		synchronized(summonerCache){
			championsData = JSON.asJSList(obj);
			championsChanged();
		}
	}
	private void championsChanged(){
		assert Thread.holdsLock(summonerCache);
		if(championList != null){
			JSList chd = championsData;
			if(chd != null){
				for(int i = 0;i < globalStates.length;i++){
					globalStates[i] &= GS_BANNED;
				}
				for(Object champion : chd){
					JSMap championData = JSON.asJSMap(champion);
					Integer id = championData.getInt("id");
					int index = indexOfChampion(championList, id);
					if(index >= 0){
						if(championData.getBoolean("freeToPlay")){
							globalStates[index] |= GS_FREE_ROTATION;
						}
						JSMap ownership = JSON.toJSMap(championData.peek("ownership"));
						if(ownership.peekBoolean("owned") || JSON.toJSMap(ownership.peek("rental")).peekBoolean("rented")){
							globalStates[index] |= GS_OWNED;
						}
					}
				}
				if(mySummoner != null){
					for(int i = 0;i < globalStates.length;i++){
						if((globalStates[i] & GS_OWNED) == 0){
							mySummoner.rt[i] = (byte)((mySummoner.rt[i] & ~KNOWN_OWNED) | KNOWN_NOT_OWNED);
						}else{
							mySummoner.rt[i] = (byte)((mySummoner.rt[i] & ~KNOWN_NOT_OWNED) | KNOWN_OWNED);
						}
					}
				}
				EventQueue.invokeLater(() -> fireTableRowsUpdated(0, globalStates.length-1));
			}
		}
	}
	//Updates the lobby state on the event queue thread.
	private void lobbyChanged(Object obj){
		if(obj instanceof JSMap){
			synchronized(summonerCache){
				if(sessionId == Integer.MIN_VALUE){
					JSList lst = JSON.toJSList(((JSMap)obj).peek("members"));
					if(!lst.isEmpty()){
						for(Object summoner : lst){
							JSMap summonerData = JSON.toJSMap(summoner);
							int id = summonerData.peekInt("summonerId");
							if(id != 0){
								Summoner sm = summonerCache.computeIfAbsent(id, this::newSummoner);
								String name = summonerData.peekString("summonerName");
								if(name != null){
									sm.name = name;
								}
							}
						}
						updateTeam(lst, false);
					}
				}
			}
		}
	}
	//Updates the current session using both the local and the event queue threads.
	private void sessionChanged(Object obj){
		synchronized(summonerCache){
			sessionData = obj == null ? null : JSON.asJSMap(obj);
			sessionChanged();
		}
	}
	private void sessionChanged(){
		assert Thread.holdsLock(summonerCache);
		if(championList != null){
			JSMap sessionLocal = sessionData;
			if(sessionLocal == null){
				sessionId = Integer.MIN_VALUE;
			}else{
				int gameId = sessionLocal.peekInt("gameId");
				JSList team = sessionLocal.getJSList("myTeam");
				int ts = team.size();
				if(gameId != sessionId){
					for(int i = 0;i < globalStates.length;i++){
						globalStates[i] &= ~GS_UNAVAILABLE;
					}
					sessionId = gameId;
				}
				for(Object actions : sessionLocal.getJSList("actions")){
					for(Object action : JSON.toJSList(actions)){
						JSMap actionData = JSON.toJSMap(action);
						if(actionData.peekBoolean("completed")){
							int dex = indexOfChampion(championList, actionData.peekInt("championId"));
							if(dex >= 0)switch(actionData.peekString("type","")){
							case "ban":
								globalStates[dex] |= GS_BANNED;
								break;
							case "pick":
								if(!actionData.peekBoolean("isAllyAction",true)){
									globalStates[dex] |= GS_PICKED_BY_ENEMY;
								}
								break;
							}
						}
					}
				}
				boolean rowsChanged = false;
				for(int summonerIndex = 0;summonerIndex < ts;summonerIndex++){
					JSMap summonerData = team.getJSMap(summonerIndex);
					int id = summonerData.peekInt("summonerId");
					if(id != 0){
						Summoner summoner = summonerCache.computeIfAbsent(id, this::newSummoner);
						int championId = summonerData.peekInt("championId");
						int champion = -1;
						if(championId > 0){
							champion = indexOfChampion(championList, championId);
						}
						if(champion < 0){
							champion = indexOfChampion(championList, championId = summonerData.peekInt("championPickIntent"));
						}
						if(champion < 0){
							champion = -1;
						}
						if(summoner.hoveredChampionIndex != champion){
							summoner.hoveredChampionIndex = champion;
							if(champion >= 0){
								if(summoner != mySummoner && (globalStates[champion] & GS_FREE_ROTATION) == 0){
									summoner.rt[champion] |= KNOWN_OWNED;
								}
							}
							rowsChanged = true;
						}
					}
				}
				updateTeam(team, rowsChanged);
			}
		}
	}
	private void fetchMastery(int summonerId){
		theManager.client.sendAsync(theManager.conref.apply("lol-collections/v1/inventories/"+summonerId+"/champion-mastery").GET().build(),
				JSONBodySubscriber.HANDLE_UTF8)
				.thenAcceptAsync(this::feedMastery, LCUManager.EVENT_QUEUE);
	}
	private void feedMastery(HttpResponse<Object> data){
		if(data.statusCode()/100 == 2){
			Object body = data.body();
			for(Object obj : JSON.toJSList(body)){
				JSMap championData = JSON.toJSMap(obj);
				int index = indexOfChampion(championList, championData.peekInt("championId"));
				Summoner summoner = summonerCache.get(championData.peekInt("playerId"));
				if(index >= 0 && summoner != null){
					int level = championData.peekInt("championLevel",-1);
					boolean chestGranted = championData.peekBoolean("chestGranted");
					int relation = summoner.rt[index];
					if(level >= 0 && level <= 7){
						relation = (relation & ~MASTERY_MASK) | level;
					}
					if(chestGranted){
						relation |= MASTERY_CHEST;
					}
					summoner.rt[index] = (byte)relation;
				}
			}
			fireTableRowsUpdated();
		}
	}
	private void fetchSummonerData(int summonerId){
		theManager.client.sendAsync(theManager.conref.apply("lol-summoner/v1/summoners/"+summonerId).GET().build(),
				JSONBodySubscriber.HANDLE_UTF8)
				.thenAcceptAsync(this::feedSummonerData, LCUManager.EVENT_QUEUE);
	}
	private void feedSummonerData(HttpResponse<Object> data){
		if(data.statusCode()/100 == 2){
			JSMap map = JSON.toJSMap(data.body());
			int id = map.peekInt("summonerId");
			if(id != 0){
				Summoner summoner = summonerCache.get(id);
				if(summoner != null){
					String name = map.peekString("displayName");
					if(name == null){
						name = map.peekString("internalName");
						if(name == null){
							return;
						}
					}
					summoner.name = name;
					summoner.isExcludedFromFreeRotation = map.peekInt("summonerLevel",11) <= 10;
					fireStateChanged();
				}
			}
		}
	}
	/**
	 * Comparator fallback order
	 * -NOT Banned by anyone or picked by non-ally
	 * -Hovered
	 * -Known owned
	 * -Free rotating champion
	 * -Probably owned (mastery chest & on at least mastery level 5)
	 * -Highest mastery
	 * -Alphabetical order
	 */
	@Override
	public int compare(Champion c1, Champion c2) {
		int o1 = indexOfChampion(championList, c1.key),o2 = indexOfChampion(championList, c2.key);
		int t1 = globalStates[o1] << 8,t2 = globalStates[o2] << 8,m1 = 0,m2 = 0;
		if((t1 & UNAVAILABLE) != 0){
			if((t2 & UNAVAILABLE) != 0){
				return 0;
			}else{
				return 1;
			}
		}else if((t2 & UNAVAILABLE) != 0){
			return -1;
		}
		for(int i = 0;i < teamSize;i++){
			Summoner sm = myTeam[i];
			if(sm.isLockedIn){//Ignore summoners who locked in, unless one of the champions is being locked in.
				if(sm.hoveredChampionIndex == o1){
					return o1 == o2 ? 0 : -1;//Locked in champions are sorted by their team order.
				}else if(sm.hoveredChampionIndex == o2){
					return 1;
				}
			}else{
				if(sm.hoveredChampionIndex == o1){
					t1 |= HOVERED;
				}else if(sm.hoveredChampionIndex == o2){
					t2 |= HOVERED;
				}
				int s1 = sm.rt[o1];
				int s2 = sm.rt[o2];
				t1 |= (s1 ^ MASTERY_MASK);
				t2 |= (s2 ^ MASTERY_MASK);
				s1 &= MASTERY_MASK;
				s2 &= MASTERY_MASK;
				m1 += s1*s1;
				m2 += s2*s2;
				if(s1 > 4){
					t1 |= MASTERY_CHEST;
				}
				if(s2 > 4){
					t2 |= MASTERY_CHEST;
				}
			}
		}
		t1 ^= t2;
		if((t1 & HOVERED) != 0){
			return (t2 & HOVERED)-1;
		}
		if((t1 & KNOWN_OWNED) != 0){
			return (t2 & KNOWN_OWNED)-1;
		}
		if((t1 & MASTERY_CHEST) != 0){
			return (t2 & MASTERY_CHEST)-1;
		}
		if((t2 & KNOWN_OWNED) == 0 && (t1 & FREE_ROTATION) != 0){
			return (t1 & FREE_ROTATION)-1;
		}
		if(m1 != m2){
			return m2-m1;
		}
		return c1.compareTo(c2);
	}
	@Override
	public int getRowCount() {
		return globalStates.length;
	}
	@Override
	public int getColumnCount() {
		return myTeam.length+1;
	}
	@Override
	public String getColumnName(int columnIndex) {
		if(columnIndex == 0){
			return "";
		}else{
			Summoner sm = myTeam[columnIndex-1];
			return sm == null ? null : sm.name;
		}
	}
	@Override
	public Class<?> getColumnClass(int columnIndex) {
		return columnIndex == 0 ? Champion.class : Short.class;
	}
	@Override
	public Object getValueAt(int rowIndex, int columnIndex) {
		if(columnIndex == 0){
			return championList.get(rowIndex);
		}else{
			Summoner summoner = myTeam[columnIndex-1];
			if(summoner == null){
				return null;
			}
			int value =		(summoner.rt[rowIndex]) |
							(globalStates[rowIndex] << 8) |
							(summoner.hoveredChampionIndex == rowIndex ? HOVERED : 0);
			if(summoner.isExcludedFromFreeRotation){
				value &= ~FREE_ROTATION;
			}
			return Short.valueOf((short)value);
		}
	}
	public void addChangeListener(ChangeListener x) {
		listenerList.add(ChangeListener.class, x);
	}
	public void removeChangeListener(ChangeListener x) {
		listenerList.remove(ChangeListener.class, x);
	}
    private ChangeEvent che;
    protected void fireStateChanged(){
		Object[] listeners = listenerList.getListenerList();
		for(int i = listeners.length - 2; i >= 0; i -= 2){
			if(listeners[i] == ChangeListener.class){
				if(che == null){
					che = new ChangeEvent(this);
				}
				((ChangeListener)listeners[i+1]).stateChanged(che);
			}
		}
    }
    protected void fireTableRowsUpdated(){
    	fireTableRowsUpdated(0, globalStates.length-1);
    }
}