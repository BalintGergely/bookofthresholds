package net.balintgergely.runebook;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.Vector;
import java.util.zip.CRC32;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.ResourceBundle.Control;
import java.util.function.Function;

import javax.imageio.ImageIO;
import javax.swing.Icon;
import javax.swing.ImageIcon;

import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import net.balintgergely.util.JSON;
import net.balintgergely.runebook.Champion.Variant;
import net.balintgergely.runebook.RuneModel.*;
import net.balintgergely.util.JSList;
import net.balintgergely.util.JSMap;
import net.balintgergely.util.ArrayListView;

class AssetManager{
	public static final byte	TOP = 0x1,
								MIDDLE = 0x2,
								BOTTOM = 0x4,
								JUNGLE = 0x8,
								SUPPORT = 0x10;
	public static final int PATH_SIZE = 32,KEYSTONE_SIZE = 48,RUNESTONE_SIZE = 32,STAT_MOD_SIZE = 24;
	private final String kaynId;
	private static void an(Object o){
		if(o != null){
			throw new IllegalStateException();
		}
	}
	final RuneModel runeModel;
	/**
	 * Used when directly pasting from Mobafire.
	 */
	final RuneModel englishRuneModel;
	final NavigableMap<String,Champion> championsById;
	final Map<Champion,List<Variant>> championVariants;
	final List<Champion> championsByKey;
	final Map<String,Champion> championsByEnglishName;
	final Map<RuneModel.Stone,Image> runeIcons;
	final Map<RuneModel.Stone,Color> runeColors;
	BufferedImage windowIcon;
	BufferedImage iconSprites;
	BufferedImage background;
	PropertyResourceBundle z;
	Locale locale;
	@SuppressWarnings("unchecked")
	AssetManager(DataDragon dragon,String locale) throws IOException{
		Locale l = null;
		if(locale == null){
			locale = (String)dragon.fetchObject("locale.txt");
		}else{
			dragon.putString("locale.txt", locale);//This time we got to retrieve client locale. Store it.
		}
		JSMap manifest = dragon.getManifest();
		JSMap n = manifest.getJSMap("n");
		String en = manifest.getString("l");
		if(locale == null){//We have yet to retrieve the locale from the LCU. Use data dragon.
			l = Locale.getDefault();
			JSList localeData = JSON.asJSList(dragon.fetchObject("languages.json"), true);
			List<String> localeList = (List<String>)(List<?>)localeData.list;
			a: for(Locale lc : Control.getControl(Control.FORMAT_DEFAULT).getCandidateLocales("", l)){
				String tag = lc.toLanguageTag().toLowerCase(Locale.ROOT).replace('-', '_');
				for(String nm : localeList){
					if(nm.toLowerCase(Locale.ROOT).startsWith(tag)){
						locale = nm;//Found a match.
						break a;
					}
				}
			}//This might be a bad way to do it. We basically default to system locale if supported by LCU.
			if(locale == null){
				locale = en;//Not even LCU supports the system locale. Default to english.
			}
		}
		boolean isEnglish = locale.equals(en);
		if(isEnglish){
			l = null;
		}
		//This logic is needed for Mobafire.
		if(l == null){
			try{
				l = Locale.forLanguageTag(locale.replace('_', '-'));
			}catch(Exception e){
				e.printStackTrace();
				if(isEnglish){
					l = Locale.ENGLISH;
				}else if(l == null){
					l = Locale.getDefault();
				}
			}
		}
		Locale.setDefault(l);
		z = (PropertyResourceBundle) ResourceBundle.getBundle("locale",isEnglish ? Locale.ROOT : l);
		JSMap championData = JSON.asJSMap(dragon.fetchObject(n.getString("champion")+"/data/"+locale+"/champion.json"), true).getJSMap("data");
		JSMap englishChampionData = isEnglish ? championData : 
			JSON.asJSMap(dragon.fetchObject(n.getString("champion")+"/data/"+en+"/champion.json"), true).getJSMap("data");
		JSList runeData = JSON.asJSList(dragon.fetchObject(n.getString("rune")+"/data/"+locale+"/runesReforged.json"), true);
		this.locale = l;
		JSList englishRuneData = isEnglish ? null : JSON.asJSList(dragon.fetchObject(n.getString("rune")+"/data/"+en+"/runesReforged.json"), true);
		HashSet<String> imageCollection = new HashSet<>();
		JSON.forEachMapEntry(championData, (String key,Object value) -> {
			if("image".equals(key)){
				imageCollection.add(n.getString("champion")+"/img/sprite/"+JSON.asJSMap(value, true).getString("sprite"));
			}
		},1);
		JSON.forEachMapEntry(runeData, (String key,Object value) -> {
			if("icon".equals(key)){
				imageCollection.add("img/"+JSON.asString(value, true, null));
			}
		},-1);
		//Instead of getting championFull.json just to fetch Last Chapter's icon, we directly go to her file.
		String iconPath = n.getString("champion")+"/img/spell/"+
						JSON.asJSMap(dragon.fetchObject(n.getString("champion")+"/data/en_US/champion/Yuumi.json"),true)
						.getDeep("data","Yuumi","spells",3,"image","full");
		imageCollection.add(iconPath);
		dragon.batchPreload(imageCollection);

		runeModel = new RuneModel(runeData, z);
		//Additionally to a localized rune model, we need an english variant just for Mobafire insertion.
		englishRuneModel = isEnglish ? runeModel : 
			new RuneModel(englishRuneData, (PropertyResourceBundle) ResourceBundle.getBundle("locale",Locale.ROOT));
		runeIcons = new HashMap<>();
		runeColors = new HashMap<>();
		Function<Stone,Image> imageFetcher = s -> (BufferedImage)dragon.fetchObject("img/"+s.imageRoute);
		Function<Stone,Color> colorFetcher = s -> averagePixels((BufferedImage)runeIcons.computeIfAbsent(s,imageFetcher));
		for(Stone st : runeModel.stoneMap.values()){
			if(st instanceof Path || st instanceof Statstone){
				runeColors.computeIfAbsent(st,colorFetcher);
			}else{
				runeIcons.computeIfAbsent(st,imageFetcher);
				runeColors.put(st,runeColors.computeIfAbsent(((Runestone)st).path,colorFetcher));
			}
		}
		for(Map.Entry<Stone,Image> entry : runeIcons.entrySet()){
			Stone st = entry.getKey();
			int scale;
			if(st instanceof Path){
				scale = PATH_SIZE;
			}else if(st instanceof Runestone){
				scale = ((Runestone)st).slot == 0 ? KEYSTONE_SIZE : RUNESTONE_SIZE;
			}else{
				scale = STAT_MOD_SIZE;
			}
			entry.setValue(entry.getValue().getScaledInstance(scale, scale, Image.SCALE_SMOOTH));
		}
		TreeMap<String,Champion> champById = new TreeMap<>();
		Champion[] champByKey = new Champion[championData.map.size()];
		HashMap<String,Champion> champByEng = new HashMap<>();
		int index = 0;
		Champion kayn = null;
		Variant ass = null,slay = null;
		for(Entry<String,Object> entry : championData.map.entrySet()){
			String key = entry.getKey();
			JSMap champion = JSON.asJSMap(entry.getValue(),true);
			JSMap image = champion.getJSMap("image");
			Champion ch = new Champion(
					key,
					champion.getString("name"),
					champion.getInt("key"),
					((BufferedImage)dragon.fetchObject(n.getString("champion")+"/img/sprite/"+image.getString("sprite")))
					.getSubimage(image.getInt("x"), image.getInt("y"), image.getInt("w"), image.getInt("h")));
			an(champByEng.put(englishChampionData.getJSMap(key).getString("name").toUpperCase(Locale.ROOT),ch));
			if(key.equals("Kayn")){
				kayn = ch;//Guaranteed to happen no more than once.
				ass = new Variant(ch,"ass",loadImageWithHash("kayn_ass_square.png",2624372905l));
				slay = new Variant(ch,"slay",loadImageWithHash("kayn_slay_square.png",4243224101l));
				an(champById.put(key+"ass",ass));
				an(champById.put(key+"slay",slay));
			}else{
				an(champById.put(key,ch));
			}
			champByKey[index++] = ch;
		}
		Arrays.sort(champByKey,(a,b) -> Integer.compare(a.key,b.key));
		//This will error if Kayn is not present in the champion database.
		champById.put(kaynId = kayn.toString(),kayn);
		championVariants = Map.of(kayn,List.of(ass,slay));
		championsById = Collections.unmodifiableNavigableMap(champById);
		championsByKey = new ArrayListView<>(champByKey);
		championsByEnglishName = Collections.unmodifiableMap(champByEng);
		{//Rune Book? Book with power? Why not?
			BufferedImage windIc = (BufferedImage)dragon.fetchObject(iconPath);//Here, Ryze's passive icon is another big candidate.
			int hx = windIc.getWidth()/2,hy = windIc.getHeight()/2;//Unsealed Spellbook however is not the way to go due to being a keystone.
			windowIcon = windIc.getSubimage(0, hy, hx, hy);
		}
		iconSprites = loadImageWithHash("icons.png",2467156344l);//Absolutely NO tampering please!
		background = loadImageWithHash("background.png",1433901601l);
	}
	public boolean hasSubChampions(Champion ch){
		return ch.toString() == kaynId;
	}
	public Vector<Champion> getEffectiveChampionList(){
		Vector<Champion> list = new Vector<>(championsById.size()-1);
		for(Champion ch : championsById.values()){
			if(ch.toString() != kaynId){
				list.add(ch);
			}
		}
		return list;
	}
	public Vector<Champion> getSelectableChampionList(){
		Vector<Champion> list = new Vector<>(championsById.size()-2);
		for(Champion ch : championsById.values()){
			if(!(ch instanceof Variant)){
				list.add(ch);
			}
		}
		return list;
	}
	BufferedImage loadImageWithHash(String name,long checksum) throws IOException{
		CRC32 sum = new CRC32();
		byte[] data;
		try(InputStream input = AssetManager.class.getResourceAsStream(name)){
			data = input.readAllBytes();
		}
		sum.update(data);
		long sim = sum.getValue();
		if(sim != checksum){
			System.err.println("Checksum mismatch. Expected: "+checksum+" Encountered: "+sim);
			throw new IOException("Could not read asset file: "+name+" The file may be corrupted. (File length: "+data.length+")");
		}
		return ImageIO.read(new ByteArrayInputStream(data));
	}
	public ImageIcon imageIconForImage(int imgx,int imgy){
		return new ImageIcon(iconSprites.getSubimage(imgx*24, imgy*24, 24, 24));
	}
	public static Color averagePixels(BufferedImage img){
		double redSum = 0,blueSum = 0,greenSum = 0,alphaSum = 0;
		int w = img.getWidth(),h = img.getHeight();
		for(int y = 0;y < h;y++){
			for(int x = 0;x < w;x++){
				int pix = img.getRGB(x, y);
				double alpha = (pix >>> 24) & 0xff;
				double red = (pix >>> 16) & 0xff;
				double green = (pix >>> 8) & 0xff;
				double blue = (pix) & 0xff;
				redSum += red*alpha;
				blueSum += blue*alpha;
				greenSum += green*alpha;
				alphaSum += alpha;
			}
		}
		if(alphaSum == 0){
			return new Color(0,true);
		}else{
			redSum /= alphaSum;
			blueSum /= alphaSum;
			greenSum /= alphaSum;
			int red = redSum <= 0 ? 0 : (redSum >= 255 ? 255 : (int)Math.round(redSum));
			int green = greenSum <= 0 ? 0 : (greenSum >= 255 ? 255 : (int)Math.round(greenSum));
			int blue = blueSum <= 0 ? 0 : (blueSum >= 255 ? 255 : (int)Math.round(blueSum));
			return new Color((red << 16) | (green << 8) | blue);
		}
	}
	public class RoleIcon implements Icon{
		public final byte roles;
		public RoleIcon(byte roles){
			this.roles = roles;
		}
		@Override
		public void paintIcon(Component c, Graphics g, int x, int y) {
			paintRoleIcon(g, roles, x, y);
		}
		@Override
		public int getIconWidth() {
			return 24;
		}
		@Override
		public int getIconHeight() {
			return 24;
		}
	}
	public void paintRoleIcon(Graphics gr,byte roles,int x,int y){
		roles &= 0x1F;
		if(roles == 0x1F){//All roles
			rimg(gr, x, y, 3, 3);
		}else if(roles == 0x1D){//Special case: All but mid
			rimg(gr, x, y, 3, 2);
		}else if(roles == 0x1A){//Special case: Mid, Support, Jungle
			rimg(gr, x, y, 1, 1);
			rimg(gr, x, y, 1, 2);
		}else{boolean top = (roles & TOP) != 0,mid = (roles & MIDDLE) != 0,bot = (roles & BOTTOM) != 0;
			if((roles & 0x18) == 0){//Laning roles only
				if(mid){
					rimg(gr, x, y, 1, 1);
					rimg(gr, x, y, 0, top ? 3 : 2);
					rimg(gr, x, y, 2, bot ? 3 : 2);
				}else{
					rimg(gr, x, y, 1, 0);
					rimg(gr, x, y, 0, top ? 1 : 0);
					rimg(gr, x, y, 2, bot ? 1 : 0);
				}
			}else if((roles & 0x18) == roles){//No lanes
				switch(roles){
				case JUNGLE:rimg(gr, x, y, 3, 0);break;//Jungle
				case SUPPORT:rimg(gr, x, y, 3, 1);break;//Support
				default:
					icog(gr, x+2, y+2, false,false);
					icog(gr, x+10, y+10, true,true);
				}
			}else if((roles & 0x18) != 0x18){//Laning roles and either jungle or support.
				if(mid){
					icog(gr, x+12, y, true, false);
					icog(gr, x, y+12, false, true);
					rimg(gr, x, y, 0, top ? 3 : 2);
					rimg(gr, x, y, 2, bot ? 3 : 2);
				}else{
					rimg(gr, x, y, 0, top ? 1 : 0);
					rimg(gr, x, y, 2, bot ? 1 : 0);
				}
				boolean isSup = (roles & 0x18) == SUPPORT;
				icog(gr, x+6,y+6, isSup, isSup);
			}else{//Both jungle and support. Either top or bot.
				if(mid){
					icog(gr, x+12, y, true, false);
					icog(gr, x, y+12, false, true);
				}
				if(top){
					rimg(gr, x, y, 0, mid ? 3 : 1);
					icog(gr, x+5, y+5, false,false);
					icog(gr, x+12, y+12, true,true);
				}else{
					rimg(gr, x, y, 2, mid ? 3 : 1);
					icog(gr, x, y, false,false);
					icog(gr, x+7, y+7, true,true);
				}
			}
		}
	}
	private void rimg(Graphics gr,int tx,int ty,int ix,int iy){
		ix *= 24;
		iy *= 24;
		gr.drawImage(iconSprites, tx, ty, tx+24, ty+24, ix, iy, ix+24, iy+24, null);
	}
	private void icog(Graphics gr,int tx,int ty,boolean x,boolean y){
		int ix = x ? 36 : 24;
		int iy = y ? 60 : 48;
		gr.drawImage(iconSprites, tx, ty, tx+12, ty+12, ix, iy, ix+12, iy+12, null);
	}
}
