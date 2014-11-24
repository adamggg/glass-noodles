import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

public class Microprocessor {

	ArrayList<Cache> iCacheLevels;
	ArrayList<Cache> dCacheLevels;
	Memory memory;
	int pc;
	int numberOfInstructionsExcuted;
	int totalNumberOfCyclesSpentForMemory;
	HashMap<Integer, String> registers;
	Assembler a;
	
	public Microprocessor(File file) throws IOException {
		this.a = new Assembler(file);
		int baseAddress = a.getBaseAddress();
		String [] memory = a.getMemoryArray();
		this.memory = new Memory(memory, baseAddress);
		
		this.pc = this.memory.getInstructionBaseAddress();
		this.numberOfInstructionsExcuted = 0;
		this.totalNumberOfCyclesSpentForMemory = 0;
		
		this.registers = new HashMap<Integer, String>();
		registers.put(0, "0000000000000000");
		for (int i = 1; i < 8; i++) {
			registers.put(i, "");
		}
		
		//cache part
		
	}
	
	public String readData(String address, boolean iCacheOrDCache, String dataToBeStored){
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
				writeCacheRecursively(address, i-1, iCacheOrDCache,dataToBeStored);

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
		String [] block = memory.read(address, cacheLevels.get(i-1));
		writeCacheRecursively(address, i-1, iCacheOrDCache,dataToBeStored);
		return block[wordNumber]+block[wordNumber+1];
		
	}
	public void writeCacheRecursively(String address ,int index,boolean iCacheOrDCache ,
			String dataToBeStored){
		ArrayList<Cache> cacheLevels = (iCacheOrDCache)?iCacheLevels:dCacheLevels;
		for(int i = index ; i >= 0; i--){
			String [] dataArray =memory.read(address, cacheLevels.get(i));
			String data="";
			boolean dirty=false;
			int wordNumber = Integer.parseInt(cacheLevels.get(i).splitAddress(address)
					.get("offset").substring(0,cacheLevels.get(i).splitAddress(address).
							get("offset").length()-1), 2);
			for(int j = 0 ;j<dataArray.length;j++)
				if(!dataToBeStored.equalsIgnoreCase("") && j==wordNumber){
					data+=dataToBeStored;
					dirty=true;
					j++;
				}
				else
					if(dataArray[j]==null)
						data+="$$$$$$$$";
					else
						data+=dataArray[j];
			if(cacheLevels.get(i).writePolicy.equalsIgnoreCase("wt")){
				memory.write(address, data);
				totalNumberOfCyclesSpentForMemory += memory.getMemoryAccessTime();
			}
			if(cacheLevels.get(i).writeCache(address, cacheLevels.get(i).trimData(data),dirty))
				totalNumberOfCyclesSpentForMemory += cacheLevels.get(i).getCacheAccessTime();
		}
	}
	
	public String to16BinaryStringValue(int value) {
		String returnValue = Integer.toBinaryString(value);
		
		while(returnValue.length() < 16) {
			returnValue = "0" + returnValue;
		}
		
		return returnValue;
	}
	
	public void execute() {
		int address = this.pc;
		String dataAddress = to16BinaryStringValue(address);		
		String data = readData(dataAddress, true, "");
		
		while (data != null) {
			
			if(data.startsWith("100")) {
				//load
				int regA = Integer.parseInt(data.substring(3, 6), 2);
				int regB = Integer.parseInt(data.substring(6, 9), 2);
				String immediateValue = data.substring(9, 16);
				
				int memoryAddress = Integer.parseInt(registers.get(regB), 2) + signedBinaryToDecimal(immediateValue);
				
//				if (a.getAddressesMapping().containsKey(memoryAddress)){
//					memoryAddress = a.getAddressesMapping().get(memoryAddress);
//				}
				
				String readData = readData(to16BinaryStringValue(memoryAddress), false, "");
				
				registers.put(regA, readData);
				
				
			}
			else if(data.startsWith("101")) {
				//store
				int regA = Integer.parseInt(data.substring(3, 6), 2);
				int regB = Integer.parseInt(data.substring(6, 9), 2);
				String immediateValue = data.substring(9, 16);
				
				int memoryAddress = Integer.parseInt(registers.get(regB), 2) + signedBinaryToDecimal(immediateValue);
				
//				if (a.getAddressesMapping().containsKey(memoryAddress)){
//					memoryAddress = a.getAddressesMapping().get(memoryAddress);
//				}
				
				readData(to16BinaryStringValue(memoryAddress), false, registers.get(regA));
				
			}
			else if(data.startsWith("110")) {
				//BEQ
				int regA = Integer.parseInt(data.substring(3, 6), 2);
				int regB = Integer.parseInt(data.substring(6, 9), 2);
				String immediateValue = data.substring(9, 16);
				
				String regAValue = registers.get(regA);
				String regBValue = registers.get(regB);
				
				if(regAValue.equalsIgnoreCase(regBValue)){
					this.pc = this.pc + 2 + signedBinaryToDecimal(immediateValue) - 2; 
				}
				
			}
			else if(data.startsWith("111")) {
				//AddI
				int regA = Integer.parseInt(data.substring(3, 6), 2);
				int regB = Integer.parseInt(data.substring(6, 9), 2);
				int immediateValue = signedBinaryToDecimal(data.substring(9, 16));
				
				int result = Integer.parseInt(registers.get(regB), 2) + immediateValue;
				
				if (regA != 0) {
					registers.put(regA, to16BinaryStringValue(result));
				}
			}
			else if(data.startsWith("0000000")) {
				//Add
				int regA = Integer.parseInt(data.substring(7, 10), 2);
				int regB = Integer.parseInt(data.substring(10, 13), 2);
				int regC = Integer.parseInt(data.substring(13, 16), 2);
				
				int result = Integer.parseInt(registers.get(regB),2) + Integer.parseInt(registers.get(regC),2);
				
				if(regA != 0) {
					registers.put(regA, to16BinaryStringValue(result));
				}
			}
			else if(data.startsWith("0000001")) {
				//SUB
				int regA = Integer.parseInt(data.substring(7, 10), 2);
				int regB = Integer.parseInt(data.substring(10, 13), 2);
				int regC = Integer.parseInt(data.substring(13, 16), 2);
				
				int result = Integer.parseInt(registers.get(regB),2) - Integer.parseInt(registers.get(regC),2);
				
				if(regA != 0) {
					registers.put(regA, to16BinaryStringValue(result));
				}
			}
			else if(data.startsWith("0000010")) {
				//NAND
				int regA = Integer.parseInt(data.substring(7, 10), 2);
				int regB = Integer.parseInt(data.substring(10, 13), 2);
				int regC = Integer.parseInt(data.substring(13, 16), 2);
				
				int result = ~(Integer.parseInt(registers.get(regB),2) & Integer.parseInt(registers.get(regC),2));
				
				if (regA != 0) {
					registers.put(regA, to16BinaryStringValue(result));
				}
			}
			else if(data.startsWith("0000011")) {
				//MUL
				int regA = Integer.parseInt(data.substring(7, 10), 2 );
				int regB = Integer.parseInt(data.substring(10, 13), 2);
				int regC = Integer.parseInt(data.substring(13, 16), 2);
				
				int result = Integer.parseInt(registers.get(regB),2) * Integer.parseInt(registers.get(regC),2);
				
				if(regA != 0) {
					registers.put(regA, to16BinaryStringValue(result));
				}
			}
			else if(data.startsWith("001000")) {
				//JMP
				int regA = Integer.parseInt(data.substring(6, 9), 2);
				String immediateValue = data.substring(9, 16);
				
				String regAValue = registers.get(regA);
				
				this.pc = this.pc + 2 + Integer.parseInt(registers.get(regA), 2) + signedBinaryToDecimal(immediateValue) - 2; 
				
			}
			else if(data.startsWith("0100000000")) {
				//JALR
				int regA = Integer.parseInt(data.substring(10, 13), 2);
				int regB = Integer.parseInt(data.substring(13, 16), 2);
				
				this.pc = Integer.parseInt(registers.get(regB), 2);
				
				if (regA != 0) {
					registers.put(regA, to16BinaryStringValue(this.pc));
				}
			}
			else if(data.startsWith("0110000000000")) {
				//RET
				int regA = Integer.parseInt(data.substring(13, 16), 2);
				
				this.pc = Integer.parseInt(registers.get(regA), 2);
			}
			
			this.pc += 2;
			numberOfInstructionsExcuted++;
			address = this.pc;
			dataAddress = to16BinaryStringValue(address);		
			data = readData(dataAddress, true, "");
		}
	}
	
	public static int signedBinaryToDecimal(String signed){
		int result = 0;		
		if (signed.startsWith("1")){
			signed = signed.replace('0', '2').replace('1', '0').replace('2', '1');
			result = 0 - (Integer.parseInt(signed, 2) + 1);
		}
		else {
			result = Integer.parseInt(signed, 2);
		}
		
		return result;
	}
	public int[] getGlobalAmat(){
		int globalAmatICache=iCacheLevels.get(0).getCacheAccessTime();
		int globalAmatDCache=dCacheLevels.get(0).getCacheAccessTime();
		for(int i =1;i<iCacheLevels.size();i++){
			int hitRatio = iCacheLevels.get(i-1).getMissPenalty();
			int missPen = iCacheLevels.get(i).getCacheAccessTime();
			if(i==iCacheLevels.size()-1)
				missPen = memory.getMemoryAccessTime();
			globalAmatICache+=hitRatio*missPen;
		}
		for(int i =1;i<dCacheLevels.size();i++){
			int hitRatio = dCacheLevels.get(i-1).getMissPenalty();
			int missPen = dCacheLevels.get(i).getCacheAccessTime();
			if(i==dCacheLevels.size()-1)
				missPen = memory.getMemoryAccessTime();
			globalAmatDCache+=hitRatio*missPen;
		}
		int [] result = {globalAmatDCache,globalAmatICache};
		return result;
	}
}
