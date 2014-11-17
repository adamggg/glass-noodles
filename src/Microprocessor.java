import java.util.ArrayList;


public class Microprocessor {

	ArrayList<Cache> iCacheLevels;
	ArrayList<Cache> dCacheLevels;
	Memory memory;
	int pc;
	int numberOfInstructionsExcuted;
	int totalNumberOfCyclesSpentForMemory;
	
	
	public String readData(String address){
		int i = 0;
		String data = "";
		String offset = "";
		String memoryData = "";
		for(i = 0; i < dCacheLevels.size() ; i++){
			totalNumberOfCyclesSpentForMemory += dCacheLevels.get(i).getCacheAccessTime();
			data = dCacheLevels.get(i).readCache(address);
			offset = dCacheLevels.get(i).splitAddress(address).get("offset");
			String [] dataBytes = new String[data.length()/8]; 
			if(data != null){
				writeData(address, data, i-1);
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
		String [] block = memory.read(address, dCacheLevels.get(i));
		
		for(int h = 0; h < block.length ; h++) {
			memoryData += block[h];
		}
		
		writeData(address, memoryData, i-1);
		return block[wordNumber]+block[wordNumber+1];
		
	}
}
