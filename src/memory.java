import java.util.HashMap;

public class memory {
	
	String [] memory;
	int InstructionsBaseAddress;
	
	public memory(String [] memory, int insBaseAddress) {
		this.memory = memory;
		this.InstructionsBaseAddress = insBaseAddress;
	}
	
	public int getInstructionBaseAddress() {
		return this.InstructionsBaseAddress;
	}
	
	public HashMap<String, String> splitAddress(String address){
		HashMap<String, String> splittedAddress = new HashMap<String, String>();

		splittedAddress.put("tag", address.substring(0, cache.tagBits));
		splittedAddress.put("index", address.substring(cache.tagBits, cache.tagBits + cache.indexBits));
		splittedAddress.put("offset" , address.substring(cache.tagBits + cache.indexBits, cache.tagBits + cache.indexBits + cache.offsetBits));
		
		return splittedAddress;		
	}
	
	public String [] read(String address) {
		String index = splitAddress(address).get("index");
		String offset = splitAddress(address).get("offset");
		
		String x = index + offset;
		
		String startRangeMask = "";
		for(int i=0; i<cache.indexBits; i++) {
			startRangeMask += 1;
		}
		for(int i=0; i<cache.offsetBits; i++) {
			startRangeMask += 0;
		}
		
		String endRangeMask = "";
		for(int i=0; i<cache.indexBits; i++) {
			endRangeMask += 0;
		}
		for(int i=0; i<cache.offsetBits; i++) {
			endRangeMask += 1;
		}
		
		int xInt = Integer.parseInt(x, 2);
		int startRangeMaskInt = Integer.parseInt(startRangeMask, 2);
		int endRangeMaskInt = Integer.parseInt(endRangeMask, 2);
		
		int startRange = xInt & startRangeMaskInt;
		int endRange = xInt | endRangeMaskInt;
		
		int size = (endRange - startRange + 1);
		String [] fetchedBlock = new String [size];
		for(int i=0; i<size; i++) {
			fetchedBlock[i] = this.memory[startRange];
			startRange++;
		}
		
		return fetchedBlock;
		
	}
	
	public boolean write(String address, String data) {
		boolean dataWritten = false;
		
		int memoryAddress = Integer.parseInt(address, 2);
		
		String [] dataBytes = new String[data.length()/8]; 
		
		for(int i=0; i<data.length()/8; i++) {
			dataBytes [i] = data.substring(i*8, (i+1)*8);
		}
		
		if (memoryAddress < this.memory.length) {
			for(int i=0; i<data.length()/8; i++) {
				this.memory[memoryAddress] = dataBytes[i];
				memoryAddress++;
			}
			dataWritten = true;
		}
		
		return dataWritten;
	}
	
	

}
