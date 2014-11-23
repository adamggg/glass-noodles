import java.io.File;
import java.util.ArrayList;

import javax.xml.crypto.Data;


public class Microprocessor {

	ArrayList<Cache> iCacheLevels;
	ArrayList<Cache> dCacheLevels;
	Memory memory;
	int pc;
	int numberOfInstructionsExcuted;
	int totalNumberOfCyclesSpentForMemory;
	
	public Microprocessor(File file) {
		assembler a = new assembler(file);
		int baseAddress = a.getBaseAddress();
		String [] memory = a.getMemoryArray();
		this.memory = new Memory(memory, baseAddress);
		
		this.pc = this.memory.getInstructionBaseAddress();
		this.numberOfInstructionsExcuted = 0;
		this.totalNumberOfCyclesSpentForMemory = 0;
		
		//cache part
	}
	
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
	
	public String to16BinaryStringAddress(int address) {
		String returnAddress = Integer.toBinaryString(address);
		
		while(returnAddress.length() < 16) {
			returnAddress = "0" + returnAddress;
		}
		
		return returnAddress;
	}
	
	public void execute() {
		int address = this.pc;
		String dataAddress = to16BinaryStringAddress(address);
		
		String data = readData(dataAddress, true);
		
		if(data.startsWith("100")) {
			//load
		}
		else if(data.startsWith("101")) {
			//store
		}
		else if(data.startsWith("110")) {
			//BEQ
		}
		else if(data.startsWith("111")) {
			//AddI
		}
		else if(data.startsWith("0000000")) {
			//Add
		}
		else if(data.startsWith("0000001")) {
			//SUB
		}
		else if(data.startsWith("0000010")) {
			//NAND
		}
		else if(data.startsWith("0000011")) {
			//MUL
		}
		else if(data.startsWith("001000")) {
			//JMP
		}
		else if(data.startsWith("0100000000")) {
			//JALR
		}
		else if(data.startsWith("0110000000000")) {
			//RET
		}
		
		
	}
	
	public static void main(String [] args) {
		Integer x = 5;
		String y = Integer.toBinaryString(x);
		while(y.length() < 16) {
			y = "0" + y;
		}
		System.out.println(y);
	}
}
