import java.util.ArrayList;


public class Microprocessor {

	ArrayList<Cache> iCacheLevels;
	ArrayList<Cache> dCacheLevels;
	Memory memory;
	int pc;
	int numberOfInstructionsExcuted;
	int totalNumberOfCyclesSpentForMemory;
	
	public String readData(String address, boolean iCacheOrDCache){
		ArrayList<Cache> cacheLevels = (iCacheOrDCache)?iCacheLevels:dCacheLevels;
		int i = 0;
		String data = "";
		String offset = "";
		for(i = 0; i < cacheLevels.size() ; i++){
			totalNumberOfCyclesSpentForMemory += cacheLevels.get(i).getCacheAccessTime();
			data = cacheLevels.get(i).readCache(address);
			offset = cacheLevels.get(i).splitAddress(address).get("offset");
			String [] dataBytes = new String[data.length()/8]; 
			if(data != null){
				writeCacheRecursively(address, i-1, iCacheOrDCache);
				if (offset.length() == 1){
					return data;
				}
				else {
					int wordNumber = Integer.parseInt(offset.substring(0, offset.length()-1), 2);
					for(int k=0; k<data.length()/8; k++) {
						dataBytes [k] = data.substring(k*8, (k+1)*8);
					}
					return dataBytes[wordNumber]+dataBytes[wordNumber+1];
				}
			}
		}
		
		totalNumberOfCyclesSpentForMemory += memory.getMemoryAccessTime();
		int wordNumber = Integer.parseInt(offset.substring(0, offset.length()-1), 2);
		String [] block = memory.read(address, cacheLevels.get(i));
		writeCacheRecursively(address, i-1, iCacheOrDCache);
		
		return block[wordNumber]+block[wordNumber+1];
		
	}
	public void writeCacheRecursively(String address ,int index,boolean iCacheOrDCache){
		ArrayList<Cache> cacheLevels = (iCacheOrDCache)?iCacheLevels:dCacheLevels;
		Cache biggestLineSizeCache = cacheLevels.get(0);
		for(int i = 1;i<cacheLevels.size();i++){
			if(cacheLevels.get(i).offsetBits>biggestLineSizeCache.offsetBits)
				biggestLineSizeCache=cacheLevels.get(i);
		}
		String [] dataArray =memory.read(address, biggestLineSizeCache);
		String data="";
		for(int i = 0 ;i<dataArray.length;i++)
			data+=dataArray[i];
		for(int i = index ; i<cacheLevels.size(); i++){
			if(cacheLevels.get(i).writePolicy.equalsIgnoreCase("wt"))
				memory.write(address, data);
			cacheLevels.get(i).writeCache(address, cacheLevels.get(i).trimData(data));
		}
	}
}
