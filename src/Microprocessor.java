import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;
import java.util.Set;

import com.sun.xml.internal.ws.api.pipe.NextAction;

public class Microprocessor {

	ArrayList<Cache> iCacheLevels;
	ArrayList<Cache> dCacheLevels;
	Memory memory;
	int pc;
	int numberOfInstructionsExcuted;
	int totalNumberOfCyclesSpentForMemory;
	HashMap<Integer, String> registers;
	Assembler a;
	
	//Start Of New Code
	HashMap<Integer, String []> reorderBuffer;
	int head;
	int tail;
	HashMap<String, String []> reservationStations;
	HashMap<Integer, Integer> registerStatus;
	String[] instBuffer ;
	//HashMap<Integer ,Instruction > instArray; //array containing instructions with the needed specifications in the given program 
	HashMap<Integer ,String> instArrayBinary;//array containing instructions in program order
	HashMap<Integer ,Integer> branchPrediction;//keep track of branch instructions misprediction
	int instToFetchAddress;
	int instNumber = 1; //needed to keep track of order of instructions in the given program 
	int loadRs = 0;
	int storeRs = 0;
	int integerAddSubRs = 0;
	int doublePrecisionAddSubRs = 0;
	int multDivRs = 0;
	int numberOfRobEntries = 0;
	int numberOfWays = 0;
	int instBufferSize = 0;
	int loadLatency = 0;
	int storeLatency = 0;
	int integerAddSubLatency = 0;
	int doublePrecisionAddSubLatency = 0;
	HashMap<Integer, String []> writeBuffer;
	HashMap<Integer, int[]> clockCycles = new HashMap<Integer, int[]>();
	//End Of New Code
	
	public Microprocessor(File confFile, File assemblerFile) throws Exception {
		this.dCacheLevels = new ArrayList<Cache>();
		this.iCacheLevels = new ArrayList<Cache>();
		this.a = new Assembler(assemblerFile);
		int baseAddress = a.getBaseAddress();
		instToFetchAddress = a.getBaseAddress();
		String [] memory = a.getMemoryArray();
		BufferedReader configFile = new BufferedReader(new FileReader(confFile));
		configFile.readLine();
		
		this.numberOfWays = Integer.parseInt(configFile.readLine());
		this.instBufferSize = Integer.parseInt(configFile.readLine());
		this.numberOfRobEntries = Integer.parseInt(configFile.readLine());
		this.loadRs = Integer.parseInt(configFile.readLine());
		this.storeRs = Integer.parseInt(configFile.readLine());
		this.integerAddSubRs = Integer.parseInt(configFile.readLine());
		this.doublePrecisionAddSubRs = Integer.parseInt(configFile.readLine());
		this.multDivRs = Integer.parseInt(configFile.readLine());
		this.loadLatency = Integer.parseInt(configFile.readLine());
		this.storeLatency = Integer.parseInt(configFile.readLine());
		this.integerAddSubLatency = Integer.parseInt(configFile.readLine());
		this.doublePrecisionAddSubLatency = Integer.parseInt(configFile.readLine());
		
		configFile.readLine();
		int memoryAccessTime = Integer.parseInt(configFile.readLine());
		this.memory = new Memory(memory, baseAddress, memoryAccessTime);
		
		this.pc = this.memory.getInstructionBaseAddress();
		this.numberOfInstructionsExcuted = 0;
		this.totalNumberOfCyclesSpentForMemory = 0;
		
		this.registers = new HashMap<Integer, String>();

		for (int i = 0; i < 8; i++) {
			registers.put(i, "0000000000000000");
		}
		
		//cache part
		
		int level = 0;
		int s, l, m, cacheAccessTime;
		String writePolicy;
		while(configFile.ready()) {
			//D-Cache
			configFile.readLine();
			s = Integer.parseInt(configFile.readLine());
			l = Integer.parseInt(configFile.readLine());
			m = Integer.parseInt(configFile.readLine());
			writePolicy = configFile.readLine();
			cacheAccessTime = Integer.parseInt(configFile.readLine());
			dCacheLevels.add(level, new Cache(s, l, m, writePolicy, cacheAccessTime));
			
			//I-Cache
			configFile.readLine();
			s = Integer.parseInt(configFile.readLine());
			l = Integer.parseInt(configFile.readLine());
			m = Integer.parseInt(configFile.readLine());
			writePolicy = configFile.readLine();
			cacheAccessTime = Integer.parseInt(configFile.readLine());
			iCacheLevels.add(level, new Cache(s, l, m, writePolicy, cacheAccessTime));
			
			configFile.readLine();
			level++;
		}
		configFile.close();
		
		//Start Of New Code
		//ROB
		this.head = 1;
		this.tail = 1;
		this.reorderBuffer = new HashMap<Integer, String []>();
		//Initialization of ROB Array
		
		String [] robInitialArray = new String[4];
		for(int i=0; i<4; i++) {
			robInitialArray[i] = "$$$$$$$$$$$$$$$$";
		}
		
		for(int i = 1; i <= numberOfRobEntries; i++){
			this.reorderBuffer.put(i, robInitialArray);
		}
		
		//Reservation Stations
		this.reservationStations = new HashMap<String, String []>();
		//Initialization of RS Array
		String [] rsInitialArray = new String[9];
		for(int i=0; i<9; i++) {
			rsInitialArray[i] = "$$$$$$$$$$$$$$$$";
		}
		
		
		String rsName = "";
		for(int j = 1; j<=loadRs; j++) {
			rsName = "Load" + j;
			reservationStations.put(rsName, rsInitialArray);
		}
		for(int j = 1; j<=storeRs; j++) {
			rsName = "Store" + j;
			reservationStations.put(rsName, rsInitialArray);
		}
		for(int j = 1; j<=integerAddSubRs; j++) {
			rsName = "Add" + j;
			reservationStations.put(rsName, rsInitialArray);
		}
		for(int j = 1; j<=doublePrecisionAddSubRs; j++) {
			rsName = "Addd" + j;
			reservationStations.put(rsName, rsInitialArray);
		}
		for(int j = 1; j<=multDivRs; j++) {
			rsName = "Multd" + j;
			reservationStations.put(rsName, rsInitialArray);
		}
		
		//Register Status
		this.registerStatus = new HashMap<Integer, Integer>();
		for (int i = 0; i < 8; i++) {
			registerStatus.put(i, 0);
		}
		
		//WriteBuffer
		this.writeBuffer = new HashMap<Integer, String []>();
		
		//InstBuffer
		this.instBuffer = new String[this.instBufferSize];
		
		//End Of New Code
		
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
			//String [] dataBytes = new String[data.length()/8]; 
			if(data != null){
				String [] dataBytes = new String[data.length()/8];
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
		int wordNumber = (offset.length()==1)?0:Integer.parseInt(offset.substring(0, offset.length()-1), 2);
		String [] block = memory.read(address, cacheLevels.get(i-1));
		writeCacheRecursively(address, i-1, iCacheOrDCache,dataToBeStored);
		return block[wordNumber]+block[wordNumber+1];
		
	}
	public void writeCacheRecursively(String address ,int index,boolean iCacheOrDCache ,
			String dataToBeStored){
		ArrayList<Cache> cacheLevels = (iCacheOrDCache)?iCacheLevels:dCacheLevels;
		if(!dataToBeStored.equalsIgnoreCase("")&& index<cacheLevels.size()-1){
			index++;
		}
		for(int i = index ;i >= 0; i--){
			String [] dataArray =memory.read(address, cacheLevels.get(i));
			String data="";
			boolean dirty=false;
			
			int wordNumber =(cacheLevels.get(i).splitAddress(address)
					.get("offset").length()==1)?0:Integer.parseInt(cacheLevels.get(i).splitAddress(address)
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
	public double[] getGlobalAmat(){
		double globalAmatICache=iCacheLevels.get(0).getCacheAccessTime();
		double globalAmatDCache=dCacheLevels.get(0).getCacheAccessTime();
		for(int i =1;i<iCacheLevels.size();i++){
			double hitRatio = iCacheLevels.get(i-1).getMissPenalty();
			double missPen = iCacheLevels.get(i).getCacheAccessTime();
			if(i==iCacheLevels.size()-1)
				missPen = memory.getMemoryAccessTime();
			globalAmatICache+=hitRatio*missPen;
		}
		for(int i =1;i<dCacheLevels.size();i++){
			double hitRatio = dCacheLevels.get(i-1).getMissPenalty();
			double missPen = dCacheLevels.get(i).getCacheAccessTime();
			if(i==dCacheLevels.size()-1)
				missPen = memory.getMemoryAccessTime();
			globalAmatDCache+=hitRatio*missPen;
		}
		double [] result = {globalAmatDCache,globalAmatICache};
		return result;
		}
	
	
	public void commit() {
		
		/* I'm assuming here that "destination" & "value" in the ROB entry are BINARY STRINGS !!
		 * where the "destination" is either the binary representation for the register number, 
		 * or it is the binary memory address in case of store instruction.
		 */
		
		for (int i = 0; i < numberOfRobEntries; i++) {
			String[] currentRobEntry = new String[6];
			currentRobEntry = reorderBuffer.get(i);
			if(!currentRobEntry[5].equalsIgnoreCase("$$$$$$$$$$$$$$$$")) {
				int updatedClockCycle = Integer.parseInt(currentRobEntry[5], 2) + 1;
				currentRobEntry[5] = to16BinaryStringValue(updatedClockCycle);
				reorderBuffer.put(i, currentRobEntry);
			}
		}
		
		String[] headRobEntry = reorderBuffer.get(head);
		
		if (headRobEntry[3].equalsIgnoreCase("yes") || headRobEntry[3].equalsIgnoreCase("y")) {
			int instructionNumber = Integer.parseInt(headRobEntry[4], 2); //Walid mafrood ye7ot el instruction number in binary
			int clockCycle = Integer.parseInt(headRobEntry[5], 2);
			if ((headRobEntry[0].equalsIgnoreCase("store")) || headRobEntry[0].equalsIgnoreCase("st")) {
				int memoryAddress = Integer.parseInt(headRobEntry[1], 2);
				readData(to16BinaryStringValue(memoryAddress), false, headRobEntry[2]);	
				updateClockCycle(instructionNumber, clockCycle, 5);
			}
			else {
				registers.put(Integer.parseInt(headRobEntry[1], 2), headRobEntry[2]);
				updateClockCycle(instructionNumber, clockCycle, 5);
			}
			
			String [] robInitialArray = new String[6];
			for(int i=0; i<6; i++) {
				robInitialArray[i] = "$$$$$$$$$$$$$$$$";
			}	
			reorderBuffer.put(head, robInitialArray);
			
			if (registerStatus.get(Integer.parseInt(headRobEntry[1], 2)) == head ) {
				registerStatus.put(Integer.parseInt(headRobEntry[1], 2) , 0);
			}
			
			if (head == numberOfRobEntries) {
				head = 1;
			} 
			else {
				head++;
			}
		}
	}
	
	public void updateClockCycle (int instructionNumber, int clockCycle, int type) {
		int[] instructionClockCycles = new int [5];
		instructionClockCycles = clockCycles.get(instructionNumber);
		instructionClockCycles[type] = clockCycle;
		clockCycles.put(instructionNumber, instructionClockCycles);
	}
	
	public void writeToAwaitingUnits(String result, String robEntryNumber) {
		String rsName = "";
		String [] rsEntry;
		
		for(int j = 1; j<=loadRs; j++) {
			rsName = rsName + "Load" + j;
			rsEntry = reservationStations.get(rsName);
			if(rsEntry[4].equalsIgnoreCase(robEntryNumber)) {
				rsEntry[2] = result;
				rsEntry[4] = "0000000000000000";
				reservationStations.put(rsName, rsEntry);
			}
			if (rsEntry[5].equalsIgnoreCase(robEntryNumber)) {
				rsEntry[3] = result;
				rsEntry[5] = "0000000000000000";
				reservationStations.put(rsName, rsEntry);
			}
		}
		
		for(int j = 1; j<=storeRs; j++) {
			rsName = rsName + "Store" + j;
			rsEntry = reservationStations.get(rsName);
			if(rsEntry[4].equalsIgnoreCase(robEntryNumber)) {
				rsEntry[2] = result;
				rsEntry[4] = "0000000000000000";
				reservationStations.put(rsName, rsEntry);
			}
			if (rsEntry[5].equalsIgnoreCase(robEntryNumber)) {
				rsEntry[3] = result;
				rsEntry[5] = "0000000000000000";
				reservationStations.put(rsName, rsEntry);
			}
		}
		
		for(int j = 1; j<=integerAddSubRs; j++) {
			rsName = rsName + "Add" + j;
			rsEntry = reservationStations.get(rsName);
			if(rsEntry[4].equalsIgnoreCase(robEntryNumber)) {
				rsEntry[2] = result;
				rsEntry[4] = "0000000000000000";
				reservationStations.put(rsName, rsEntry);
			}
			if (rsEntry[5].equalsIgnoreCase(robEntryNumber)) {
				rsEntry[3] = result;
				rsEntry[5] = "0000000000000000";
				reservationStations.put(rsName, rsEntry);
			}
		}
		
		for(int j = 1; j<=doublePrecisionAddSubRs; j++) {
			rsName = rsName + "Addd" + j;
			rsEntry = reservationStations.get(rsName);
			if(rsEntry[4].equalsIgnoreCase(robEntryNumber)) {
				rsEntry[2] = result;
				rsEntry[4] = "0000000000000000";
				reservationStations.put(rsName, rsEntry);
			}
			if (rsEntry[5].equalsIgnoreCase(robEntryNumber)) {
				rsEntry[3] = result;
				rsEntry[5] = "0000000000000000";
				reservationStations.put(rsName, rsEntry);
			}
		}
		
		for(int j = 1; j<=multDivRs; j++) {
			rsName = rsName + "Multd" + j;
			rsEntry = reservationStations.get(rsName);
			if(rsEntry[4].equalsIgnoreCase(robEntryNumber)) {
				rsEntry[2] = result;
				rsEntry[4] = "0000000000000000";
				reservationStations.put(rsName, rsEntry);
			}
			if (rsEntry[5].equalsIgnoreCase(robEntryNumber)) {
				rsEntry[3] = result;
				rsEntry[5] = "0000000000000000";
				reservationStations.put(rsName, rsEntry);
			}
		}
	}
	
	public void write(String executionResult, String rsName) {
		String [] rsEntry = reservationStations.get(rsName);
		String robEntryNumber = rsEntry[6];
		String [] robEntry = reorderBuffer.get(Integer.parseInt(robEntryNumber, 2));
		
		//Part el store dah msh waska feh !!!
		if(rsEntry[1].equalsIgnoreCase("store") || rsEntry[1].equalsIgnoreCase("st")) {
			if(rsEntry[5].equalsIgnoreCase("0000000000000000")) {
				//In the issue stage Qj lazem yb2a feh zero bs 16 bits
				robEntry[2] = rsEntry[3];
				robEntry[3] = "yes";
				reorderBuffer.put(Integer.parseInt(robEntryNumber, 2), robEntry);
			}
		}
		else {
			writeToAwaitingUnits(executionResult, robEntryNumber);
		
			robEntry[2] = executionResult;
			robEntry[3] = "yes";
			reorderBuffer.put(Integer.parseInt(robEntryNumber, 2), robEntry);
		}
		
		for(int i=0; i<rsEntry.length; i++) {
			rsEntry[i] = "$$$$$$$$$$$$$$$$";
		}
		
		reservationStations.put(rsName, rsEntry);
		
	}
	
	public void fetch() {
		instBuffer = new String[100]; //size supposed to be read from configuration file
		
		int j = 0;
		while(instToFetchAddress<a.getEndAddress() && j<instBuffer.length){
			//each two cells in memory is an instruction 
			instBuffer[j] = a.getMemoryArray()[instToFetchAddress]+a.getMemoryArray()[instToFetchAddress+1];
			instArrayBinary.put(instNumber++, instBuffer[j]);
			instToFetchAddress += 2;
			j++;
		}
		
	}
	
	public void execute() {
		
		String instToExecute;
		String[] inner;
		
		//Load instructions
		for(int a = 1 ; a<=loadRs ; a++ ){
			String x = "Load"+a;
			inner = reservationStations.get(x);
			if(inner[4].equals("$$$$$$$$$$$$$$$$")){
				instToExecute = instArrayBinary.get(Integer.parseInt(inner[8]));
				int regB = Integer.parseInt(instToExecute.substring(6, 9), 2);
				String immediateValue = instToExecute.substring(9, 16);
				int memoryAddress = Integer.parseInt(registers.get(regB), 2) + signedBinaryToDecimal(immediateValue);
				reservationStations.get(x)[7] = memoryAddress+"";
				//result is the data to be read
				String result = readData(to16BinaryStringValue(memoryAddress), false, "");
			}
		}
		
		//Store instructions
		for(int a = 1 ; a<=storeRs ; a++){
			String x = "Store"+a;
			inner = reservationStations.get(x);
			if(inner[4].equals("$$$$$$$$$$$$$$$$")){
				instToExecute = instArrayBinary.get(Integer.parseInt(inner[8]));
				int regB = Integer.parseInt(instToExecute.substring(6, 9), 2);
				String immediateValue = instToExecute.substring(9, 16);
				int memoryAddress = Integer.parseInt(registers.get(regB), 2) + signedBinaryToDecimal(immediateValue);
				reservationStations.get(x)[7] = memoryAddress+"";
				//result is the memory address to write to 
				String result = memoryAddress+""; 
			}
			
		}

		//ADD single precision instruction
		for(int a = 1 ;a<integerAddSubRs ; a++){
			String x = "Add"+a;
			inner = reservationStations.get(x);
			instToExecute = instArrayBinary.get(Integer.parseInt(inner[8]));
			
			//JALR (to be continued..)
			if(instToExecute.startsWith("0100000000")) {
				if(inner[4].equals("$$$$$$$$$$$$$$$$")){
					int regB = Integer.parseInt(instToExecute.substring(13, 16), 2);
					
				}	
			}
			//RET (to be continued..)
			if(instToExecute.startsWith("0110000000000")) {
				if(inner[4].equals("$$$$$$$$$$$$$$$$")){
					int regA = Integer.parseInt(instToExecute.substring(13, 16), 2);
				}
			}
			//JMP (to be continued..)
			if(instToExecute.startsWith("001000")) {
				if(inner[4].equals("$$$$$$$$$$$$$$$$")){
					int regA = Integer.parseInt(instToExecute.substring(6, 9), 2);
					String immediateValue = instToExecute.substring(9, 16);
					String regAValue = registers.get(regA);
				}	
			}	
			//ADDI
			if(instToExecute.startsWith("111")){
				if(inner[4].equals("$$$$$$$$$$$$$$$$") && inner[5].equals("$$$$$$$$$$$$$$$$")){
					int regA = Integer.parseInt(instToExecute.substring(3, 6), 2);
					int regB = Integer.parseInt(instToExecute.substring(6, 9), 2);
					int immediateValue = signedBinaryToDecimal(instToExecute.substring(9, 16));
					
					int r = Integer.parseInt(registers.get(regB), 2) + immediateValue;
					//result is the value to be stored in the destination register
					String result = to16BinaryStringValue(r);
				}	
			}
					
			
		}

		//ADD double precision instruction
		for(int a = 1 ; a<doublePrecisionAddSubRs ; a++){
			String x = "Addd"+a;
			inner = reservationStations.get(x);
			instToExecute = instArrayBinary.get(Integer.parseInt(inner[8]));
			
			//BEQ instruction (to be ccontinued..)
			if(instToExecute.startsWith("110")){
				if(inner[4].equals("$$$$$$$$$$$$$$$$") && inner[5].equals("$$$$$$$$$$$$$$$$")){
					int regA = Integer.parseInt(instToExecute.substring(3, 6), 2);
					int regB = Integer.parseInt(instToExecute.substring(6, 9), 2);
					String immediateValue = instToExecute.substring(9, 16);
					reservationStations.get("x")[7] = immediateValue;
					String regAValue = registers.get(regA);
					String regBValue = registers.get(regB);
					
					if(regAValue.equalsIgnoreCase(regBValue)){
						branchPrediction.put(Integer.parseInt(inner[8]), 0);
					}
					else{
						// 1 means that a branch misprediction occured
						branchPrediction.put(Integer.parseInt(inner[8]), 1);
					}
					//result is the address to branch to
					String result = immediateValue+"";
					
				}
			
			}
			
			//ADD instruction
			if(instToExecute.startsWith("0000000")) {
				if(inner[4].equals("$$$$$$$$$$$$$$$$") && inner[5].equals("$$$$$$$$$$$$$$$$")){
					int regA = Integer.parseInt(instToExecute.substring(7, 10), 2);
					int regB = Integer.parseInt(instToExecute.substring(10, 13), 2);
					int regC = Integer.parseInt(instToExecute.substring(13, 16), 2);
					
					int r = Integer.parseInt(registers.get(regB),2) + Integer.parseInt(registers.get(regC),2);
					String result = to16BinaryStringValue(r);
				}
			}
			
			//SUB instruction
			if(instToExecute.startsWith("0000001")) {
				if(inner[4].equals("$$$$$$$$$$$$$$$$") && inner[5].equals("$$$$$$$$$$$$$$$$")){
					int regA = Integer.parseInt(instToExecute.substring(7, 10), 2);
					int regB = Integer.parseInt(instToExecute.substring(10, 13), 2);
					int regC = Integer.parseInt(instToExecute.substring(13, 16), 2);
					int r = Integer.parseInt(registers.get(regB),2) - Integer.parseInt(registers.get(regC),2);
					String result = to16BinaryStringValue(r);
				}
			}
			
			//NAND instruction
			if(instToExecute.startsWith("0000010")) {
				if(inner[4].equals("$$$$$$$$$$$$$$$$") && inner[5].equals("$$$$$$$$$$$$$$$$")){
					int regA = Integer.parseInt(instToExecute.substring(7, 10), 2);
					int regB = Integer.parseInt(instToExecute.substring(10, 13), 2);
					int regC = Integer.parseInt(instToExecute.substring(13, 16), 2);
					int r = (Integer.parseInt(registers.get(regB),2) & Integer.parseInt(registers.get(regC),2));
					String result = to16BinaryStringValue(r);
				}
			}

		}

		//Mul and Div instructions
		for(int a = 1 ; a<multDivRs ; a++){
			String x = "Multd"+a;
			inner = reservationStations.get(x);
			if(inner[4].equals("$$$$$$$$$$$$$$$$")){
				instToExecute = instArrayBinary.get(Integer.parseInt(inner[8]));
				int regA = Integer.parseInt(instToExecute.substring(7, 10), 2 );
				int regB = Integer.parseInt(instToExecute.substring(10, 13), 2);
				int regC = Integer.parseInt(instToExecute.substring(13, 16), 2);
				int r = Integer.parseInt(registers.get(regB),2) * Integer.parseInt(registers.get(regC),2);
				String result = to16BinaryStringValue(r);
			}
			
		}
	}	
		
///////////////////////////////old execute///////////////////////////////////////////////////////////
//		public void execute() {
//		int address = this.pc;
//		String dataAddress = to16BinaryStringValue(address);		
//		String data = readData(dataAddress, true, "");
//		
//		while(!data.equalsIgnoreCase("nullnull")){
//			if(data.startsWith("100")) {
//				//load
//				int regA = Integer.parseInt(data.substring(3, 6), 2);
//				int regB = Integer.parseInt(data.substring(6, 9), 2);
//				String immediateValue = data.substring(9, 16);
//				
//				int memoryAddress = Integer.parseInt(registers.get(regB), 2) + signedBinaryToDecimal(immediateValue);
//				
////				if (a.getAddressesMapping().containsKey(memoryAddress)){
////					memoryAddress = a.getAddressesMapping().get(memoryAddress);
////				}
//				
//				String readData = readData(to16BinaryStringValue(memoryAddress), false, "");
//				
//				registers.put(regA, readData);
//				
//				
//			}
//			else if(data.startsWith("101")) {
//				//store
//				int regA = Integer.parseInt(data.substring(3, 6), 2);
//				int regB = Integer.parseInt(data.substring(6, 9), 2);
//				String immediateValue = data.substring(9, 16);
//				
//				int memoryAddress = Integer.parseInt(registers.get(regB), 2) + signedBinaryToDecimal(immediateValue);
//				
////				if (a.getAddressesMapping().containsKey(memoryAddress)){
////					memoryAddress = a.getAddressesMapping().get(memoryAddress);
////				}
//				
//				readData(to16BinaryStringValue(memoryAddress), false, registers.get(regA));
//				
//			}
//			else if(data.startsWith("110")) {
//				//BEQ
//				int regA = Integer.parseInt(data.substring(3, 6), 2);
//				int regB = Integer.parseInt(data.substring(6, 9), 2);
//				String immediateValue = data.substring(9, 16);
//				
//				String regAValue = registers.get(regA);
//				String regBValue = registers.get(regB);
//				
//				if(regAValue.equalsIgnoreCase(regBValue)){
//					this.pc = this.pc + 2 + signedBinaryToDecimal(immediateValue) - 2; 
//				}
//				
//			}
//			else if(data.startsWith("111")) {
//				//AddI
//				int regA = Integer.parseInt(data.substring(3, 6), 2);
//				int regB = Integer.parseInt(data.substring(6, 9), 2);
//				int immediateValue = signedBinaryToDecimal(data.substring(9, 16));
//				
//				int result = Integer.parseInt(registers.get(regB), 2) + immediateValue;
//				
//				if (regA != 0) {
//					registers.put(regA, to16BinaryStringValue(result));
//				}
//			}
//			else if(data.startsWith("0000000")) {
//				//Add
//				int regA = Integer.parseInt(data.substring(7, 10), 2);
//				int regB = Integer.parseInt(data.substring(10, 13), 2);
//				int regC = Integer.parseInt(data.substring(13, 16), 2);
//				
//				int result = Integer.parseInt(registers.get(regB),2) + Integer.parseInt(registers.get(regC),2);
//				
//				if(regA != 0) {
//					registers.put(regA, to16BinaryStringValue(result));
//				}
//				
//			}
//			else if(data.startsWith("0000001")) {
//				//SUB
//				int regA = Integer.parseInt(data.substring(7, 10), 2);
//				int regB = Integer.parseInt(data.substring(10, 13), 2);
//				int regC = Integer.parseInt(data.substring(13, 16), 2);
//				
//				int result = Integer.parseInt(registers.get(regB),2) - Integer.parseInt(registers.get(regC),2);
//				
//				if(regA != 0) {
//					registers.put(regA, to16BinaryStringValue(result));
//				}
//			}
//			else if(data.startsWith("0000010")) {
//				//NAND
//				int regA = Integer.parseInt(data.substring(7, 10), 2);
//				int regB = Integer.parseInt(data.substring(10, 13), 2);
//				int regC = Integer.parseInt(data.substring(13, 16), 2);
//				
//				int result = (Integer.parseInt(registers.get(regB),2) & Integer.parseInt(registers.get(regC),2));
//				
//				if (regA != 0) {
//					registers.put(regA, to16BinaryStringValue(result));
//				}
//			}
//			else if(data.startsWith("0000011")) {
//				//MUL
//				int regA = Integer.parseInt(data.substring(7, 10), 2 );
//				int regB = Integer.parseInt(data.substring(10, 13), 2);
//				int regC = Integer.parseInt(data.substring(13, 16), 2);
//				
//				int result = Integer.parseInt(registers.get(regB),2) * Integer.parseInt(registers.get(regC),2);
//				
//				if(regA != 0) {
//					registers.put(regA, to16BinaryStringValue(result));
//				}
//			}
//			else if(data.startsWith("001000")) {
//				//JMP
//				int regA = Integer.parseInt(data.substring(6, 9), 2);
//				String immediateValue = data.substring(9, 16);
//				
//				String regAValue = registers.get(regA);
//				
//				this.pc = this.pc + 2 + Integer.parseInt(registers.get(regA), 2) + signedBinaryToDecimal(immediateValue) - 2; 
//				
//			}
//			else if(data.startsWith("0100000000")) {
//				//JALR
//				int regA = Integer.parseInt(data.substring(10, 13), 2);
//				int regB = Integer.parseInt(data.substring(13, 16), 2);
//				
//				if (regA != 0) {
//					registers.put(regA, to16BinaryStringValue(this.pc+2));
//				}
//				this.pc = Integer.parseInt(registers.get(regB), 2)-2;
//				
//				
//				
//			}
//			else if(data.startsWith("0110000000000")) {
//				//RET
//				int regA = Integer.parseInt(data.substring(13, 16), 2);
//				
//				this.pc = Integer.parseInt(registers.get(regA), 2)-2;
//			}
//			
//			this.pc += 2;
//			numberOfInstructionsExcuted++;
//			address = this.pc;
//			dataAddress = to16BinaryStringValue(address);		
//			data = readData(dataAddress, true, "");
//			for(int i = 0;i<dCacheLevels.size();i++){
//				System.out.println("the hit ratio for d cache of level "+i+" is "+dCacheLevels.get(i).getHitRatio());
//			}
//			for(int i = 0;i<iCacheLevels.size();i++){
//				System.out.println("the hit ratio for i cache of level "+i+" is "+iCacheLevels.get(i).getHitRatio());
//			}
//			System.out.println("the global AMAT for d cache is : "+getGlobalAmat()[0]);
//			System.out.println("the global AMAT for i cache is : "+getGlobalAmat()[1]);
//		}
//		}
/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	
	public static void main(String[] args) throws Exception {
		Scanner sc = new Scanner(System.in);
		System.out.println("Please enter the directory for your file , your file should look like this :-" +"\n"+"\n"
				+"CODE"+"\n"+"Base Address  (write the base address for your program and remove the 0x ; just write the value)"
				+"\n"+"first program instruction"+"\n"+"second program instruction"+"\n"+"third program instruction"+"\n"
				+"....."+"\n"+".....  (the instruction should look like this : instName operand1,operand2,... )"+"\n"
				+"DATA"+"\n"+"value1,address1"+"\n"+"value2,address2"+"\n"+"value3,address3"+"\n"+"....."+"\n"+".....  "
				+"(the data address should also be writtin in hexamdecimal with no 0x or H ; just the value)"+"\n"+"\n"+
				"Some guidelines to follow :"+"\n"+"1)No additional/missing spaces if not specified in the above format are allowed ."
				+"\n"+"2)No additional/missing semicollons are allowed ."+"\n"+"3)Semicollon should be inserted in between two operands ."+
				"\n"+"4)No empty lines are allowed within the text or after it which means that the text file should start with the word "+
				"\"CODE\""+"\n"+"  and ends with the last data value ."+"\n"+"5)The word \"CODE\" comes before your program code at the "
				+"very first line of the file" +"\n"+"  and the word \"DATA\" comes before the data separting between the code and the data lines."
				+"\n"+"**ANY TEXT FILE VIOLATING ONE OF THE ABOVE CONDITIONS WILL NOT BE ACCEPTED :)");
		System.out.println("enter the program file path: ");
		String prog = sc.nextLine();
		System.out.println("enter the config file path: ");
		String config = sc.nextLine();
		Microprocessor m = new Microprocessor(new File(config), new File(prog));
		sc.close();
		m.execute();
	}
}
