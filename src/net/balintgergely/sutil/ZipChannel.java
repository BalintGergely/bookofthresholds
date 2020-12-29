package net.balintgergely.sutil;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.EOFException;
import java.io.File;
import java.io.FilterInputStream;
import java.io.Flushable;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashMap;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.zip.CRC32;
import java.util.zip.CheckedInputStream;
import java.util.zip.Deflater;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;
import java.util.ArrayList;
/**
 * A facility to read and write the zip portion of files.<br>
 * Why? Because Java has no builtin support for caching.<br>
 * Why zip? Because of how convenient it is to take a look at the cache in the explorer.<br>
 * Zip is outdated. Zip64 should exist as an entirely separate format, much less an extension.<br>
 * @author balintgergely
 */
public class ZipChannel implements Flushable, Closeable, Map<String,ZipChannel.ZipEntry>{
	@SuppressWarnings("unused")
	private final static int ENDSIG = 0x06054b50,CENSIG = 0x02014b50,LOCSIG = 0x04034b50,ENTSIG = 0x04034b50,
							ZIP64_LOCSIG = 0x07064b50,ZIP64_ENDSIG = 0x06064b50;
	private final static long ZIP_64_TRESHOLD = 0xffffffffl;
	//private final static short DD_FLAG = 0x08;
	private final static byte[] EMPTY_ARRAY = new byte[0];
	private final static Comparator<ZipEntry> OFFSET_COMPARATOR = (a,b) -> Long.compare(a.offset,b.offset);
	private static byte[] allocate(int length){
		return length == 0 ? EMPTY_ARRAY : new byte[length];
	}
	private static void put64(byte[] data,int index,long value){
		data[index  ] =	(byte)(value);
		data[index+1] =	(byte)(value >> 0x8);
		data[index+2] =	(byte)(value >> 0x10);
		data[index+3] =	(byte)(value >> 0x18);
		data[index+4] =	(byte)(value >> 0x20);
		data[index+5] =	(byte)(value >> 0x28);
		data[index+6] =	(byte)(value >> 0x30);
		data[index+7] =	(byte)(value >> 0x38);
	}
	private static void put32(byte[] data,int index,int value){
		data[index] =	(byte)(value);
		data[index+1] =	(byte)(value >> 0x8);
		data[index+2] =	(byte)(value >> 0x10);
		data[index+3] =	(byte)(value >> 0x18);
	}
	private static void put16(byte[] data,int index,int value){
		data[index] =	(byte)(value);
		data[index+1] =	(byte)(value >> 0x8);
	}
	private static long get64(byte[] data,int index){
		return	( data[index  ] & 0xffl) |
				((data[index+1] & 0xffl) << 0x8) |
				((data[index+2] & 0xffl) << 0x10) |
				((data[index+3] & 0xffl) << 0x18) |
				((data[index+4] & 0xffl) << 0x20) |
				((data[index+5] & 0xffl) << 0x28) |
				((data[index+6] & 0xffl) << 0x30) |
				((data[index+7] & 0xffl) << 0x38);
	}
	private static int get32(byte[] data,int index){
		return	( data[index  ] & 0xff) |
				((data[index+1] & 0xff) << 0x8) |
				((data[index+2] & 0xff) << 0x10) |
				((data[index+3] & 0xff) << 0x18);
	}
	private static int get16(byte[] data,int index){
		return	( data[index  ] & 0xff) |
				((data[index+1] & 0xff) << 0x8);
	}
	public static void main(String[] atgs) throws Throwable{
		File file = new File("dataDragon.zip");
		//file.delete();
		try(ZipChannel channel = new ZipChannel(file, true)){
			System.out.println("Offset: "+channel.getStartPosition()+" Entry count: "+channel.entryList.size());
			Iterator<ZipEntry> itr = channel.values().iterator();
			while(itr.hasNext()){
				ZipEntry entry = itr.next();
				try{
					boolean singleton = channel.entryMap.containsKey(entry.name);
					if(entry.getName().equals("languages.json")){
						Thread.currentThread();
					}
					CheckedInputStream cin = new CheckedInputStream(entry.getInputStream(), new CRC32());
					cin.skip(Long.MAX_VALUE);
					boolean matches = ((int)cin.getChecksum().getValue()) == entry.getCRC32();
					System.out.println(entry.name+" s: "+singleton+" m: "+matches);
				}catch(Throwable t){
					System.out.println(entry.name+" read error!");
					t.printStackTrace(System.out);
				}
			}
		}
	}
	public final File file;
	/**
	 * The ever-constant startPosition is the start of the zip file inside the channel.
	 * We do not modify content before this point ever.
	 */
	private long startPosition;
	private Map<String,ZipEntry> entryMap;
	private List<Reference<ZipEntry>> entryList;
	private byte[] fileComment;
	//private int directoryLocation = 0;
	private RandomAccessFile channel;
	private long autoFlushTreshold = 0x1000000;
	private long cachedBytes;
	private int readInt() throws IOException{
		int ch1 = channel.read();
		int ch2 = channel.read();
		int ch3 = channel.read();
		int ch4 = channel.read();
		if ((ch1 | ch2 | ch3 | ch4) < 0){
			throw new EOFException();
		}
		return (ch1 + (ch2 << 8) + (ch3 << 0x10) + (ch4 << 0x18));
	}
	private void writeLong(long v) throws IOException{
		channel.write((int)(v) & 0xFF);
		channel.write((int)(v >>> 0x08) & 0xFF);
		channel.write((int)(v >>> 0x10) & 0xFF);
		channel.write((int)(v >>> 0x18) & 0xFF);
		channel.write((int)(v >>> 0x20) & 0xFF);
		channel.write((int)(v >>> 0x28) & 0xFF);
		channel.write((int)(v >>> 0x30) & 0xFF);
		channel.write((int)(v >>> 0x38) & 0xFF);
	}
	private void writeInt(int v) throws IOException{
		channel.write(v & 0xFF);
		channel.write((v >>> 0x08) & 0xFF);
		channel.write((v >>> 0x10) & 0xFF);
		channel.write((v >>> 0x18) & 0xFF);
	}
	private void writeShort(int v) throws IOException{
		channel.write(v & 0xFF);
		channel.write((v >>> 8) & 0xFF);
	}
	public ZipChannel(File file0,boolean write) throws IOException {
		channel = new RandomAccessFile(file = file0, write ? "rwd" : "r");
		boolean success = false;
		try{
			readDirectory(write);
			success = true;
		}finally{
			if(!success){
				channel.close();
			}
		}
	}
	public void setAutoFlushTreshold(long treshold){
		this.autoFlushTreshold = treshold;
	}
	public long getStartPosition(){
		return startPosition;
	}
	public long getZipLength() throws IOException{
		return channel.length()-startPosition;
	}
	public String getFileComment(){
		return new String(fileComment,StandardCharsets.UTF_8);
	}
	public void setFileComment(String str){
		fileComment = str.getBytes(StandardCharsets.UTF_8);
	}
	private static final int
			END_HDR_LENGTH = 22,
			END_64_LENGTH = 20,
			HDR_64_LENGTH = 56,
			CEN_HDR_LENGTH = 46,
			LOC_HDR_LENGTH = 30,
			CEN_HDR_STORE = 0,
			HDR_64_STORE = CEN_HDR_STORE+CEN_HDR_LENGTH,
			END_64_STORE = HDR_64_STORE+HDR_64_LENGTH,
			END_STORE = END_64_STORE+END_64_LENGTH,
			END_BUFFER_LENGTH = END_STORE+END_HDR_LENGTH,
			EXTID_ZIP64 = 0x0001,
			METHOD_DEFLATED = 0x8,
			METHOD_STORED = 0;
	@SuppressWarnings("unchecked")
	private void readDirectory(boolean forWrite) throws IOException{
		//Shamelessly copied and altered from java.util.zip.ZipFile.
		long cenlen,cenoff,endpos,centot;
		int comlen;
		//ByteBuffer buf = ByteBuffer.allocate(END_BUFFER_LENGTH).order(ByteOrder.LITTLE_ENDIAN);
		byte[] buf = new byte[END_BUFFER_LENGTH];
		findEnd:{
			long ziplen = channel.length();
			long minHDR = ziplen - (0xffff + END_HDR_LENGTH);
			if(minHDR < 0){
				minHDR = 0;
			}
			long minPos = minHDR - (END_BUFFER_LENGTH - END_HDR_LENGTH);
			for (long pos = ziplen - END_BUFFER_LENGTH; pos >= minPos; pos -= (END_BUFFER_LENGTH - END_HDR_LENGTH)) {
				int off = 0;
				if (pos < 0) {
					off = (int)-pos;
				}
				int len = END_BUFFER_LENGTH - off;
				channel.seek(pos + off);
				channel.readFully(buf, off, len);
				// Now scan the block backwards for END header signature
				for (int i = END_BUFFER_LENGTH - END_HDR_LENGTH; i >= off; i--) {
					if (get32(buf,i) == ENDSIG){
						cenlen = get32(buf,i+12) & 0xffffffffl;
						cenoff = get32(buf,i+16) & 0xffffffffl;
						endpos = pos + i;
						comlen = get16(buf,i+20);
						if (endpos + END_HDR_LENGTH + comlen != ziplen) {
							//Original code performs some verifications. (Here optimized.)
							long cenpos = endpos - cenlen;
							long locpos = cenpos - cenoff;
							if(cenpos < 0 || locpos < 0 || cenpos+4 > ziplen || locpos+4 > ziplen){
								i -= 3;//No bytes of ENDSIG match, therefore we skip checking the same bytes that had originally matched in a different offset.
								continue;
							}
							channel.seek(cenpos);
							if(readInt() != CENSIG){
								i -= 3;
								continue;
							}
							channel.seek(locpos);
							if(readInt() != LOCSIG){
								i -= 3;
								continue;
							}
						}
						centot = get16(buf,i+10) & 0xffff;
						System.arraycopy(buf, i, buf, END_STORE, END_HDR_LENGTH);
						break findEnd;
					}
				}
			}
			startPosition = channel.length();
			fileComment = EMPTY_ARRAY;
			entryMap = forWrite ? new ConcurrentHashMap<>() : Map.of();
			entryList = forWrite ? new CopyOnWriteArrayList<>() : null;
			return;
		}
		if (comlen > 0) {
			fileComment = new byte[comlen];
			channel.seek(endpos+END_HDR_LENGTH);
			int comPos = 0,res;
			while(comPos < comlen && (res = channel.read(fileComment, comPos, comlen-comPos)) > 0){
				comPos += res;
			}
			if(comlen != comPos){
				fileComment = Arrays.copyOf(fileComment, comPos);
			}
		}else{
			fileComment = EMPTY_ARRAY;
		}
		// must check for a zip64 end record; it is always permitted to be present
		zip64: if(endpos >= END_64_LENGTH){
			channel.seek(endpos-END_64_LENGTH);
			channel.readFully(buf,END_64_STORE,END_64_LENGTH);
			long end64pos = get64(buf,END_64_STORE+8);
			if(get32(buf,END_64_STORE) != ZIP64_LOCSIG || end64pos < 0 || end64pos > endpos-(END_64_LENGTH+HDR_64_LENGTH)){
				break zip64;
			}
			channel.seek(end64pos);
			channel.readFully(buf,HDR_64_STORE,HDR_64_LENGTH);
			if(get32(buf,HDR_64_STORE) != ZIP64_ENDSIG){
				break zip64;
			}
			// end64 candidate found,
			long centot64 = get64(buf,HDR_64_STORE+32);
			long cenlen64 = get64(buf,HDR_64_STORE+40);
			long cenoff64 = get64(buf,HDR_64_STORE+48);
			// double-check
			if (cenlen64 != cenlen && cenlen != 0xffffffffl ||
				cenoff64 != cenoff && cenoff != 0xffffffffl ||
				centot64 != centot && centot != 0xffff) {
				break zip64;
			}
			// to use the end64 values
			cenlen = cenlen64;
			cenoff = cenoff64;
			centot = centot64;
			endpos = end64pos;
		}
		if(centot < 0 || centot > Integer.MAX_VALUE){
			throw new IOException("Too many entries!");
		}
		if(cenlen > endpos){
			throw new IOException("Bad central directory size!");
		}
		long cenpos = endpos - cenlen;
		startPosition = cenpos - cenoff;//locoff
		if(startPosition < 0){
			throw new IOException("Bad central directory offset!");
		}
		channel.seek(cenpos);
		entryMap = forWrite ? new ConcurrentHashMap<>((int)centot) : new HashMap<>((int)centot);
		ArrayList<?> strongEntryList = forWrite ? new ArrayList<>((int)centot) : null;
		readCen: while((cenpos + CEN_HDR_LENGTH) <= endpos){
			channel.readFully(buf, CEN_HDR_STORE, CEN_HDR_LENGTH);
			cenpos += CEN_HDR_LENGTH;
			if(get32(buf, CEN_HDR_STORE) != CENSIG){
				break readCen;
			}
			ZipEntry entry = new ZipEntry(buf);
			int nameLength = entry.getFileNameLength();
			int extraLength = entry.getExtraFieldLength();
			int commentLength = entry.getCommentLength();
			int totalExtraLength = nameLength+extraLength+commentLength;
			cenpos += totalExtraLength;
			if(cenpos > endpos){
				break readCen;
			}
			byte[] nameBytes = allocate(nameLength);
			byte[] headerExtraArray = allocate(extraLength);
			byte[] commentBytes = allocate(commentLength);
			channel.readFully(nameBytes);
			channel.readFully(headerExtraArray);
			channel.readFully(commentBytes);
			long relativeOffset = entry.getLocation();
			zip64: if(relativeOffset == 0xffffffffl){//We need to dig into the extra
				int index = 0;
				while (index+12 < headerExtraArray.length) {
					int tag = get16(headerExtraArray,index);
					int sz = get16(headerExtraArray,index+2);
					index += 4;
					if(tag == EXTID_ZIP64){
						if(entry.getUncompressedSize() == 0xffffffff && sz >= 8){//Uncompressed size first!
							index += 8;
							sz -= 8;
						}
						if(entry.getCompressedSize() == 0xffffffff && sz >= 8){
							index += 8;
							sz -= 8;
						}
						if(sz >= 8){
							relativeOffset = get64(headerExtraArray,index);
							entry.extra64 = index;//First half is always zero at this point.
							break zip64;
						}
					}
					index += sz;
				}
				continue readCen;
			}
			entry.header = Arrays.copyOf(buf,CEN_HDR_LENGTH);
			entry.nameBytes = nameBytes;
			entry.cenExtra = headerExtraArray;
			entry.commentBytes = commentBytes;
			entry.name = new String(nameBytes,StandardCharsets.UTF_8);
			entry.offset = relativeOffset;
			if(forWrite){
				((ArrayList<ZipEntry>)strongEntryList).add(entry);
			}
			entryMap.put(entry.name,entry);
		}
		if(forWrite){
			if(!strongEntryList.isEmpty()){
				((ArrayList<ZipEntry>)strongEntryList).sort(OFFSET_COMPARATOR);
				ZipEntry ked = (ZipEntry)strongEntryList.get(0);
				int len = strongEntryList.size();
				for(int i = 1;i < len;i++){
					ZipEntry kad = (ZipEntry)strongEntryList.get(i);
					ked.maximumLength = kad.offset-ked.offset;
					ked = kad;
				}
				ked.maximumLength = cenoff-ked.offset;
				((ArrayList<Object>)strongEntryList).replaceAll(WeakReference::new);
			}
			entryList = new CopyOnWriteArrayList<>((ArrayList<Reference<ZipEntry>>)strongEntryList);
		}
	}
	@Override
	public void flush() throws IOException{
		if(entryList != null){
			synchronized(channel){
				int len = entryList.size();
				int index = 0;
				ZipEntry lastEncounter = null;
				while(index < len){//Phase 1: Seek out first index of override
					ZipEntry entry = entryList.get(index).get();
					if(entry == null || entry.data != null || entry.cenExtra == null){
						break;
					}
					lastEncounter = entry;
					index++;
				}
				if(index == len){
					return;
				}
				long writeLocation = lastEncounter == null ? startPosition : startPosition+lastEncounter.offset+lastEncounter.maximumLength;
				byte[] transferBuffer = null;
				for(;index < len;index++){//Phase 2: Move entries
					Reference<ZipEntry> wrf = entryList.get(index);
					ZipEntry entry = wrf.get();
					if(entry != null){
						if(entry.cenExtra == null){
							entry.offset = 0-startPosition-entry.maximumLength;
							wrf.clear();
						}else{
							long entryOffset = writeLocation-startPosition;
							entry.prepLoc();
							channel.seek(writeLocation);
							writeInt(LOCSIG);
							channel.write(entry.header, 6, 26);
							channel.write(entry.nameBytes);
							channel.write(entry.localExtra);
							if(entry.data == null){
								long readLocation = LOC_HDR_LENGTH+startPosition+entry.offset+entry.nameBytes.length+entry.localExtra.length;
								long remaining = entry.getCompressedSize64();
								if(transferBuffer == null){
									transferBuffer = new byte[0x4000];
								}
								writeLocation = channel.getFilePointer();
								while(remaining >= 0x4000){
									channel.seek(readLocation);
									channel.readFully(transferBuffer);
									channel.seek(writeLocation);
									channel.write(transferBuffer);
									readLocation += 0x4000;
									writeLocation += 0x4000;
									remaining -= 0x4000;
								}
								if(remaining != 0){
									channel.seek(readLocation);
									channel.readFully(transferBuffer, 0, (int)remaining);
									channel.seek(writeLocation);
									channel.write(transferBuffer, 0, (int)remaining);
									writeLocation += remaining;
								}
							}else{
								//System.out.println(entry.getName());
								//If entryOffset so demands it, we need to enable zip64 for the entry we are about to insert.
								//Since entry data length is guaranteed not to be above treshold, first half of the extra64 field is 0.
								if(entryOffset >= ZIP_64_TRESHOLD && entry.extra64+8 > entry.cenExtra.length){
									byte[] nce = new byte[12+entry.cenExtra.length];
									System.arraycopy(entry.cenExtra, 0, nce, 12, entry.cenExtra.length);
									put16(nce, 0, EXTID_ZIP64);
									put16(nce, 2, 8);
									entry.extra64 = 4;
									entry.cenExtra = nce;
								}
								channel.write(entry.data);
								entry.data = null;
								writeLocation = channel.getFilePointer();
							}
							entry.offset = entryOffset;
							entry.maximumLength = writeLocation-entryOffset;
						}
					}
				}
				cachedBytes = 0;
				channel.seek(writeLocation);
				int count = 0;
				for(int i = 0;i < len;i++){//Phase 3: Cen. Note that it is possible that some entries get gc'd between this and phase 2.
					ZipEntry entry = entryList.get(i).get();
					if(entry != null){
						entry.prepCen();
						channel.write(entry.header);
						channel.write(entry.nameBytes);
						channel.write(entry.cenExtra);
						channel.write(entry.commentBytes);
						count++;
					}
				}
				long cenLength = channel.getFilePointer()-writeLocation;
				writeLocation -= startPosition;//From this point writeLocation is the cen position.
				entryList.removeIf((a) -> a.get() == null);
				if(writeLocation >= ZIP_64_TRESHOLD || cenLength >= ZIP_64_TRESHOLD || count >= 0xffff){
					writeInt(ZIP64_ENDSIG);        // zip64 END record signature
					writeLong(HDR_64_LENGTH - 12);  // size of zip64 end
					writeShort((short)45);                // version made by
					writeShort((short)45);                // version needed to extract
					//16
					writeInt(0);                   // number of this disk
					writeInt(0);                   // central directory start disk
					writeLong(count);    // number of directory entires on disk
					//32
					writeLong(count);    // number of directory entires
					writeLong(cenLength);                // length of central directory
					//48
					writeLong(writeLocation);                // offset of central directory
					//56
				
					//zip64 end of central directory locator
					writeInt(ZIP64_LOCSIG);        // zip64 END locator signature
					writeInt(0);                   // zip64 END start disk
					//64
					writeLong(writeLocation+cenLength);              // offset of zip64 END
					//72
					writeInt(1);                   // total number of disks (?)
					//76
					count = count >= 0xffff ? 0xffff : count;
					cenLength = cenLength >= ZIP_64_TRESHOLD ? 0xffffffffl : cenLength;
					writeLocation = writeLocation >= ZIP_64_TRESHOLD ? 0xffffffffl : writeLocation;
				}
				writeInt(ENDSIG);
				writeShort((short)0);// number of this disk
				writeShort((short)0);// central directory start disk
				//8
				writeShort((short)count);// number of directory entries on disk
				writeShort((short)count);// total number of directory entries
				//12
				writeInt((int)cenLength);// length of central directory
				writeInt((int)writeLocation);// offset of central directory
				//20
				writeShort((short)fileComment.length);
				channel.write(fileComment);
				channel.setLength(channel.getFilePointer());
			}
		}
	}
	public void clearUnreachableEntries(){
		synchronized(channel){
			for(Reference<ZipEntry> wr : entryList){
				ZipEntry entry = wr.get();
				if(entry != null && entryMap.get(entry.name) != entry){
					entry.cenExtra = null;
				}
			}
		}
	}
	public ZipEntry clearEntry(Object name){
		synchronized(channel){
			return entryMap.remove(name);
		}
	}
	public ZipEntry putEntry(ZipEntry entry) throws IOException{
		entry.check(this);
		synchronized(channel){
			if(entry.cenExtra == null){
				throw new IllegalArgumentException();
			}
			if(entry.offset == -1){
				entryList.add(new WeakReference<ZipChannel.ZipEntry>(entry));
				cachedBytes += entry.data.length;
				entry.offset = -2;
			}
			entry = entryMap.put(entry.name,entry);
			if(cachedBytes >= autoFlushTreshold){
				flush();
			}
			return entry;
		}
	}
	/**
	 * Forcibly deletes the specified entry. It's data will still be available for read
	 * until the next flush.
	 */
	public void deleteEntry(ZipEntry entry){
		entry.check(this);
		synchronized(channel){
			entry.cenExtra = null;
			entryMap.remove(entry.name,entry);
		}
	}
	@Override
	public void close() throws IOException {
		synchronized(channel){
			try(Closeable ch = channel){
				flush();
			}
		}
	}
	/**
	 * During the lifecycle of a ZipChannel, ZipEntries may only be created or deleted but not overridden.
	 * ZipChannel only moves ZipEntries closer towards the beginning of the file and never rearranges them.
	 * If two entries share the same name, the one that came later in the cen "hides" the former.
	 * 
	 * We do support a form of "overriding" entries but internally it works by creating a new entry with identical name
	 * that comes after it in the cen and later clearing entries that are "hidden".
	 * 
	 * @author balintgergely
	 *
	 */
	public class ZipEntry{
		byte[] data;
		byte[] header;
		byte[] nameBytes;
		byte[] localExtra;
		byte[] cenExtra;
		byte[] commentBytes;
		/**
		 * Two shorts in one.
		 * <li>Higher order is location of 64 bit length inside the localExtra buffer. 0 if length is not over the treshold.
		 * <li>Lower order is location of 64 bit pointer inside the cenExtra buffer. 0 if pointer is not over the treshold.
		 * Most of the time this field is written or read in it's entirety based on assumptions we can make.
		 */
		int extra64;
		/*
		 * Offset always equal to name length in bytes.
		 * Capacity always equal to total extra data.
		 * Limit is the part of the data contained both in local and cen.
		 * If the file location is in 64 bits, the position is set to point to where the 64 bit location is stored.
		 *
		ByteBuffer headerExtra;*/
		String name;
		long offset = -1;//Actual location is calculated by adding this to startPosition
		/**
		 * The absolute maximum length this entry can have. Including the headers.
		 */
		long maximumLength;
		private ZipEntry(byte[] dt){
			this.header = dt;
		}
		public ZipEntry(String name,byte[] content){
			this(name,content,0,content.length);
		}
		public ZipEntry(String name,byte[] content,int off,int len){
			nameBytes = name.getBytes(StandardCharsets.UTF_8);
			if(nameBytes.length > 0xffff){
				throw new IllegalArgumentException();
			}
			byte[] dtr = new byte[len];
			Deflater dfl = new Deflater(Deflater.DEFAULT_COMPRESSION, true);
			CRC32 crc = new CRC32();
			crc.update(content,off,len);
			dfl.setInput(content,off,len);
			dfl.finish();
			int outputLength = dfl.deflate(dtr);
			header = new byte[CEN_HDR_LENGTH];
			this.name = name;
			localExtra = EMPTY_ARRAY;
			cenExtra = EMPTY_ARRAY;
			commentBytes = EMPTY_ARRAY;
			boolean isCompressed = dfl.finished() && outputLength != len;
			if(isCompressed){
				data = dtr.length == outputLength ? dtr : Arrays.copyOf(dtr, outputLength);
			}else{
				System.arraycopy(content, off, dtr, 0, len);
				data = dtr;
			}
			put32(header, 0, CENSIG);
			put16(header, 4, (short)45);
			put16(header, 6, (short)45);
			put16(header, 8, (short)0x800);
			put16(header, 10,(short)(isCompressed ? METHOD_DEFLATED : METHOD_STORED));
			//Skip last modification time
			put32(header, 16,(int)crc.getValue());
			put32(header, 20,data.length);
			put32(header, 24,len);
			put16(header, 28,(short)nameBytes.length);
			//Extra and comment length left unchanged.
			maximumLength = LOC_HDR_LENGTH+nameBytes.length+data.length;
		}
		private int getCompressionMethod(){
			return get16(header, 10);
		}
		public int getCRC32(){
			return get32(header, 16);
		}
		private int getCompressedSize(){
			return get32(header, 20);
		}
		private int getFlags(){
			return get16(header, 8);
		}
		private long getCompressedSize64(){
			int x = getCompressedSize();
			if(x == 0xffffffff){
				return get64(cenExtra, extra64 >>> 16);
			}
			return x & 0xffffffffl;
		}
		private int getUncompressedSize(){
			return get32(header, 24);
		}
		private int getFileNameLength(){
			return get16(header, 28);
		}
		private int getExtraFieldLength(){
			return get16(header, 30);
		}
		private int getCommentLength(){
			return get16(header, 32);
		}
		private int getLocation(){
			return get32(header, 42);
		}
		private void check(ZipChannel ch){
			if(ch != ZipChannel.this){
				throw new IllegalArgumentException();
			}
		}
		private void readLoc() throws IOException{
			assert Thread.holdsLock(channel);
			if(localExtra == null){
				channel.seek(offset+startPosition);
				if(readInt() != LOCSIG){
					throw new IOException("Invalid entry!");
				}
				channel.readFully(header, 6, 10);
				if((getFlags() & 0x08) == 0x08){
					channel.skipBytes(12);//If this flag is set, the loc crc, clen and ulen fields are unreliable. We use the cen fields.
					channel.readFully(header, 28, 4);
				}else{
					channel.readFully(header, 16, 16);
				}
				int localNameLength = getFileNameLength();
				int localExtraLength = getExtraFieldLength();
				byte[] bt = (localNameLength == nameBytes.length) ? nameBytes : new byte[localNameLength];
				channel.readFully(bt);
				nameBytes = bt;
				if(localExtraLength == 0){
					localExtra = EMPTY_ARRAY;
				}else{
					localExtra = new byte[localExtraLength];
					channel.readFully(localExtra);
				}
				if(getCompressedSize() == 0xffffffff){
					int index = 0;
					while (index+12 < localExtra.length) {
						int tag = get16(localExtra,index);
						int sz = get16(localExtra,index+2);
						index += 4;
						if(tag == EXTID_ZIP64 && sz >= 8){
							extra64 |= (index << 16);
						}
						index += sz;
					}
				}
			}
		}
		/**
		 * Prepares the local header for write.
		 * @throws IOException
		 */
		private void prepLoc() throws IOException{
			readLoc();
			header[8] &= (~0x08);//We do future readers a favor and let them know the length of the entry in advance.
			put16(header, 30, (short)localExtra.length);
		}
		/**
		 * Prepares the CEN header for write.
		 */
		private void prepCen(){
			if(localExtra != null){
				put16(header, 30, (short)cenExtra.length);
				if(offset >= ZIP_64_TRESHOLD){
					put32(header, 42, 0xffffffff);
					put64(cenExtra, extra64 & 0xffff, offset);
				}else{
					put32(header, 42, (int)offset);
				}
			}
		}
		public InputStream getInputStream() throws IOException{
			byte[] d = data;
			if(d == null){
				synchronized(channel){
					readLoc();
				}
				ZipEntryInputStream zin = new ZipEntryInputStream();
				switch(getCompressionMethod()){
				case METHOD_DEFLATED:
					return new InflaterInputStream(zin,new Inflater(true));
				case METHOD_STORED:
					return new BufferedInputStream(zin);
				}
			}else{
				switch(getCompressionMethod()){
				case METHOD_DEFLATED:Inflater inf = new Inflater(true);
					inf.setInput(d);
					return new InflaterInputStream(InputStream.nullInputStream(), inf, 1);//Yes, 1 is a power of 2.
				case METHOD_STORED:
					return new FilterInputStream(new ByteArrayInputStream(d)){};//Good job Orcale!
				}
			}
			throw new IOException("Unknown compression format!");
		}
		public String getName(){
			return name;
		}
		@Override
		public final boolean equals(Object that){
			return this == that;
		}
		@Override
		public final int hashCode(){
			return super.hashCode();
		}
		@Override
		public String toString(){
			return "\""+name+
				"\" m:"+getCompressionMethod()
				+" crc:"+getCRC32()
				+" csize:"+getCompressedSize64()
				+" size:"+getUncompressedSize()
				+" extra:"+getExtraFieldLength();
		}
		private class ZipEntryInputStream extends InputStream {
			private long add,remaining;
			private ZipEntryInputStream() throws IOException{
				synchronized(channel){
					readLoc();
					long rx = LOC_HDR_LENGTH+nameBytes.length+localExtra.length;
					add = rx+startPosition;
					remaining = Math.min(maximumLength-rx, getCompressedSize64());
				}
			}
			@Override
			public synchronized int read() throws IOException {
				throw new Error("Unreachable Code");
				//We always wrap this in something that buffers input.
				/**if(remaining == 0){
					return -1;
				}
				int rt;
				synchronized(channel){
					channel.seek(offset+add);
					rt = channel.readUnsignedByte();
				}
				add++;
				remaining--;
				return rt;**/
			}
			@Override
			public synchronized int read(byte[] b, int off, int len) throws IOException {
				if(remaining >= 0 && remaining < len){
					if(remaining == 0){
						return -1;
					}
					len = (int)remaining;
				}
				synchronized(channel){
					channel.seek(offset+add);
					len = channel.read(b,off,len);//Bound checking done here.
				}
				if(len > 0){
					remaining -= len;
					add += len;
				}
				return len;
			}
			@Override
			public synchronized long skip(long n) throws IOException {
				if(remaining+Long.MIN_VALUE < n+Long.MIN_VALUE){
					n = remaining;
				}
				remaining -= n;
				add += n;
				return n;
			}
			@Override
			public synchronized int available() throws IOException {
				return (remaining < 0 || remaining >= Integer.MAX_VALUE) ? Integer.MAX_VALUE : (int)remaining;
			}
			@Override
			public synchronized void close() throws IOException {
				remaining = 0;
			}
		}
	}
	@Override
	public int size() {
		return entryMap.size();
	}
	@Override
	public boolean isEmpty() {
		return entryMap.isEmpty();
	}
	@Override
	public boolean containsKey(Object key) {
		return entryMap.containsKey(key);
	}
	@Override
	public boolean containsValue(Object value) {
		return (value instanceof ZipEntry && entryMap.get(((ZipEntry)value).name) == value);
	}
	@Override
	public ZipEntry get(Object key) {
		return entryMap.get(key);
	}
	@Override
	public ZipEntry put(String key, ZipEntry entry) {
		if(!entry.name.equals(key)){
			throw new IllegalArgumentException();
		}
		entry.check(this);
		synchronized(channel){
			if(entry.cenExtra == null){
				throw new IllegalArgumentException();
			}
			if(entry.offset == -1){
				entryList.add(new WeakReference<ZipChannel.ZipEntry>(entry));
				cachedBytes += entry.data.length;
				entry.offset = -2;
			}
			return entryMap.put(entry.name,entry);
		}
	}
	@Override
	public ZipEntry remove(Object key) {
		return entryMap.remove(key);
	}
	@Override
	public void clear() {
		entryMap.clear();
	}
	@Override
	public Set<String> keySet() {
		return entryMap.keySet();//We allow removals.
	}
	@Override
	public Collection<ZipEntry> values() {
		return entryMap.values();
	}
	@Override
	public Set<Entry<String, ZipEntry>> entrySet() {
		return Collections.unmodifiableMap(entryMap).entrySet();
	}
	@Override
	public void putAll(Map<? extends String, ? extends ZipEntry> m) {
		for(Entry<? extends String, ? extends ZipEntry> entry : m.entrySet()){
			put(entry.getKey(), entry.getValue());
		}
	}
}
