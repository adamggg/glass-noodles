import java.util.ArrayList;


public class Microprocessor {

	ArrayList<Cache> cacheLevels;
	Memory memory;
	int pc;
	int numberOfInstructionsExcuted;
	int totalNumberOfCyclesSpentForMemory;

	public void writeCacheRecursively(String address ,int index){
		Cache biggestLineSizeCache = cacheLevels.get(0);
		for(int i = 1;i<cacheLevels.size();i++){
			if(cacheLevels.get(i).offsetBits>biggestLineSizeCache.offsetBits)
				biggestLineSizeCache=cacheLevels.get(i);
		}
		String [] dataArray =memory.read(address, biggestLineSizeCache);
		String data="";
		for(int i = 0 ;i<dataArray.length;i++)
			data+=dataArray[i];
		for(int i = index ; i<cacheLevels.size(); i++)
			cacheLevels.get(i).writeCache(address, cacheLevels.get(i).trimData(data));
	}
}
