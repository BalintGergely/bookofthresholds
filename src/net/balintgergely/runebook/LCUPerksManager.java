package net.balintgergely.runebook;

import java.awt.EventQueue;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublisher;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import net.balintgergely.runebook.LCUManager.LCUDataRoute;
import net.balintgergely.runebook.LCUManager.LCUModule;
import net.balintgergely.util.JSList;
import net.balintgergely.util.JSMap;
import net.balintgergely.util.JSON;
import net.balintgergely.util.JSONBodyPublishing;
import net.balintgergely.util.JSONBodySubscriber;

public class LCUPerksManager extends HybridListModel<Build> implements LCUModule{
	@Override
	public Map<LCUDataRoute, Consumer<Object>> getDataRoutes() {
		return Map.of(new LCUDataRoute("lol-perks/v1/pages","OnJsonApiEvent_lol-perks_v1_pages",false), this::perksChanged,
					  new LCUDataRoute("lol-perks/v1/inventory","OnJsonApiEvent_lol-perks_v1_inventory",false), this::inventoryChanged);
	}
	private static final long serialVersionUID = 1L;
	private ArrayList<Build> buildList = new ArrayList<>();
	private HashMap<Number,Build> buildMap = new HashMap<>();
	private RuneModel model;
	private JSList perkList;
	private int ownedPageCount = -1;
	private LCUManager theManager;
	LCUPerksManager(LCUManager md){
		this.theManager = md;
	}
	@Override
	public Build getElementAt(int index) {
		return buildList.get(index);
	}
	@Override
	public int getSize() {
		return buildList.size();
	}
	public void setRuneModel(RuneModel model){
		this.model = model;
		JSList pr = perkList;
		if(pr != null){
			EventQueue.invokeLater(() -> update(pr));
		}
	}
	public CompletableFuture<Object> exportRune(Rune rune,String name){
		BodyPublisher runeExport = JSONBodyPublishing.publish(rune.toJSMap().put("name", name, "current", Boolean.TRUE));
		CompletableFuture<?> strf = theManager.tryConnect();
		CompletableFuture<Object> response = strf.thenCompose(o -> exportRuneInternal(runeExport,name));
		return response.exceptionallyCompose(t ->
			theManager.refreshConnection(strf).thenCompose(o1 -> exportRuneInternal(runeExport,name))
		);
	}
	private CompletableFuture<Object> exportRuneInternal(BodyPublisher bp,String name){
		JSList runeList = perkList;
		JSMap pageToReplace = null;
		int pageLimit = ownedPageCount;
		long lastModTime = Long.MAX_VALUE;
		for(Object obj : runeList){
			JSMap runeBuild = JSON.asJSMap(obj, true);
			if(runeBuild.peekBoolean("isEditable",false)){//Ignore builtin pages
				if(name.equals(runeBuild.peekString("name"))){
					pageToReplace = runeBuild;
					pageLimit = -1;
					break;
				}
				long modTime = runeBuild.peekLong("lastModified");
				if(modTime <= lastModTime){
					pageToReplace = runeBuild;
					lastModTime = modTime;
				}
				pageLimit--;
			}
		}
		HttpRequest exportRequest;
		if(pageLimit > 0){
			exportRequest = theManager.conref.apply("lol-perks/v1/pages").POST(bp).build();
		}else{
			int pageId = pageToReplace.getInt("id");
			exportRequest = theManager.conref.apply("lol-perks/v1/pages/"+pageId).PUT(bp).build();
		}
		return theManager.client.sendAsync(exportRequest, JSONBodySubscriber.HANDLE_UTF8).thenApply(LCUManager::bodyOf);
	}
	private void inventoryChanged(Object o){
		int pc = JSON.toJSMap(o).peekInt("ownedPageCount",-1);
		if(pc >= 0){
			ownedPageCount = pc;
		}
	}
	private void perksChanged(Object o){
		if(o instanceof JSList){
			JSList pl = (JSList)o;
			this.perkList = pl;
			EventQueue.invokeLater(() -> update(pl));
		}
	}
	private void update(JSList builds){
		if(model == null){
			return;
		}
		antiRecurse = true;
		try{
			Build selectedBuild = selectedIndex >= 0 ? buildList.get(selectedIndex) : null;
			int previousSize = buildList.size();
			buildList.clear();
			int indexOfSelectedBuild = -1;
			for(Object obj : builds){
				JSMap m = JSON.asJSMap(obj, false);
				if(m != null){
					try{
						Number id = m.getNumber("id");
						String name = m.peekString("name","");
						long timestamp = m.peekLong("lastModified", 0);
						boolean editable = m.peekBoolean("isEditable");
						Rune rn = model.parseRune(m);
						Build bld = buildMap.get(id);
						if(bld == null){
							buildMap.put(id, bld = new Build(name, null, rn, editable ? (byte)0x80 : (byte)0xC0, timestamp));
						}else{
							if(bld == selectedBuild){
								indexOfSelectedBuild = buildList.size();
							}
							bld.setName(name);
							bld.setOrder(timestamp);
							bld.setRune(rn);
							bld.setFlags((byte)((bld.getFlags() & 0x3F) | (editable ? 0x80 : 0xC0)));
						}
						if(m.peekBoolean("current")){
							selectedBuild = bld;
							indexOfSelectedBuild = buildList.size();
						}
						buildList.add(bld);
					}catch(Exception e){
						e.printStackTrace();
					}
				}
			}
			fireContentsChanged(this, 0, Math.max(previousSize, buildList.size()));
			super.moveSelection(indexOfSelectedBuild);
			super.fireStateChanged();
		}finally{
			antiRecurse = false;
		}
	}
}
