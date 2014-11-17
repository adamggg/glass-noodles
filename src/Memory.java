
public class Memory {
	
	String [] memory;
	int InstructionsBaseAddress;
	int memoryAccessTime;
	
	public Memory(String [] memory, int insBaseAddress) {
		this.memory = memory;
		this.InstructionsBaseAddress = insBaseAddress;
	}
	
	public int getInstructionBaseAddress() {
		return this.InstructionsBaseAddress;
	}
	
	public int getMemoryAccessTime() {
		return this.memoryAccessTime;
	}
	
	public String [] read(String address, Cache c) {
		String index = c.splitAddress(address).get("index");
		String offset = c.splitAddress(address).get("offset");
		String tag = c.splitAddress(address).get("tag");
		
		String x = tag + index + offset;
		
		String startRangeMask = "";
		for(int i=0; i<(c.indexBits+c.tagBits); i++) {
			startRangeMask += 1;
		}
		for(int i=0; i<c.offsetBits; i++) {
			startRangeMask += 0;
		}
		
		String endRangeMask = "";
		for(int i=0; i<(c.indexBits+c.tagBits); i++) {
			endRangeMask += 0;
		}
		for(int i=0; i<c.offsetBits; i++) {
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
