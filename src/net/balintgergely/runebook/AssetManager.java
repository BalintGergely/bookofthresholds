package net.balintgergely.runebook;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.zip.CRC32;
import java.util.Map.Entry;
import java.util.ResourceBundle.Control;
import java.util.function.Function;

import javax.imageio.ImageIO;
import javax.swing.Icon;

import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;
import java.awt.Color;
import java.awt.Component;
import java.awt.EventQueue;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import net.balintgergely.util.JSON;
import net.balintgergely.util.NavigableList;
import net.balintgergely.util.OrdinalMap;
import net.balintgergely.runebook.Champion.Variant;
import net.balintgergely.runebook.RuneModel.*;
import net.balintgergely.util.JSList;
import net.balintgergely.util.JSMap;
import net.balintgergely.util.ArrayListView;
import net.balintgergely.util.ImmutableOrdinalMap;
import net.balintgergely.util.OrdinalSet;

class AssetManager{
	public static final byte	TOP = 0x1,
								MIDDLE = 0x2,
								BOTTOM = 0x4,
								JUNGLE = 0x8,
								SUPPORT = 0x10;
	public static final int PATH_SIZE = 32,KEYSTONE_SIZE = 48,RUNESTONE_SIZE = 32,STAT_MOD_SIZE = 24;
	public static final Comparator<Number> KEY_COMPARATOR = (a,b) -> Long.compare(a.intValue(), b.intValue());
	private static final String[] ROLE_NAMES = new String[]{"roleTop","roleMid","roleBot","roleJg","roleSp"};
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
	final OrdinalMap<String,Champion> championsById;
	final OrdinalMap<Number,Champion> championsByKey;
	final Map<String,Champion> championsByEnglishName;
	final List<Champion> championsSortedByName;
	final NavigableList<Champion> championList;
	final Map<Champion,List<Variant>> championVariants;
	final Map<RuneModel.Stone,Image> runeIcons;
	final Map<RuneModel.Stone,Color> runeColors;
	final String listOfLocales;
	final List<String> tagList;
	final int championCount;
	BufferedImage shóma;
	BufferedImage icons;
	BufferedImage book;
	BufferedImage runebase;
	BufferedImage petricite;
	PropertyResourceBundle z;
	Locale locale;
	@SuppressWarnings("unchecked")
	AssetManager(DataDragon dragon,String locale,boolean isCustomLocale) throws IOException{
		Locale l = null;
		JSList localeData = JSON.asJSList(dragon.fetchObject("languages.json"), true);
		if(locale == null){
			locale = (String)dragon.fetchObject("locale.txt");
		}else if(isCustomLocale){
			if(!localeData.list.contains(locale)){
				locale = (String)dragon.fetchObject("locale.txt");
			}else{
				dragon.fetchObject("locale.txt");
			}
		}else{
			dragon.putString("locale.txt", locale);//This time we got to retrieve client locale. Store it.
		}
		List<String> localeList = (List<String>)(List<?>)localeData.list;
		{
			StringBuilder bld = new StringBuilder(localeData.size()*6);
			for(String str : localeList){
				bld.append(str);
				bld.append(',');
			}
			bld.setLength(bld.length()-1);
			listOfLocales = bld.toString();
		}
		JSMap manifest = dragon.getManifest();
		JSMap n = manifest.getJSMap("n");
		String en = manifest.getString("l");
		if(locale == null){//We have yet to retrieve the locale from the LCU. Use data dragon.
			l = Locale.getDefault();
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
		{//Kayn is hardcoded because his runes depend on his passive which in turn depends on the composition.
			//Id is their "internal name". Kayn gets two extra ids.
			TreeMap<String,Champion> champById = new TreeMap<>();
			//Key is a number unique to each champion and is used by the champion selection. Kayn has a single id like any other champion.
			TreeMap<Number,Champion> champByKey = new TreeMap<>(KEY_COMPARATOR);
			//A hash map of champions by their english name. Used by the mobafire parser.
			HashMap<String,Champion> champByEng = new HashMap<>();
			//A mapping of tag lists. Used to eliminate redundancy.
			HashMap<Object,List<String>> tagListIndex = new HashMap<>();
			championCount = championData.map.size();
			Champion[] championArray = new Champion[championCount];
			int index = 0;
			Champion kayn = null;//Kayn.
			Variant ass = null,slay = null;//Kayn's two forms.
			for(Entry<String,Object> entry : championData.map.entrySet()){
				String key = entry.getKey();
				JSMap champion = JSON.asJSMap(entry.getValue(),true);
				List<String> championTagList = (List<String>)(List<?>)champion.getJSList("tags").list;
				switch(championTagList.size()){
				case 0:
					championTagList = OrdinalSet.emptyList();
					break;
				case 1:
					championTagList = tagListIndex.computeIfAbsent(championTagList.get(0),OrdinalSet.SingletonOrdinalSet::new);
					break;
				default:
					List<String> alt = tagListIndex.get(championTagList);
					if(alt == null){//Should happen no more than n factorial where n is the length of the list. 2 in all currently known cases.
						alt = new NavigableList.StringList(championTagList);
						List<String> alt1 = tagListIndex.putIfAbsent(alt,alt);
						if(alt1 == null){
							for(String str : alt){
								tagListIndex.computeIfAbsent(str,OrdinalSet.SingletonOrdinalSet::new);
							}
						}else{
							alt = alt1;
						}
						tagListIndex.putIfAbsent(championTagList,alt);
					}
					championTagList = alt;
				}
				JSMap image = champion.getJSMap("image");
				Number id = champion.getNumber("key");
				Champion ch = new Champion(
						key,
						champion.getString("name"),
						id,
						((BufferedImage)dragon.fetchObject(n.getString("champion")+"/img/sprite/"+image.getString("sprite")))
						.getSubimage(image.getInt("x"), image.getInt("y"), image.getInt("w"), image.getInt("h")),
						championTagList);
				an(champByEng.put(englishChampionData.getJSMap(key).getString("name").toUpperCase(Locale.ROOT),ch));
				if(key.equals("Kayn")){
					kayn = ch;
					ass = new Variant(ch,"ass",loadImageWithHash("kayn_ass_square.png",2624372905l));
					slay = new Variant(ch,"slay",loadImageWithHash("kayn_slay_square.png",4243224101l));
					an(champById.put("Kaynass",ass));
					an(champById.put("Kaynslay",slay));
				}else{
					an(champById.put(key,ch));
				}
				champByKey.put(ch.key,ch);
				championArray[index] = ch;
				index++;
			}
			ArrayList<String> localTagList = new ArrayList<>(7);
			for(List<String> nls : tagListIndex.values()){
				if(nls.size() == 1){
					String tag = nls.get(0);
					int d = Collections.binarySearch(localTagList,tag);
					if(d < 0){
						localTagList.add(-1-d,tag);
					}
				}
			}
			Champion[] nameSort = championArray.clone();
			tagList = new NavigableList.StringList(localTagList);
			Arrays.sort(championArray,Comparator.comparing(a -> a.key,KEY_COMPARATOR));
			Arrays.sort(championArray,null);
			//This will error if Kayn is not present in the champion database.
			champById.put(kaynId = kayn.toString(),kayn);
			championVariants = Map.of(kayn,List.of(ass,slay));
			championList = new Champion.ChampionList(championArray);
			championsByKey = ImmutableOrdinalMap.of(champByKey,Number.class);
			championsById = ImmutableOrdinalMap.of(champById,String.class);
			championsByEnglishName = Collections.unmodifiableMap(champByEng);
			championsSortedByName = new ArrayListView<>(nameSort);
		}
		icons = loadImageWithHash("icons.png",2358622739l);//Absolutely NO tampering please!
		runebase = loadImageWithHash("runebase.png",4054446114l);
		book = loadImageWithHash("book.png",1433901601l);
		petricite = loadImageWithHash("petricite.jpg",1048823524l);
		shóma = loadImageWithHash("shóma.jpg",4091669783l);
		tintedIcons.put(Color.WHITE,icons);
	}
	public boolean hasSubChampions(Champion ch){
		return ch.toString() == kaynId;
	}
	public String getRoleName(int role){
		return z.getString(ROLE_NAMES[role]);
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
	public Image image(int imgx,int imgy){
		return icons.getSubimage(imgx*24, imgy*24, 24, 24);
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
	private HashMap<Color,BufferedImage> tintedIcons = new HashMap<>();
	public class RoleIcon implements Icon{
		private byte roles;
		private Color color = Color.WHITE;
		public RoleIcon(){}
		public RoleIcon(byte roles){
			this.roles = roles;
		}
		public RoleIcon(byte roles,Color color){
			this.roles = roles;
			this.color = color;
		}
		public void setRoles(byte r){
			this.roles = r;
		}
		public byte getRoles(){
			return roles;
		}
		public void setColor(Color color){
			this.color = color;
		}
		@Override
		public void paintIcon(Component c, Graphics g, int x, int y) {
			paintRoleIcon(g, roles, color, x, y);
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
	private BufferedImage tintIcons(Color color){
		BufferedImage newImage = new BufferedImage(96, 72, BufferedImage.TYPE_INT_ARGB);
		for(int y = 0;y < 72;y++){
			for(int x = 0;x < 96;x++){
				int rgb = icons.getRGB(x, y);
				if(rgb == 0xffffffff){
					rgb = color.getRGB();
				}
				newImage.setRGB(x, y, rgb);
			}
		}
		return newImage;
	}
	public void paintRoleIcon(Graphics gr,byte roles,Color color,int x,int y){
		assert EventQueue.isDispatchThread();
		BufferedImage i = tintedIcons.computeIfAbsent(color, this::tintIcons);
		BufferedImage g = tintedIcons.computeIfAbsent(Color.GRAY, this::tintIcons);
		roles &= 0x1F;
		if(roles == 0x1F){//Special case: All roles
			rimg(i,gr, x, y, 3, 2);
		}else if(roles == 0x1D){//Special case: All but mid
			rimg(i,gr, x, y, 1, 2);
		}else{boolean top = (roles & TOP) != 0,mid = (roles & MIDDLE) != 0,bot = (roles & BOTTOM) != 0;
			if((roles & 0x18) == 0){//Laning roles only
				if(mid){
					rimg(top ? i : g,gr, x, y, 0, 1);
					rimg(i			,gr, x, y, 1, 1);
					rimg(bot ? i : g,gr, x, y, 2, 1);
				}else{
					rimg(top ? i : g,gr, x, y, 0, 0);
					rimg(g			,gr, x, y, 1, 0);
					rimg(bot ? i : g,gr, x, y, 2, 0);
				}
			}else if((roles & 0x18) == roles){//No lanes
				switch(roles){
				case JUNGLE:rimg(i,gr, x, y, 3, 0);break;//Jungle
				case SUPPORT:rimg(i,gr, x, y, 3, 1);break;//Support
				default:
					icog(i,gr, x+2, y+2, false,false);
					icog(i,gr, x+10, y+10, true,true);
				}
			}else if((roles & 0x18) != 0x18){//Laning roles and either jungle or support.
				if(mid){
					icog(i,gr, x+12, y, true, false);
					icog(i,gr, x, y+12, false, true);
					rimg(top ? i : g,gr, x, y, 0, 1);
					rimg(bot ? i : g,gr, x, y, 2, 1);
				}else{
					rimg(top ? i : g,gr, x, y, 0, 0);
					rimg(bot ? i : g,gr, x, y, 2, 0);
				}
				boolean isSup = (roles & 0x18) == SUPPORT;
				icog(i,gr, x+6,y+6, isSup, isSup);
			}else if(roles == 0x1A){//Special case: mid support jungle
				rimg(i			,gr, x, y, 1, 1);
				icog(i,gr, x, y, false,false);
				icog(i,gr, x+12, y+12, true,true);
			}else{
				if(mid){//Both jungle and support. Either top or bot.
					icog(i,gr, x+12, y, true, false);
					icog(i,gr, x, y+12, false, true);
				}
				if(top){
					rimg(i,gr, x, y, 0, mid ? 1 : 0);
					icog(i,gr, x+5, y+5, false,false);
					icog(i,gr, x+12, y+12, true,true);
				}else if(bot){
					rimg(i,gr, x, y, 2, mid ? 1 : 0);
					icog(i,gr, x, y, false,false);
					icog(i,gr, x+7, y+7, true,true);
				}
			}
		}
	}
	private static void rimg(Image icons,Graphics gr,int tx,int ty,int ix,int iy){
		ix *= 24;
		iy *= 24;
		gr.drawImage(icons, tx, ty, tx+24, ty+24, ix, iy, ix+24, iy+24, null);
	}
	private static void icog(Image icons,Graphics gr,int tx,int ty,boolean x,boolean y){
		int ix = x ? 12 : 0;
		int iy = y ? 60 : 48;
		gr.drawImage(icons, tx, ty, tx+12, ty+12, ix, iy, ix+12, iy+12, null);
	}
}
