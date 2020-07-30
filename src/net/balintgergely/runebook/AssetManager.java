package net.balintgergely.runebook;

import java.util.Collections;
import java.util.HashSet;
import java.util.TreeMap;
import java.util.zip.CRC32;
import java.util.Map.Entry;

import javax.imageio.ImageIO;

import java.util.NavigableMap;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import net.balintgergely.util.JSList;
import net.balintgergely.util.JSMap;
import net.balintgergely.util.JSON;

class AssetManager {
	public static final byte	TOP = 0x1,
								MIDDLE = 0x2,
								BOTTOM = 0x4,
								JUNGLE = 0x8,
								SUPPORT = 0x10;
	final RuneModel runeModel;
	final NavigableMap<String,Champion> champions;
	BufferedImage windowIcon;
	BufferedImage iconSprites;
	BufferedImage background;
	AssetManager(DataDragon dragon,String gameVersion) throws IOException{
		JSMap championData = JSON.asJSMap(dragon.fetchObject(gameVersion+"/data/en_US/championFull.json"), true).getJSMap("data");
		JSList runeData = JSON.asJSList(dragon.fetchObject(gameVersion+"/data/en_US/runesReforged.json"), true);
		HashSet<String> imageCollection = new HashSet<>();
		JSON.forEachMapEntry(championData, (String key,Object value) -> {
			if("image".equals(key)){
				imageCollection.add(gameVersion+"/img/sprite/"+JSON.asJSMap(value, true).getString("sprite"));
			}
		},1);
		JSON.forEachMapEntry(runeData, (String key,Object value) -> {
			if("icon".equals(key)){
				imageCollection.add("img/"+JSON.asString(value, true, null));
			}
		},-1);
		String iconPath = gameVersion+"/img/spell/"+championData.getJSMap("Yuumi").getJSList("spells").getJSMap(3).getJSMap("image").getString("full");
		imageCollection.add(iconPath);
		dragon.batchPreload(imageCollection);
		{//Rune Book? Book with power? Why not? Also the reason we need championFull.json which is kinda big. In the future we can improve on this.
			BufferedImage windIc = (BufferedImage)dragon.fetchObject(iconPath);//Here, Ryze's passive icon is another big candidate.
			int hx = windIc.getWidth()/2,hy = windIc.getHeight()/2;//Unsealed Spellbook however is not the way to go due to being a keystone.
			windowIcon = windIc.getSubimage(0, hy, hx, hy);
		}
		runeModel = new RuneModel(runeData, (String str) -> (BufferedImage)dragon.fetchObject("img/"+str));
		TreeMap<String,Champion> champ = new TreeMap<>();
		for(Entry<String,Object> entry : championData.map.entrySet()){
			String name = entry.getKey();
			JSMap champion = JSON.asJSMap(entry.getValue(),true);
			JSMap image = champion.getJSMap("image");
			champ.put(name,new Champion(name,champion.getString("name"),
					(BufferedImage)dragon.fetchObject(gameVersion+"/img/sprite/"+image.getString("sprite")),
					image.getInt("x"), image.getInt("y"), image.getInt("w"), image.getInt("h")));
		}
		champions = Collections.unmodifiableNavigableMap(champ);
		iconSprites = loadImageWithHash("icons.png",990741473);//Absolutely NO tampering please!
		background = loadImageWithHash("background.png",1433901601);
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
	Champion[] getChampions(){
		return champions.values().toArray(Champion[]::new);
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
	public void paintRoleIcon(Graphics gr,byte roles,int x,int y){
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
					icog(gr, x+2, y+2, false);
					icog(gr, x+10, y+10, true);
				}
			}else if((roles & 0x18) != 0x18){//Laning roles and either jungle or support.
				if(mid){
					rimg(gr, x, y, 1, 3);
					rimg(gr, x, y, 0, top ? 3 : 2);
					rimg(gr, x, y, 2, bot ? 3 : 2);
				}else{
					rimg(gr, x, y, 0, top ? 1 : 0);
					rimg(gr, x, y, 2, bot ? 1 : 0);
				}
				icog(gr, x+6,y+6, (roles & 0x18) == SUPPORT);
			}else{//Both jungle and support. Either top or bot.
				if(mid){
					rimg(gr, x, y, 1, 3);
				}
				if(top){
					rimg(gr, x, y, 0, mid ? 3 : 1);
					icog(gr, x+5, y+5, false);
					icog(gr, x+12, y+12, true);
				}else{
					rimg(gr, x, y, 2, mid ? 3 : 1);
					icog(gr, x, y, false);
					icog(gr, x+7, y+7, true);
				}
			}
		}
	}
	private void rimg(Graphics gr,int tx,int ty,int ix,int iy){
		ix *= 24;
		iy *= 24;
		gr.drawImage(iconSprites, tx, ty, tx+24, ty+24, ix, iy, ix+24, iy+24, null);
	}
	private void icog(Graphics gr,int tx,int ty,boolean b){
		int ix = b ? 36 : 24;
		int iy = b ? 60 : 48;
		gr.drawImage(iconSprites, tx, ty, tx+12, ty+12, ix, iy, ix+12, iy+12, null);
	}
}
