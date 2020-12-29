package net.balintgergely.runebook;

import java.awt.EventQueue;
import java.net.http.HttpResponse;
import java.util.Comparator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

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

public class LCUSummonerManager extends AbstractTableModel implements LCUModule{
	private static final long serialVersionUID = 1L;
	/**
	 * The maximum number of summoners we ever display.
	 * The local summoner is never hidden once displayed.
	 * Any summoner with a known id who participates in the current lobby or session is added to the list.
	 * Queuing requires us to keep track of at most 5 summoners.
	 * Spectating a match requires us to keep track of the 10 summoners who participate.
	 * A custom game supports 10 summoners and 4 spectators. Unsure of whether the local summoner always
	 * has to participate in one we maintain one extra space for them. Hence the number 15.
	 */
	private static final int	PGHL = 15;
	public static final int		MASTERY_OFFSET =	0x8,
								MASTERY_MASK =		0x7*MASTERY_OFFSET,
								STATUS_OFFSET =		0x40,
								STATUS_MASK =		0x3*STATUS_OFFSET,
								MEDAL_OFFSET =		0x1,
								MEDAL_MASK =		0x7*MEDAL_OFFSET,
								MEDAL_NOT_OWNED =	0x0*MEDAL_OFFSET,
								MEDAL_UNKNOWN =		0x1*MEDAL_OFFSET,
								MEDAL_MASTERY =		0x3*MEDAL_OFFSET,
								MEDAL_FREE =		0x6*MEDAL_OFFSET,
								MEDAL_OWNED =		0x7*MEDAL_OFFSET,
								STATE_NONE =		0x3*STATUS_OFFSET,
								STATE_ALLY =		0x1*STATUS_OFFSET,
								STATE_ENEMY =		0x0*STATUS_OFFSET,
								STATE_BANNED =		0x2*STATUS_OFFSET;
	private static final int	SM_MASTERY =		0x7,
								SM_OWNED =			0x8,
								SM_NOT_OWNED =		0x10,
								SM_CHEST =			0x20;
	private static final int	GB_FREE =			0x1,
								GB_BANNED =			0x2,
								GB_ENEMY =			0x4,
								GB_LOCAL_OWNED =	0x8,
								GB_UNAVAILABLE =	GB_BANNED | GB_ENEMY;
	/*private static final int	MASTERY_CHEST =		0x8,
								KNOWN_OWNED =		0x10,
								KNOWN_NOT_OWNED =	0x20,
								HOVERED	=			0x40,
								IS_ENEMY =			0x80,
							
								FREE_ROTATION =		0x100,
								BANNED =			0x200,
								OWNED_LOCALLY =		0x400,
								PICKED_BY_ENEMY =	0x800,
							
								UNAVAILABLE =		0xA00;
	
	private static final int	GS_FREE_ROTATION =	0x1,
								GS_BANNED =			0x2,
								GS_OWNED =			0x4,
								GS_PICKED_BY_ENEMY =0x8,
							
								GS_UNAVAILABLE =	0xA;*/
	public static final Comparator<Object> SUMMONER_PRIORITY = (a,b) -> {
		if(a instanceof Summoner){
			if(b instanceof Summoner){
				return ((Summoner) a).priority-((Summoner) b).priority;
			}
			return -1;
		}
		if(b instanceof Summoner){
			return 1;
		}
		return ((Byte)b).compareTo((Byte)a);
	};
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
	public static class Summoner{
		private final byte[] rt;
		private String name;
		private byte position;
		private final int summonerId;
		private Champion champion;
		private boolean isDetailed = false;
		private boolean isExcludedFromFreeRotation = false;
		private int priority = Integer.MAX_VALUE;
		private boolean isEnemy;
		private Summoner(int id,int rtSize){
			this.summonerId = id;
			this.rt = new byte[rtSize];
		}
		public byte getPosition(){
			return position;
		}
		@Override
		public String toString(){
			return name;
		}
		public boolean isEnemy(){
			return isEnemy;
		}
	}
	private LCUManager theManager;
	LCUSummonerManager(LCUManager md,AssetManager assets){
		this.theManager = md;
		int length = assets.championList.size();
		this.assets = assets;
		globalStates = new byte[length];
	}
	@Override
	public Map<LCUDataRoute, BiConsumer<String,Object>> getDataRoutes() {
		return Map.of(
				new LCUDataRoute("lol-lobby/v2/lobby","OnJsonApiEvent_lol-lobby_v2_lobby",true),
				this::lobbyChanged,
				new LCUDataRoute("lol-champ-select/v1/session","OnJsonApiEvent_lol-champ-select_v1_session",true),
				this::sessionChanged,
				new LCUDataRoute("lol-champions/v1/owned-champions-minimal","OnJsonApiEvent_lol-champions_v1_owned-champions-minimal",false),
				this::championsChanged
				);
	}
	private AssetManager assets;
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
	private Summoner[] columnArray = new Summoner[PGHL];
	private int sessionId = Integer.MIN_VALUE;
	private int teamSize;
	
	/**
	 * Updates the summoner list based on the JSList of summoners provided.
	 */
	private void updateTeam(JSList team0,JSList team1,boolean forceUpdate){
		EventQueue.invokeLater(() -> {
			teamSize = 0;
			boolean pghlChanged = false,isEnemy = team0.isEmpty();
			JSList currentTeam = isEnemy ? team1 : team0;
			int summonerIndex = 0;
			if(!currentTeam.isEmpty())pghLoop: while(true){
				JSMap summonerData = currentTeam.getJSMap(summonerIndex);
				int id = summonerData.peekInt("summonerId");
				if(id != 0){
					Summoner sm = summonerCache.get(id);
					//All low and behold the 'pghl' algorythm is now fully embedded in one single "updateTeam" method!
					Summoner sm0 = columnArray[teamSize];
					if(sm0 != sm){
						for(int s = 0;s < columnArray.length;s++){
							if(columnArray[s] == sm || columnArray[s] == null){
								columnArray[s] = sm0;
								if(sm0 != null){
									sm0.priority = PGHL+s;
									sm0 = null;
								}
							}
						}
						if(sm0 != null){
							for(int index1 = teamSize;index1 < columnArray.length;index1++){
								Summoner sm1 = columnArray[index1];
								if(sm1 != mySummoner){
									columnArray[index1] = sm0;
									sm0.priority = PGHL+index1;
									sm0 = sm1;
								}
							}
							sm0.priority = PGHL+PGHL;
							sm0.isDetailed = false;
						}
						columnArray[teamSize] = sm;
						sm.priority = teamSize;
						if(detailedMode){
							sm.isDetailed = true;
							fetchMastery(sm.summonerId);
							if(sm.name == null){
								fetchSummonerData(sm.summonerId);
							}
						}
						pghlChanged = true;
					}
					teamSize++;
				}
				summonerIndex++;
				if(summonerIndex == currentTeam.size()){
					if(isEnemy || team1.isEmpty()){
						break pghLoop;
					}else{
						currentTeam = team1;
						summonerIndex = 0;
						isEnemy = true;
					}
				}
			}
			if(globalStates != null && (pghlChanged || forceUpdate)){
				fireTableRowsUpdated();
				fireStateChanged();
			}
		});
	}
	private Summoner newSummoner(int id){
		return new Summoner(id, globalStates.length);
	}
	public void setDetailed(boolean enabled){
		assert EventQueue.isDispatchThread();
		if(enabled != detailedMode){
			if(enabled){
				if(assets == null){
					throw new IllegalStateException("Model not ready!");
				}
				detailedMode = true;
				for(Summoner sm : columnArray){
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
	public int getNPreferredChampions(Champion[] array){
		assert EventQueue.isDispatchThread();
		int index = 0;
		if(array.length == index || mySummoner == null){
			return index;
		}
		boolean imbl = mySummoner.isExcludedFromFreeRotation;
		if(mySummoner.champion != null){
			array[index++] = mySummoner.champion;
			if(array.length == index){
				return index;
			}
		}
		for(int i = 0;i < teamSize;i++){
			Summoner sm = columnArray[i];
			a: if(sm != null && sm != mySummoner){
				Champion champion = sm.champion;
				int hoverIndex = assets.championList.indexOf(champion);
				if(champion != null && (
						 ((mySummoner.rt[hoverIndex] & SM_OWNED) != 0) ||
						!(imbl || (globalStates[hoverIndex] & GB_FREE) == 0)
				)){
					for(int dex = 0;dex < index;dex++){
						if(array[dex] == champion){
							break a;
						}
					}
					array[index++] = champion;
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
	void championsChanged(String m,Object obj){
		synchronized(summonerCache){
			championsData = JSON.asJSList(obj);
			championsChanged();
		}
	}
	private void championsChanged(){
		assert Thread.holdsLock(summonerCache);
		if(assets != null){
			JSList chd = championsData;
			if(chd != null){
				for(int i = 0;i < globalStates.length;i++){
					globalStates[i] &= GB_BANNED;
				}
				for(Object obj : chd){
					JSMap championData = JSON.asJSMap(obj);
					Champion champion = assets.championsByKey.get(championData.get("id"));
					if(champion != null){
						int index = assets.championList.indexOf(champion);
						if(championData.getBoolean("freeToPlay")){
							globalStates[index] |= GB_FREE;
						}
						JSMap ownership = JSON.toJSMap(championData.peek("ownership"));
						if(ownership.peekBoolean("owned") || JSON.toJSMap(ownership.peek("rental")).peekBoolean("rented")){
							globalStates[index] |= GB_LOCAL_OWNED;
						}
					}
				}
				if(mySummoner != null){
					for(int i = 0;i < globalStates.length;i++){
						if((globalStates[i] & GB_LOCAL_OWNED) == 0){
							mySummoner.rt[i] = (byte)((mySummoner.rt[i] & ~SM_OWNED) | SM_NOT_OWNED);
						}else{
							mySummoner.rt[i] = (byte)((mySummoner.rt[i] & ~SM_NOT_OWNED) | SM_OWNED);
						}
					}
				}
				EventQueue.invokeLater(() -> fireTableRowsUpdated(0, globalStates.length-1));
			}
		}
	}
	//Updates the lobby state on the event queue thread.
	private void lobbyChanged(String m,Object obj){
		if(obj instanceof JSMap){
			synchronized(summonerCache){
				JSList lst;
				JSMap lobbyData = (JSMap)obj;
				boolean isHowlingAbyss = JSON.toJSMap(lobbyData.peek("gameConfig")).peekInt("mapId") == 12;
				if(sessionId == Integer.MIN_VALUE && !(lst = JSON.toJSList(lobbyData.peek("members"))).isEmpty()){
					boolean rolesChanged = false;
					for(Object smObj : lst){
						JSMap summonerData = JSON.toJSMap(smObj);
						int id = summonerData.peekInt("summonerId");
						if(id != 0){
							Summoner summoner = summonerCache.computeIfAbsent(id, this::newSummoner);
							byte pos;
							if(isHowlingAbyss){
								pos = AssetManager.MIDDLE;
							}else{
								boolean rf = true;
								switch(summonerData.peek("firstPositionPreference", "")){
								case "TOP":pos = AssetManager.TOP;break;
								case "JUNGLE":pos = AssetManager.JUNGLE;break;
								case "MIDDLE":pos = AssetManager.MIDDLE;break;
								case "BOTTOM":pos = AssetManager.BOTTOM;break;
								case "UTILITY":pos = AssetManager.SUPPORT;break;
								case "UNSELECTED":rf = false;//$FALL-THROUGH$
								default:pos = 0;
								}
								switch(summonerData.peek("secondPositionPreference", "")){
								case "TOP":pos |= AssetManager.TOP;break;
								case "JUNGLE":pos |= AssetManager.JUNGLE;break;
								case "MIDDLE":pos |= AssetManager.MIDDLE;break;
								case "BOTTOM":pos |= AssetManager.BOTTOM;break;
								case "UTILITY":pos |= AssetManager.SUPPORT;break;
								}
								if(pos == 0 && rf){
									pos = 0x1f;
								}
							}
							if(summoner.position != pos){
								summoner.position = pos;
								rolesChanged = true;
							}
							
							boolean isEnemy = summonerData.peekInt("teamId") == 200;
							if(summoner.isEnemy != isEnemy){
								summoner.isEnemy = isEnemy;
								rolesChanged = true;
							}
							String name = summonerData.peekString("summonerName");
							if(name != null){
								summoner.name = name;
							}
						}
					}
					updateTeam(lst, JSList.EMPTY_LIST, rolesChanged);
				}
			}
		}
	}
	//Updates the current session using both the local and the event queue threads.
	private void sessionChanged(String m,Object obj){
		if("Delete".equals(m)){
			obj = null;
		}
		synchronized(summonerCache){
			sessionData = obj == null ? null : JSON.asJSMap(obj);
			sessionChanged();
		}
	}
	private void sessionChanged(){
		assert Thread.holdsLock(summonerCache);
		if(assets != null){
			JSMap sessionLocal = sessionData;
			if(sessionLocal == null){
				sessionId = Integer.MIN_VALUE;
			}else{
				int gameId = sessionLocal.peekInt("gameId");
				//JSMap timer = sessionLocal.peekJSMap("timer");
				//if(timer != null){// && "FINALIZATION".equals(timer.peekString("phase"))){
				//	System.out.println("Phase: "+timer.peekString("phase"));
				//	System.out.println("Time left in phase: "+timer.peekLong("adjustedTimeLeftInPhase"));
				//}
				JSList team0 = sessionLocal.getJSList("myTeam"),
						team1 = JSON.toJSList(sessionLocal.peek("theirTeam"));
				if(gameId != sessionId){
					for(int i = 0;i < globalStates.length;i++){
						globalStates[i] &= ~GB_UNAVAILABLE;
					}
					sessionId = gameId;
				}
				for(Object actions : sessionLocal.getJSList("actions")){
					for(Object action : JSON.toJSList(actions)){
						JSMap actionData = JSON.toJSMap(action);
						if(actionData.peekBoolean("completed")){
							Champion champion = assets.championsByKey.get(actionData.get("championId"));
							int index = assets.championList.indexOf(champion);
							if(champion != null)switch(actionData.peekString("type","")){
							case "ban":
								globalStates[index] |= GB_BANNED;
								break;
							case "pick":
								if(!actionData.peekBoolean("isAllyAction",true)){
									globalStates[index] |= GB_ENEMY;
								}
								break;
							}
						}
					}
				}
				boolean rowsChanged = false;
				int selfChampionIndex = -1;
				int total = team0.size()+team1.size(),localTeamSize = team0.size();
				for(int summonerIndex = 0;summonerIndex < total;summonerIndex++){
					boolean isEnemy = summonerIndex >= localTeamSize;
					JSMap summonerData = isEnemy ? team1.getJSMap(summonerIndex-localTeamSize) : team0.getJSMap(summonerIndex);
					int id = summonerData.peekInt("summonerId");
					if(id != 0){
						Summoner summoner = summonerCache.computeIfAbsent(id, this::newSummoner);
						summoner.isEnemy = isEnemy;
						switch(summonerData.peek("assignedPosition", "")){
						case "top":summoner.position = AssetManager.TOP;break;
						case "jungle":summoner.position = AssetManager.JUNGLE;break;
						case "middle":summoner.position = AssetManager.MIDDLE;break;
						case "bottom":summoner.position = AssetManager.BOTTOM;break;
						case "utility":summoner.position = AssetManager.SUPPORT;break;
						default:summoner.position = 0;
						}
						Champion champion = assets.championsByKey.get(summonerData.get("championId"));
						if(champion == null){
							champion = assets.championsByKey.get(summonerData.get("championPickIntent"));
						}
						int index = assets.championList.indexOf(champion);
						if(summoner.champion != champion){
							summoner.champion = champion;
							if(champion != null){
								if(summoner != mySummoner && (globalStates[index] & GB_FREE) == 0){
									summoner.rt[index] |= SM_OWNED;
								}
							}
							rowsChanged = true;
						}
						if(summoner == mySummoner){
							selfChampionIndex = index;
						}
					}
				}
				if(//If we can trade with an ally it means they own the champion we picked.
						selfChampionIndex >= 0 &&//Of course that is unless our champion is part of the free rotation.
						(globalStates[selfChampionIndex] & GB_FREE) == 0 &&
						!(
								mySummoner.isExcludedFromFreeRotation ||
								sessionLocal.peekBoolean("allowRerolling", false)
						)
					){
					for(Object trade : JSON.toJSList(sessionLocal.peek("trades"))){
						JSMap tradeMap = JSON.toJSMap(trade);
						a: if("AVAILABLE".equals(tradeMap.peekString("state"))){
							int cellId = tradeMap.peekInt("cellId",-1);
							if(cellId >= 0){//This is a bit of a roundabout way to iterate. We expect the cell id to match their index.
								for(int offset = 0;offset < localTeamSize;offset++){
									JSMap summonerData = team0.getJSMap((cellId+offset)%localTeamSize);
									if(cellId == summonerData.peekInt("cellId",-1)){
										int id = summonerData.peekInt("summonerId");
										if(id != 0){
											Summoner summoner = summonerCache.get(id);
											if(!summoner.isExcludedFromFreeRotation){
												summoner.rt[selfChampionIndex] |= SM_OWNED;
											}
										}
										break a;
									}
								}
							}
						}
					}
				}
				updateTeam(team0, team1, rowsChanged);
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
				Champion champion = assets.championsByKey.get(championData.get("championId"));
				Summoner summoner = summonerCache.get(championData.peekInt("playerId"));
				if(champion != null && summoner != null){
					int index = assets.championList.indexOf(champion);
					int level = championData.peekInt("championLevel",-1);
					boolean chestGranted = championData.peekBoolean("chestGranted");
					int relation = summoner.rt[index];
					if(level >= 0 && level <= 7){
						relation = (relation & ~SM_MASTERY) | level;
					}
					if(chestGranted){
						relation |= SM_CHEST;
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
	@Override
	public int getRowCount() {
		return globalStates.length;
	}
	@Override
	public int getColumnCount() {
		return columnArray.length+2;
	}
	@Override
	public String getColumnName(int columnIndex) {
		if(columnIndex < 2){
			return "";
		}else{
			Summoner sm = columnArray[columnIndex-2];
			return sm == null ? null : sm.name;
		}
	}
	public Summoner getSummoner(int index){
		return columnArray[index];
	}
	@Override
	public Class<?> getColumnClass(int columnIndex) {
		return Object.class;
	}
	@Override
	public Object getValueAt(int rowIndex, int columnIndex) {
		if(columnIndex == 0){
			return assets.championList.get(rowIndex);
		}else if(columnIndex == 1){
			for(int i = 0;i < teamSize;i++){
				Summoner sm = columnArray[i];
				if(sm != null && assets.championList.indexOf(sm.champion) == rowIndex){
					return sm;
				}
			}
			int value = globalStates[rowIndex];
			byte target = (byte)(((value & GB_FREE) == 0) ? MEDAL_UNKNOWN : MEDAL_FREE);
			if((value & GB_BANNED) != 0){
				if((value & GB_ENEMY) != 0){
					target = (byte)((target & ~MEDAL_MASK) | STATE_ENEMY);
				}else{
					target |= STATE_BANNED;
				}
			}else if((value & GB_ENEMY) != 0){
				target |= STATE_ENEMY;
			}else{
				target |= STATE_NONE;
			}
			return Byte.valueOf(target);
		}else{
			Summoner summoner = columnArray[columnIndex-2];
			if(summoner == null){
				return null;
			}
			int value = summoner.rt[rowIndex];
			int global = globalStates[rowIndex];
			byte target = (byte) ((value & SM_MASTERY) * MASTERY_OFFSET);
			if((value & SM_OWNED) != 0){
				target |= MEDAL_OWNED;
			}else if((global & GB_FREE) != 0){
				target |= MEDAL_FREE;
			}else if((value & SM_CHEST) != 0){
				target |= MEDAL_MASTERY;
			}else if((value & SM_NOT_OWNED) != 0){
				target |= MEDAL_NOT_OWNED;
			}else{
				target |= MEDAL_UNKNOWN;
			}
			if(assets.championList.indexOf(summoner.champion) == rowIndex){
				target |= summoner.isEnemy ? STATE_ENEMY : STATE_ALLY;
			}else{
				target |= (global & GB_BANNED) == 0 ? STATE_NONE : STATE_BANNED;
			}
			return Byte.valueOf(target);
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