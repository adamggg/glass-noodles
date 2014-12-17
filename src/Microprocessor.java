import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;
import java.util.Set;



public class Microprocessor {

	ArrayList<Cache> iCacheLevels;
	ArrayList<Cache> dCacheLevels;
	Memory memory;
	int pc;
	int numberOfInstructionsExcuted;
	int totalNumberOfCyclesSpentForMemory;
	HashMap<Integer, String> registers;
	Assembler a;

	// Start Of New Code
	HashMap<Integer, String[]> reorderBuffer;
	int head;
	int tail;
	HashMap<String, String[]> reservationStations;
	HashMap<Integer, Integer> registerStatus;

	int[] instBuffer ;

	// End Of New Code

	//HashMap<Integer ,Instruction > instArray; //array containing instructions with the needed specifications in the given program 
	HashMap<Integer ,String> instArrayBinary;//array containing instructions in program order
	HashMap<Integer ,String> branchPrediction;//keep track of branch instructions misprediction
	int instToFetchAddress;
	boolean unconditionalJMP = false;
	int instNumber = 0; //needed to keep track of order of instructions in the given program 
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
	int multDivLatency = 0;

	HashMap<Integer, String []> writeBuffer;
	HashMap<Integer, int[]> clockCycles = new HashMap<Integer, int[]>();
	int writeWaitingCycles;
	int programCycles = 0;
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
		this.instBuffer = new int[instBufferSize];
		this.instArrayBinary  = new HashMap<Integer, String>();
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
		this.multDivLatency = Integer.parseInt(configFile.readLine());

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

		// cache part

		int level = 0;
		int s, l, m, cacheAccessTime;
		String writePolicy;
		while (configFile.ready()) {
			// D-Cache
			configFile.readLine();
			s = Integer.parseInt(configFile.readLine());
			l = Integer.parseInt(configFile.readLine());
			m = Integer.parseInt(configFile.readLine());
			writePolicy = configFile.readLine();
			cacheAccessTime = Integer.parseInt(configFile.readLine());
			dCacheLevels.add(level, new Cache(s, l, m, writePolicy,
					cacheAccessTime));

			// I-Cache
			configFile.readLine();
			s = Integer.parseInt(configFile.readLine());
			l = Integer.parseInt(configFile.readLine());
			m = Integer.parseInt(configFile.readLine());
			writePolicy = configFile.readLine();
			cacheAccessTime = Integer.parseInt(configFile.readLine());
			iCacheLevels.add(level, new Cache(s, l, m, writePolicy,
					cacheAccessTime));

			configFile.readLine();
			level++;
		}
		configFile.close();

		// Start Of New Code
		// ROB
		this.head = 1;
		this.tail = 1;
		this.reorderBuffer = new HashMap<Integer, String []>();
		//Initialization of ROB Array
		
		String [] robInitialArray = new String[6];
		for(int i=0; i<6; i++) {
			robInitialArray[i] = "$$$$$$$$$$$$$$$$";
		}

		for (int i = 1; i <= numberOfRobEntries; i++) {
			this.reorderBuffer.put(i, robInitialArray);
		}

		
		//Reservation Stations
		this.reservationStations = new HashMap<String, String []>();
		//Initialization of RS Array
		String [] rsInitialArray = new String[10];
		for(int i=0; i<10; i++) {
			if(i==0)
				rsInitialArray[i] = "n";
			else
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

		// Register Status
		this.registerStatus = new HashMap<Integer, Integer>();
		for (int i = 0; i < 8; i++) {
			registerStatus.put(i, 0);
		}


		
		//WriteBuffer
		this.writeBuffer = new HashMap<Integer, String []>();
		

		this.writeWaitingCycles = 1;
		
		//End Of New Code
		
	}

	public String readData(String address, boolean iCacheOrDCache,
			String dataToBeStored) {
		ArrayList<Cache> cacheLevels = (iCacheOrDCache) ? iCacheLevels
				: dCacheLevels;
		int i = 0;
		String data = "";
		String offset = "";
		for (i = 0; i < cacheLevels.size(); i++) {
			totalNumberOfCyclesSpentForMemory += cacheLevels.get(i)
					.getCacheAccessTime();
			data = cacheLevels.get(i).readCache(address);
			offset = cacheLevels.get(i).splitAddress(address).get("offset");
			// String [] dataBytes = new String[data.length()/8];
			if (data != null) {
				String[] dataBytes = new String[data.length() / 8];
				writeCacheRecursively(address, i - 1, iCacheOrDCache,
						dataToBeStored);

				if (offset.length() == 1) {
					return data;
				} else {
					int wordNumber = Integer.parseInt(
							offset.substring(0, offset.length() - 1), 2);
					for (int k = 0; k < data.length() / 8; k++) {
						dataBytes[k] = data.substring(k * 8, (k + 1) * 8);
					}
					return dataBytes[wordNumber] + dataBytes[wordNumber + 1];
				}
			}
		}

		totalNumberOfCyclesSpentForMemory += memory.getMemoryAccessTime();
		int wordNumber = (offset.length() == 1) ? 0 : Integer.parseInt(
				offset.substring(0, offset.length() - 1), 2);
		String[] block = memory.read(address, cacheLevels.get(i - 1));
		writeCacheRecursively(address, i - 1, iCacheOrDCache, dataToBeStored);
		return block[wordNumber] + block[wordNumber + 1];

	}

	public void writeCacheRecursively(String address, int index,
			boolean iCacheOrDCache, String dataToBeStored) {
		ArrayList<Cache> cacheLevels = (iCacheOrDCache) ? iCacheLevels
				: dCacheLevels;
		if (!dataToBeStored.equalsIgnoreCase("")
				&& index < cacheLevels.size() - 1) {
			index++;
		}
		for (int i = index; i >= 0; i--) {
			String[] dataArray = memory.read(address, cacheLevels.get(i));
			String data = "";
			boolean dirty = false;

			int wordNumber = (cacheLevels.get(i).splitAddress(address)
					.get("offset").length() == 1) ? 0 : Integer.parseInt(
					cacheLevels
							.get(i)
							.splitAddress(address)
							.get("offset")
							.substring(
									0,
									cacheLevels.get(i).splitAddress(address)
											.get("offset").length() - 1), 2);
			for (int j = 0; j < dataArray.length; j++)
				if (!dataToBeStored.equalsIgnoreCase("") && j == wordNumber) {
					data += dataToBeStored;
					dirty = true;
					j++;
				} else if (dataArray[j] == null)
					data += "$$$$$$$$";
				else
					data += dataArray[j];
			if (cacheLevels.get(i).writePolicy.equalsIgnoreCase("wt")) {
				memory.write(address, data);
				totalNumberOfCyclesSpentForMemory += memory
						.getMemoryAccessTime();
			}
			if (cacheLevels.get(i).writeCache(address,
					cacheLevels.get(i).trimData(data), dirty))
				totalNumberOfCyclesSpentForMemory += cacheLevels.get(i)
						.getCacheAccessTime();
		}
	}

	public String to16BinaryStringValue(int value) {
		String returnValue = Integer.toBinaryString(value);

		while (returnValue.length() < 16) {
			returnValue = "0" + returnValue;
		}

		return returnValue;
	}

	
	
	
	public static int signedBinaryToDecimal(String signed){
		int result = 0;		
		if (signed.startsWith("1")){
			signed = signed.replace('0', '2').replace('1', '0').replace('2', '1');
			result = 0 - (Integer.parseInt(signed, 2) + 1);
		} else {
			result = Integer.parseInt(signed, 2);
		}

		return result;
	}

	public double[] getGlobalAmat() {
		double globalAmatICache = iCacheLevels.get(0).getCacheAccessTime();
		double globalAmatDCache = dCacheLevels.get(0).getCacheAccessTime();
		for (int i = 1; i < iCacheLevels.size(); i++) {
			double hitRatio = iCacheLevels.get(i - 1).getMissPenalty();
			double missPen = iCacheLevels.get(i).getCacheAccessTime();
			if (i == iCacheLevels.size() - 1)
				missPen = memory.getMemoryAccessTime();
			globalAmatICache += hitRatio * missPen;
		}
		for (int i = 1; i < dCacheLevels.size(); i++) {
			double hitRatio = dCacheLevels.get(i - 1).getMissPenalty();
			double missPen = dCacheLevels.get(i).getCacheAccessTime();
			if (i == dCacheLevels.size() - 1)
				missPen = memory.getMemoryAccessTime();
			globalAmatDCache += hitRatio * missPen;
		}
		double[] result = { globalAmatDCache, globalAmatICache };
		return result;
	}

	public void commit() {

		/*
		 * I'm assuming here that "destination" & "value" in the ROB entry are
		 * BINARY STRINGS !! where the "destination" is either the binary
		 * representation for the register number, or it is the binary memory
		 * address in case of store instruction.
		 */

		
		for (int i = 1 ; i <= numberOfRobEntries; i++) {
			String[] currentRobEntry = new String[6];
			currentRobEntry = reorderBuffer.get(i);
			if(!currentRobEntry[5].equalsIgnoreCase("$$$$$$$$$$$$$$$$")) {
				int updatedClockCycle = Integer.parseInt(currentRobEntry[5], 2) + 1;
				currentRobEntry[5] = to16BinaryStringValue(updatedClockCycle);
				reorderBuffer.put(i, currentRobEntry);
			}
		}
		
		String[] headRobEntry = reorderBuffer.get(head);
		if(!headRobEntry[4].equalsIgnoreCase("$$$$$$$$$$$$$$$$")){
		int instructionNumber = Integer.parseInt(headRobEntry[4], 2);
		int clockCycle = Integer.parseInt(headRobEntry[5], 2);
		}
		boolean misprediction = false;
		
		String [] robInitialArray = new String[6];
		for(int i=0; i<6; i++) {
			robInitialArray[i] = "$$$$$$$$$$$$$$$$";
		}
		
		if (headRobEntry[3].equalsIgnoreCase("yes") || headRobEntry[3].equalsIgnoreCase("y")) {
			if ((headRobEntry[0].equalsIgnoreCase("store")) || headRobEntry[0].equalsIgnoreCase("st")) {
				int memoryAddress = Integer.parseInt(headRobEntry[1], 2);
				readData(to16BinaryStringValue(memoryAddress), false, headRobEntry[2]);	
				updateClockCycle(instructionNumber, clockCycle, 5);
			}
			else if (headRobEntry[0].equalsIgnoreCase("branch")) {
				if (!branchPrediction.get(instructionNumber).equalsIgnoreCase("$$$$$$$$$$$$$$$$")) {
					misprediction = true;
					instToFetchAddress = Integer.parseInt(branchPrediction.get(instructionNumber), 2);
					for (int i = 1; i <= numberOfRobEntries; i++) {
						reorderBuffer.put(i, robInitialArray);
					}
					deleteMispredictedFromRs(headRobEntry[4]);
				}
			} 
			else {
				registers.put(Integer.parseInt(headRobEntry[1], 2), headRobEntry[2]);
				updateClockCycle(instructionNumber, clockCycle, 5);
			}

			
			robInitialArray = new String[6]; // had a duplicate local variable error here so i removed the declaration
			for(int i=0; i<6; i++) {
				robInitialArray[i] = "$$$$$$$$$$$$$$$$";
			}

			reorderBuffer.put(head, robInitialArray);

			if (registerStatus.get(Integer.parseInt(headRobEntry[1], 2)) == head) {
				registerStatus.put(Integer.parseInt(headRobEntry[1], 2), 0);
			}

			if (head == numberOfRobEntries) {
				head = 1;
			} 
			else {
				head++;
			}
			if (misprediction) {
				tail = head;
			}
		}
	}
	
	public void updateClockCycle (int instructionNumber, int clockCycle, int type) {
		if(type ==0){
			//new instruction
			int[] instructionClockCycles = new int [5];
			instructionClockCycles[0] = clockCycle; 
			for(int i =1 ; i<instructionClockCycles.length;i++)
				instructionClockCycles[i] = 0;
			clockCycles.put(instructionNumber, instructionClockCycles);
		}
		else{
			int[] instructionClockCycles = new int [5];
			instructionClockCycles = clockCycles.get(instructionNumber);
			instructionClockCycles[type] = clockCycle;
			clockCycles.put(instructionNumber, instructionClockCycles);
		}
	}
	
	public void deleteMispredictedFromRs (String instructionNumber) {
		String[] rsEntry = new String[10];
		Object[] keySet = reservationStations.keySet().toArray();
		
		String[] rsInitialArray = new String[10];
		for(int i=0; i<rsInitialArray.length; i++) {
			rsInitialArray[i] = "$$$$$$$$$$$$$$$$";
		}
		
		for (int i = 0; i < keySet.length ; i++) {
			rsEntry = reservationStations.get(keySet[i]);
			int integerInstructionNumber = Integer.parseInt(instructionNumber, 2);
			int integerRobInstructionNumber = Integer.parseInt(rsEntry[8], 2);
			if (integerInstructionNumber <= integerRobInstructionNumber) {
				reservationStations.put((String) keySet[i], rsInitialArray);
			}
		}
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
	
	public void write() {
		Set<Integer> writeBufferKeys = writeBuffer.keySet();
		
		if(!writeBufferKeys.isEmpty()) {
			Integer insToBeWrittenKey = (Integer)writeBufferKeys.toArray()[0];
			for(int i=1; i<writeBufferKeys.size(); i++) {
				int key = (Integer)writeBufferKeys.toArray()[i]; 
				if(key < insToBeWrittenKey) {
					insToBeWrittenKey = key;
				}
			}
			
			for(int i=0; i<writeBufferKeys.size(); i++) {
				String [] writeBufferValue = writeBuffer.get(writeBufferKeys.toArray()[i]);
				writeBufferValue[2] = to16BinaryStringValue((Integer.parseInt(writeBufferValue[2],2) + 1));
				writeBuffer.put((Integer)writeBufferKeys.toArray()[i], writeBufferValue);						
			}
			
			String [] insArrayValues = writeBuffer.get(insToBeWrittenKey);
			
			boolean written = write(insArrayValues[0], insArrayValues[1], insArrayValues[2]);
			
			if(written) {
				updateClockCycle(insToBeWrittenKey, Integer.parseInt(insArrayValues[2], 2), 3);
				writeBufferKeys.remove(insToBeWrittenKey);
			}
		}

	}
	
	public boolean write(String executionResult, String rsName, String cycle) {
		String [] rsEntry = reservationStations.get(rsName);
		String robEntryNumber = rsEntry[6];
		String [] robEntry = reorderBuffer.get(Integer.parseInt(robEntryNumber, 2));
		boolean written = false;
		
		//Part el store dah msh waska feh !!!
		if(rsEntry[1].equalsIgnoreCase("store") || rsEntry[1].equalsIgnoreCase("st")) {
			if(rsEntry[5].equalsIgnoreCase("0000000000000000")) {
				//In the issue stage Qj lazem yb2a feh zero bs 16 bits
				if(writeWaitingCycles != storeLatency) {
					writeWaitingCycles++;
				}
				else{
					writeWaitingCycles = 1;
					robEntry[2] = rsEntry[3];
					robEntry[3] = "yes";
					robEntry[5] = cycle;
					reorderBuffer.put(Integer.parseInt(robEntryNumber, 2), robEntry);
					written = true;
				}
			}
		}
		else {
			writeToAwaitingUnits(executionResult, robEntryNumber);
			robEntry[2] = executionResult;
			robEntry[3] = "yes";
			robEntry[5] = cycle;
			reorderBuffer.put(Integer.parseInt(robEntryNumber, 2), robEntry);
			written = true;
		}
		
		if(written) {
			for(int i=0; i<rsEntry.length; i++) {
				rsEntry[i] = "$$$$$$$$$$$$$$$$";
			}
			reservationStations.put(rsName, rsEntry);
		}
		
		return written;
	}
	
	public void fetch(int j){
		if(unconditionalJMP == false){
			String fetchedInst ;
			while(instToFetchAddress<a.getEndAddress() && j<numberOfWays){
				
				//each two cells in memory is an instruction
				fetchedInst = readData(to16BinaryStringValue(instToFetchAddress), true, "");
				instBuffer[j] = ++instNumber;
				instArrayBinary.put(instNumber, fetchedInst);
				if(fetchedInst.startsWith("110")){
					//BEQ instruction
					if((signedBinaryToDecimal(fetchedInst.substring(9, 16)))<0){
						//prediction : Taken
						instToFetchAddress = Integer.parseInt(fetchedInst , 2)+ signedBinaryToDecimal(fetchedInst.substring(9, 16));
					}
					else //prediction : not taken
						instToFetchAddress += 2;
						
				}
				else if(fetchedInst.startsWith("0100000000") || fetchedInst.startsWith("001000") ||fetchedInst.startsWith("0110000000000")){
					// JALR or RET or JMP insructions always assumed to be taken 
					unconditionalJMP = true;
					
				}
				else
					instToFetchAddress += 2; 
				//update cycles table
				
				updateClockCycle (instNumber, programCycles, 0);
				j++;
			}
		}	
	}
	public void execute() {
		
		String instToExecute;
		String[] inner;
		
		
		
		//Load instructions
		for(int a = 1 ; a<=loadRs ; a++ ){
			String x = "Load"+a;
			inner = reservationStations.get(x);
			if(inner[0].equalsIgnoreCase("Y") && inner[4].startsWith("$") && inner[9].startsWith("$")){
				// new load instruction
				instToExecute = instArrayBinary.get(Integer.parseInt(inner[8] , 2));
				int regB = Integer.parseInt(instToExecute.substring(6, 9), 2);
				String immediateValue = instToExecute.substring(9, 16);
				int memoryAddress = Integer.parseInt(registers.get(regB), 2) + signedBinaryToDecimal(immediateValue);
				// writing memory address to get data from into A
				reservationStations.get(x)[7] = to16BinaryStringValue(memoryAddress);
				// writing the load latency in the last cell in rs
				reservationStations.get(x)[9] = to16BinaryStringValue(loadLatency);

			}
			else if(inner[0].equalsIgnoreCase("Y") && inner[4].startsWith("$") && !inner[9].startsWith("$")){
					// load instruction in execute process
					if(Integer.parseInt(inner[9],2)==0){
						// load instruction just finished execution 
						instToExecute = instArrayBinary.get(Integer.parseInt(inner[8] , 2));
						int regB = Integer.parseInt(instToExecute.substring(6, 9), 2);
						String immediateValue = instToExecute.substring(9, 16);
						int memoryAddress = Integer.parseInt(registers.get(regB), 2) + signedBinaryToDecimal(immediateValue);
						//result is the data to be read
						String result = readData(to16BinaryStringValue(memoryAddress), false, "");
						String[] writeBufferInnerArray = new String[3];
						writeBufferInnerArray[0] = result;
						writeBufferInnerArray[1] = x;
						writeBufferInnerArray[3] = to16BinaryStringValue(programCycles+loadLatency);
						//writing to write buffer
						writeBuffer.put(Integer.parseInt(inner[8],2), writeBufferInnerArray);
						reservationStations.get(x)[9] = "-1";
						//update cycles table
						updateClockCycle(Integer.parseInt(inner[8],2), programCycles+loadLatency, 2);
				}
				else if(Integer.parseInt(inner[9],2)>0){
						// load instruction still not finished
						reservationStations.get(x)[9] = to16BinaryStringValue((Integer.parseInt(reservationStations.get(x)[9],2))-1);
				} 
			}
		}
		//Store instructions
		for(int a = 1 ; a<=storeRs ; a++){
			String x = "Store"+a;
			inner = reservationStations.get(x);
			if(inner[0].equalsIgnoreCase("Y") && inner[4].startsWith("$") && inner[9].startsWith("$")){
				// new store instruction
				instToExecute = instArrayBinary.get(Integer.parseInt(inner[8] , 2));
				int regB = Integer.parseInt(instToExecute.substring(6, 9), 2);
				String immediateValue = instToExecute.substring(9, 16);
				int memoryAddress = Integer.parseInt(registers.get(regB), 2) + signedBinaryToDecimal(immediateValue);
				// writing memory address to write data to into A
				reservationStations.get(x)[7] = to16BinaryStringValue(memoryAddress);
				// writing the store latency in the last cell in rs
				reservationStations.get(x)[9] = to16BinaryStringValue(storeLatency);
			}
			else if(inner[0].equalsIgnoreCase("Y") && inner[4].startsWith("$") && !inner[9].startsWith("$")){
					// store instruction in execute process
					if(Integer.parseInt(inner[9],2)==0){
						//store instruction just finished execution
						String[] writeBufferInnerArray = new String[3];
						writeBufferInnerArray[0] = "$$$$$$$$$$$$$$$$";
						writeBufferInnerArray[1] = x;
						writeBufferInnerArray[3] = to16BinaryStringValue(programCycles+storeLatency);
						//writing to write buffer
						writeBuffer.put(Integer.parseInt(inner[8],2), writeBufferInnerArray);
						reservationStations.get(x)[9] = "-1";
						//update cycles table
						updateClockCycle(Integer.parseInt(inner[8],2), programCycles+storeLatency, 2);
					}
					else if(Integer.parseInt(inner[9],2)>0){
							// store instruction still not finished
							reservationStations.get(x)[9] = to16BinaryStringValue((Integer.parseInt(reservationStations.get(x)[9],2))-1);
					} 
				
				
			}
		}
		//ADD single precision instruction
		for(int a = 1 ;a<integerAddSubRs ; a++){
			String x = "Add"+a;
			inner = reservationStations.get(x);
			
			
			//JALR instruction
			if(inner[1].equalsIgnoreCase("jalr")) {
				
				if(inner[0].equalsIgnoreCase("Y") && inner[4].startsWith("$") && inner[9].startsWith("$")){
					//new JALR instruction
					reservationStations.get(x)[9] = to16BinaryStringValue(integerAddSubLatency);
				}
				else if(inner[0].equalsIgnoreCase("Y") && inner[4].startsWith("$") && !inner[9].startsWith("$")){
						// JALR instruction in process
						if(Integer.parseInt(inner[9],2) == 0){
							//JALR instruction just finished execution
							instToExecute = instArrayBinary.get(Integer.parseInt(inner[8] , 2));
							int entry = Integer.parseInt(reservationStations.get(x)[6],2);
							reorderBuffer.get(entry)[3] = "yes";
							reorderBuffer.get(entry)[5] = to16BinaryStringValue(integerAddSubLatency+programCycles);
							int regA = Integer.parseInt(instToExecute.substring(10, 13), 2);
							int regB = Integer.parseInt(instToExecute.substring(13, 16), 2);
							String result = to16BinaryStringValue((Integer.parseInt(instToExecute , 2)+2));
							if (regA != 0) {
								registers.put(regA, result);
							}
							instToFetchAddress = Integer.parseInt((registers.get(regB)),2);
							unconditionalJMP = false;
							reservationStations.get(x)[9] = "-1";
							//update cycles table
							updateClockCycle(Integer.parseInt(inner[8],2), programCycles+integerAddSubLatency, 2);

							
						}
						else if(Integer.parseInt(inner[9],2)>0){
							// JALR instruction still not finished
							reservationStations.get(x)[9] = to16BinaryStringValue((Integer.parseInt(reservationStations.get(x)[9],2))-1);

						}
				}
			}	
		
			//RET instruction
			if(inner[1].equalsIgnoreCase("ret")) {
				
				if(inner[0].equalsIgnoreCase("Y") && inner[4].startsWith("$") && inner[9].startsWith("$")){
					//new RET instruction
					reservationStations.get(x)[9] = to16BinaryStringValue(integerAddSubLatency);
				}
				else if(inner[0].equalsIgnoreCase("Y") && inner[4].startsWith("$") && !inner[9].startsWith("$")){
						//RET instruction in process
						if(Integer.parseInt(inner[9],2)==0){
							//RET instruction just finished execution
							instToExecute = instArrayBinary.get(Integer.parseInt(inner[8] , 2));
							int entry = Integer.parseInt(reservationStations.get(x)[6],2);
							reorderBuffer.get(entry)[3] = "yes";
							reorderBuffer.get(entry)[5] = to16BinaryStringValue(integerAddSubLatency+programCycles);
							int regA = Integer.parseInt(instToExecute.substring(13, 16), 2);
							instToFetchAddress = Integer.parseInt((registers.get(regA)),2);
							unconditionalJMP = false;
							reservationStations.get(x)[9] = "-1";
							//update cycles table
							updateClockCycle(Integer.parseInt(inner[8],2), programCycles+integerAddSubLatency, 2);
							
						}
						else if(Integer.parseInt(inner[9],2)>0){
							// RET instruction still not finished
							reservationStations.get(x)[9] = to16BinaryStringValue((Integer.parseInt(reservationStations.get(x)[9],2))-1);
						}
				}
			}
			//JMP instruction
			if(inner[1].equalsIgnoreCase("jmp")) {
				
				if(inner[0].equalsIgnoreCase("Y") &&  inner[4].startsWith("$") && inner[9].startsWith("$")){
					// new JMP instruction 
					reservationStations.get(x)[9] = to16BinaryStringValue(integerAddSubLatency);
				}
				else if(inner[0].equalsIgnoreCase("Y") && inner[4].startsWith("$") && !inner[9].startsWith("$")){
						// JMP instruction in process
						if(Integer.parseInt(inner[9],2)==0){
							//JMP instruction just finished execution
							instToExecute = instArrayBinary.get(Integer.parseInt(inner[8] , 2));
							int entry = Integer.parseInt(reservationStations.get(x)[6],2);
							reorderBuffer.get(entry)[3] = "yes";
							reorderBuffer.get(entry)[5] = to16BinaryStringValue(integerAddSubLatency+programCycles);
							int regA = Integer.parseInt(instToExecute.substring(6, 9), 2);
							String immediateValue = instToExecute.substring(9, 16);
							instToFetchAddress = Integer.parseInt(instToExecute , 2)+ Integer.parseInt((registers.get(regA)),2)+ signedBinaryToDecimal(immediateValue);
							unconditionalJMP = false ;
							reservationStations.get(x)[9] = "-1";
							//update cycles table
							updateClockCycle(Integer.parseInt(inner[8],2), programCycles+integerAddSubLatency, 2);
						}
						else if(Integer.parseInt(inner[9],2)>0){
							// RET instruction still not finished
							reservationStations.get(x)[9] = to16BinaryStringValue((Integer.parseInt(reservationStations.get(x)[9],2))-1);
						}
				}
				 
			}	
			//ADDI
			if(inner[1].equalsIgnoreCase("addi")){
				
				if(inner[0].equalsIgnoreCase("Y") &&  inner[4].startsWith("$")&& inner[9].startsWith("$")){
					// new ADDI instruction 
					
					reservationStations.get(x)[9] = to16BinaryStringValue(integerAddSubLatency);
				}
				else if(inner[0].equalsIgnoreCase("Y") && inner[4].startsWith("$")&& !inner[9].startsWith("$")){
						// ADDI instruction in execute process
						if(Integer.parseInt(inner[9],2) == 0){
							//ADDI instruction just finished execution
							instToExecute = instArrayBinary.get(Integer.parseInt(inner[8] , 2));
							int regB = Integer.parseInt(instToExecute.substring(6, 9), 2);
							int immediateValue = signedBinaryToDecimal(instToExecute.substring(9, 16));
							int r = Integer.parseInt(registers.get(regB), 2) + immediateValue;
							//result is the value to be stored in the destination register(regA)
							String result = to16BinaryStringValue(r);
							String[] writeBufferInnerArray = new String[3];
							writeBufferInnerArray[0] = result;
							writeBufferInnerArray[1] = x;
							writeBufferInnerArray[3] = to16BinaryStringValue(programCycles+integerAddSubLatency);
							//writing to write buffer
							writeBuffer.put(Integer.parseInt(inner[8],2), writeBufferInnerArray);
							reservationStations.get(x)[9] = "-1";
							//update cycles table
							updateClockCycle(Integer.parseInt(inner[8],2), programCycles+integerAddSubLatency, 2);
						}
						else if(Integer.parseInt(inner[9],2)>0){
							// ADDI instruction still not finished
							reservationStations.get(x)[9] = to16BinaryStringValue((Integer.parseInt(reservationStations.get(x)[9],2))-1);
						}
				}
			}
		}

		//ADD double precision instruction
		for(int a = 1 ; a<doublePrecisionAddSubRs ; a++){
			String x = "Addd"+a;
			inner = reservationStations.get(x);
			
			
			//BEQ instruction 
			if(inner[1].equalsIgnoreCase("branch")){
				
				if(inner[0].equalsIgnoreCase("Y") && inner[4].startsWith("$") && inner[5].startsWith("$") && inner[9].startsWith("$")){
					// new BEQ instruction
					reservationStations.get(x)[9] = to16BinaryStringValue(doublePrecisionAddSubLatency);
				}
				else if(inner[0].equalsIgnoreCase("Y") && inner[4].startsWith("$") && inner[5].startsWith("$")&& !inner[9].startsWith("$")){
						// BEQ instruction in process
						if(Integer.parseInt(inner[9],2)==0){
							//BEQ just finished execution
							instToExecute = instArrayBinary.get(Integer.parseInt(inner[8] , 2));
							int regA = Integer.parseInt(instToExecute.substring(3, 6), 2);
							int regB = Integer.parseInt(instToExecute.substring(6, 9), 2);
							String immediateValue = instToExecute.substring(9, 16);
							String regAValue = registers.get(regA);
							String regBValue = registers.get(regB);
							int entry = Integer.parseInt(reservationStations.get(x)[6],2);
							reorderBuffer.get(entry)[3] = "yes";
							reorderBuffer.get(entry)[5] = to16BinaryStringValue(doublePrecisionAddSubLatency+programCycles);
							reservationStations.get(x)[9] = "-1";
							//update cycles table
							updateClockCycle(Integer.parseInt(inner[8],2), programCycles+doublePrecisionAddSubLatency, 2);
							
							if(signedBinaryToDecimal(immediateValue)<0){
								//branch prediction : Taken
								if(regAValue.equalsIgnoreCase(regBValue)){
									//branch is taken
									branchPrediction.put(Integer.parseInt(inner[8],2), "$$$$$$$$$$$$$$$$");
								}
								else //misprediction assumed to be taken and turned out to be not taken
									
									branchPrediction.put(Integer.parseInt(inner[8],2), to16BinaryStringValue((Integer.parseInt(instToExecute,2)+2)));
							}
							else{
								//branch prediction : not taken
								if(!regAValue.equalsIgnoreCase(regBValue)){
									//branch is not taken
									branchPrediction.put(Integer.parseInt(inner[8],2), "$$$$$$$$$$$$$$$$");
								}
								else //misprediction assumed to be not taken and turned out to be taken
								
									branchPrediction.put(Integer.parseInt(inner[8],2), to16BinaryStringValue((Integer.parseInt(instToExecute , 2)+ signedBinaryToDecimal(instToExecute.substring(9, 16)))));
							}
						}
						else if(Integer.parseInt(inner[9],2)>0){
							// BEQ instruction still not finished
							reservationStations.get(x)[9] = to16BinaryStringValue((Integer.parseInt(reservationStations.get(x)[9],2))-1);
						}
				}
			}
		
			//ADD instruction
			if(inner[1].equalsIgnoreCase("addd")) {
				
				if(inner[0].equalsIgnoreCase("Y") && inner[4].startsWith("$") && inner[5].startsWith("$") && inner[9].startsWith("$")){
					// new ADD instruction 
					reservationStations.get(x)[9] = to16BinaryStringValue(doublePrecisionAddSubLatency);
				}
				else if(inner[0].equalsIgnoreCase("Y") && inner[4].startsWith("$") && inner[5].startsWith("$") && !inner[9].startsWith("$")){
						// ADD instruction in process 
						if(Integer.parseInt(inner[9],2)==0){
							// ADD instruction just finished execution
							instToExecute = instArrayBinary.get(Integer.parseInt(inner[8] , 2));
							int regB = Integer.parseInt(instToExecute.substring(10, 13), 2);
							int regC = Integer.parseInt(instToExecute.substring(13, 16), 2);
							int r = Integer.parseInt(registers.get(regB),2) + Integer.parseInt(registers.get(regC),2);
							//result to be stored in destination register (regA)
							String result = to16BinaryStringValue(r);
							String[] writeBufferInnerArray = new String[3];
							writeBufferInnerArray[0] = result;
							writeBufferInnerArray[1] = x;
							writeBufferInnerArray[3] = to16BinaryStringValue(programCycles+doublePrecisionAddSubLatency);
							//writing to write buffer
							writeBuffer.put(Integer.parseInt(inner[8],2), writeBufferInnerArray);
							reservationStations.get(x)[9] = "-1";
							//update cycles table
							updateClockCycle(Integer.parseInt(inner[8],2), programCycles+doublePrecisionAddSubLatency, 2);

						}
						else if(Integer.parseInt(inner[9],2)>0){
							// ADD instruction still not finished
							reservationStations.get(x)[9] = to16BinaryStringValue((Integer.parseInt(reservationStations.get(x)[9],2))-1);
						}
				}
			}
			//SUB instruction
			if(inner[1].equalsIgnoreCase("subb")) {
				
				if(inner[0].equalsIgnoreCase("Y") && inner[4].startsWith("$") && inner[5].startsWith("$") && inner[9].startsWith("$")){
					// new SUB instruction 
					reservationStations.get(x)[9] = to16BinaryStringValue(doublePrecisionAddSubLatency);
				}
				else if(inner[0].equalsIgnoreCase("Y") && inner[4].startsWith("$") && inner[5].startsWith("$") && !inner[9].startsWith("$")){
					// SUB instruction in process 
					if(Integer.parseInt(inner[9],2)==0){
						// SUB instruction just finished execution
						instToExecute = instArrayBinary.get(Integer.parseInt(inner[8] , 2));
						int regB = Integer.parseInt(instToExecute.substring(10, 13), 2);
						int regC = Integer.parseInt(instToExecute.substring(13, 16), 2);
						int r = Integer.parseInt(registers.get(regB),2) - Integer.parseInt(registers.get(regC),2);
						//result to be stored in destination register (regA)
						String result = to16BinaryStringValue(r);
						String[] writeBufferInnerArray = new String[3];
						writeBufferInnerArray[0] = result;
						writeBufferInnerArray[1] = x;
						writeBufferInnerArray[3] = to16BinaryStringValue(programCycles+doublePrecisionAddSubLatency);
						//writing to write buffer
						writeBuffer.put(Integer.parseInt(inner[8],2), writeBufferInnerArray);
						reservationStations.get(x)[9] = "-1";
						//update cycles table
						updateClockCycle(Integer.parseInt(inner[8],2), programCycles+doublePrecisionAddSubLatency, 2);
					}
					else if(Integer.parseInt(inner[9],2)>0){
						//SUB instruction still not finished
						reservationStations.get(x)[9] = to16BinaryStringValue((Integer.parseInt(reservationStations.get(x)[9],2))-1);
					}
				}
			}
			//NAND instruction
			if(inner[1].equalsIgnoreCase("nand")) {
				
				if(inner[0].equalsIgnoreCase("Y") && inner[4].startsWith("$") && inner[5].startsWith("$") && inner[9].startsWith("$")){
					// new NAND instruction 
					reservationStations.get(x)[9] = to16BinaryStringValue(doublePrecisionAddSubLatency);
				}
				else if(inner[0].equalsIgnoreCase("Y") && inner[4].startsWith("$") && inner[5].startsWith("$") && !inner[9].startsWith("$")){
					// NAND instruction in process 
					if(Integer.parseInt(inner[9],2)==0){
						//NAND instruction just finished execution
						instToExecute = instArrayBinary.get(Integer.parseInt(inner[8] , 2));
						int regB = Integer.parseInt(instToExecute.substring(10, 13), 2);
						int regC = Integer.parseInt(instToExecute.substring(13, 16), 2);
						int r = (Integer.parseInt(registers.get(regB),2) & Integer.parseInt(registers.get(regC),2));
						//result to be stored in destination register (regA)
						String result = to16BinaryStringValue(r);
						String[] writeBufferInnerArray = new String[3];
						writeBufferInnerArray[0] = result;
						writeBufferInnerArray[1] = x;
						writeBufferInnerArray[3] = to16BinaryStringValue(programCycles+doublePrecisionAddSubLatency);
						//writing to write buffer
						writeBuffer.put(Integer.parseInt(inner[8],2), writeBufferInnerArray);
						reservationStations.get(x)[9] = "-1";
						//update cycles table
						updateClockCycle(Integer.parseInt(inner[8],2), programCycles+doublePrecisionAddSubLatency, 2);

					}
					else if(Integer.parseInt(inner[9],2)>0){
						//NAND instruction still not finished
						reservationStations.get(x)[9] = to16BinaryStringValue((Integer.parseInt(reservationStations.get(x)[9],2))-1);
					}
				}
			}
		}

		//Mul and Div instructions 
		for(int a = 1 ; a<multDivRs ; a++){
			String x = "Multd"+a;
			inner = reservationStations.get(x);
			if(inner[0].equalsIgnoreCase("Y") && inner[4].startsWith("$") && inner[5].startsWith("$") && inner[9].startsWith("$")){
				//new Mul instruction 
				reservationStations.get(x)[9] = to16BinaryStringValue(multDivLatency);
			}
			else if(inner[0].equalsIgnoreCase("Y") && inner[4].startsWith("$") && inner[5].startsWith("$") && !inner[9].startsWith("$")){
					//Mul instruction in process
					if(Integer.parseInt(inner[9],2)==0){
						//Mul instruction just finished execution
						instToExecute = instArrayBinary.get(Integer.parseInt(inner[8] , 2));
						int regB = Integer.parseInt(instToExecute.substring(10, 13), 2);
						int regC = Integer.parseInt(instToExecute.substring(13, 16), 2);
						int r = Integer.parseInt(registers.get(regB),2) * Integer.parseInt(registers.get(regC),2);
						//result to be stored in destination register (regA)
						String result = to16BinaryStringValue(r);
						String[] writeBufferInnerArray = new String[3];
						writeBufferInnerArray[0] = result;
						writeBufferInnerArray[1] = x;
						writeBufferInnerArray[3] = to16BinaryStringValue(programCycles+multDivLatency);
						//writing to write buffer
						writeBuffer.put(Integer.parseInt(inner[8],2), writeBufferInnerArray);
						reservationStations.get(x)[9] = "-1";
						//update cycles table
						updateClockCycle(Integer.parseInt(inner[8],2), programCycles+multDivLatency, 2);

					}
					else if(Integer.parseInt(inner[9],2)>0){
						//Mul instruction still not finished
						reservationStations.get(x)[9] = to16BinaryStringValue((Integer.parseInt(reservationStations.get(x)[9],2))-1);
					}
			}
		}
	}
	
	public void ClockCycle() {
		issue();
		execute();
		write();
		commit();
		
		programCycles ++;
	}
	
	public void printClockCycleTable() {
		System.out.println("\t (F)  (I)  (E)  (W)  (C)");
		for(int i=0; i<clockCycles.size(); i++) {
			System.out.println("I" + i + "  " + ((clockCycles.get(i)[0]<9)?clockCycles.get(i)[0]+" ":clockCycles.get(i)[0]) +  "  " 
					+ ((clockCycles.get(i)[1]<9)?clockCycles.get(i)[1]+" ":clockCycles.get(i)[1]) + "  "
					+ ((clockCycles.get(i)[2]<9)?clockCycles.get(i)[2]+" ":clockCycles.get(i)[2]) + "  " 
					+ ((clockCycles.get(i)[3]<9)?clockCycles.get(i)[3]+" ":clockCycles.get(i)[3]) + "  " 
					+ ((clockCycles.get(i)[4]<9)?clockCycles.get(i)[4]+" ":clockCycles.get(i)[4]));
		}
	}
	public void mispredictionPercentage(){
		
		int numberOfmisprediction = 0;
		for(int i : branchPrediction.keySet()){
			if(!branchPrediction.get(i).startsWith("$"))
				numberOfmisprediction++;
		}
		System.out.println("The branch misprediction percentage is : "+((numberOfmisprediction*100)/branchPrediction.size())+"%");
	}
	public void totalNumberOfCycles(){
		int lastInstNumber = clockCycles.size();
		int [] inner = clockCycles.get(lastInstNumber);
		System.out.println("The program total execution time is : "+inner[4]+" Cycles");
	}
	public void averageIPC(){
		int total = 0;
		for(int i : clockCycles.keySet()){
			total += clockCycles.get(i)[4]-clockCycles.get(i)[0];
		}
		int cpi = total/clockCycles.size();
		System.out.println("The approximate number of instructions per cycle IPC is : "+(1/cpi)+" instruction per cycle");
		
	}
///////////////////////////////old execute///////////////////////////////////////////////////////////
//		public void execute() {
//		int address = this.pc;
//		String dataAddress = to16BinaryStringValue(address);		
//		String data = Data(dataAddress, true, "");
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
		System.out
				.println("Please enter the directory for your file , your file should look like this :-"
						+ "\n"
						+ "\n"
						+ "CODE"
						+ "\n"
						+ "Base Address  (write the base address for your program in hexadecimal format 0x...)"
						+ "\n"
						+ "first program instruction"
						+ "\n"
						+ "second program instruction"
						+ "\n"
						+ "third program instruction"
						+ "\n"
						+ "....."
						+ "\n"
						+ ".....  (the instruction should look like this : instName operand1,operand2,... )"
						+ "\n"
						+ "DATA"
						+ "\n"
						+ "value1,address1"
						+ "\n"
						+ "value2,address2"
						+ "\n"
						+ "value3,address3"
						+ "\n"
						+ "....."
						+ "\n"
						+ ".....  "
						+ "(the data address should also be writtin in hexamdecimal format 0x...)"
						+ "\n"
						+ "\n"
						+ "Some guidelines to follow :"
						+ "\n"
						+ "1)No additional/missing spaces if not specified in the above format are allowed ."
						+ "\n"
						+ "2)No additional/missing semicollons are allowed ."
						+ "\n"
						+ "3)Semicollon should be inserted in between two operands ."
						+ "\n"
						+ "4)No empty lines are allowed within the text or after it which means that the text file should start with the word "
						+ "\"CODE\""
						+ "\n"
						+ "  and ends with the last data value ."
						+ "\n"
						+ "5)The word \"CODE\" comes before your program code at the "
						+ "very first line of the file"
						+ "\n"
						+ "  and the word \"DATA\" comes before the data separting between the code and the data lines."
						+ "\n"
						+ "**ANY TEXT FILE VIOLATING ONE OF THE ABOVE CONDITIONS WILL NOT BE ACCEPTED :)");
		System.out.println("enter the program file path: ");
		String prog = sc.nextLine();
		System.out.println("enter the config file path: ");
		String config = sc.nextLine();
		Microprocessor m = new Microprocessor(new File(config), new File(prog));
		sc.close();
//		System.out.print(m.readData(m.to16BinaryStringValue(m.a.getBaseAddress()), true, "")); // da 3ashan a3raf howa shayef awel instruction f awel address
		m.fetch(0);
		m.ClockCycle();
		m.mispredictionPercentage();
		m.totalNumberOfCycles();
		m.averageIPC();
	}

	public void issue() {
		/*
		 * destination registers needs more bits than the available in the
		 * instructions
		 */
		boolean issued [] = new boolean[numberOfWays];
		for (int i = 0; i < numberOfWays; i++) {
			String data = instArrayBinary.get(instBuffer[i]);
			if (data.startsWith("100")) {
				// load
				int regA = Integer.parseInt(data.substring(3, 6), 2);
				int regB = Integer.parseInt(data.substring(6, 9), 2);
				String immediateValue = data.substring(9, 16);
				int j = 1;
				for (j = 1; j <= loadRs; j++)
					if (reservationStations.get("Load" + j)[0]
							.equalsIgnoreCase("n"))
						break;

				if (j <= loadRs 
						&& !(head == tail && !reorderBuffer.get(head)[0]
								.contains("$"))) {
					issued[i]=true;
					String [] resEntry = reservationStations.get("Load" + j);
					
					resEntry[0] = "Y";
					resEntry[1] = "load";
					if (registerStatus.get(regB) == 0) {
						resEntry[2] = registers
								.get(regB);
						resEntry[4] = "$$$$$$$$$$$$$$$$";
					}else{
						resEntry[2] = "$$$$$$$$$$$$$$$$";
						resEntry[4] = to16BinaryStringValue(registerStatus.get(regB));
					}
					resEntry[3] = "$$$$$$$$$$$$$$$$";
					resEntry[5] = "$$$$$$$$$$$$$$$$";
					resEntry[6] = to16BinaryStringValue(tail);
					resEntry[7] = immediateValue;
					resEntry[8] = to16BinaryStringValue(instBuffer[i]);
					String robEntry[] = { "Load", regA + "", "$$$$$$$$$$$$$$$$", "N" ,to16BinaryStringValue(instBuffer[i])};
					reorderBuffer.put(tail, robEntry);
					registerStatus.put(regA, tail);
					reservationStations.put("Load"+j, resEntry);
					// increment tail correctly
					tail = (tail == numberOfRobEntries) ? 1 : tail + 1;
				}

			} else if (data.startsWith("101")) {
				// store
				int regA = Integer.parseInt(data.substring(3, 6), 2);
				int regB = Integer.parseInt(data.substring(6, 9), 2);
				String immediateValue = data.substring(9, 16);
				int j = 1;
				for (j = 1; j <= storeRs; j++)
					if (reservationStations.get("Store" + j)[0]
							.equalsIgnoreCase("n"))
						break;

				if (j <= storeRs 
						&& !(head == tail && !reorderBuffer.get(head)[0]
								.contains("$"))) {
					issued[i]=true;
					String [] resEntry = reservationStations.get("Store" + j);
					
					resEntry[0] = "Y";
					resEntry[1] = "Store";
					if (registerStatus.get(regB) == 0) {
						resEntry[2] = registers
								.get(regB);
						resEntry[4] = "$$$$$$$$$$$$$$$$";
					}else{
						resEntry[2] = "$$$$$$$$$$$$$$$$";
						resEntry[4] = to16BinaryStringValue(registerStatus.get(regB));
					}
					resEntry[3] = "$$$$$$$$$$$$$$$$";
					resEntry[5] = "$$$$$$$$$$$$$$$$";
					resEntry[6] = to16BinaryStringValue(tail);
					resEntry[7] = immediateValue;
					resEntry[8] = to16BinaryStringValue(instBuffer[i]);
					String robEntry[] = { "Store", regB + "", "$$$$$$$$$$$$$$$$", "N" ,to16BinaryStringValue(instBuffer[i])};
					reorderBuffer.put(tail, robEntry);
					registerStatus.put(regA, tail);
					reservationStations.put("Store"+j, resEntry);
					// increment tail correctly
					tail = (tail == numberOfRobEntries) ? 1 : tail + 1;
				}

			} else if (data.startsWith("110")) {
				// BEQ
				int regA = Integer.parseInt(data.substring(3, 6), 2);
				int regB = Integer.parseInt(data.substring(6, 9), 2);
				String immediateValue = data.substring(9, 16);

				int j = 1;
				for (j = 1; j <= doublePrecisionAddSubRs ; j++)
					if (reservationStations.get("addd" + j)[0]
							.equalsIgnoreCase("n"))
						break;

				if (j <= doublePrecisionAddSubRs
						&& !(head == tail && !reorderBuffer.get(head)[0]
								.contains("$"))) {
					issued[i]=true;
					String [] resEntry = reservationStations.get("addd" + j);
					
					resEntry[0] = "Y";
					resEntry[1] = "branch";
					if (registerStatus.get(regB) == 0) {
						resEntry[2] = registers
								.get(regB);
						resEntry[4] = "$$$$$$$$$$$$$$$$";
					}else{
						resEntry[2] = "$$$$$$$$$$$$$$$$";
						resEntry[4] = to16BinaryStringValue(registerStatus.get(regB));
					}
					if (registerStatus.get(regA) == 0) {
						resEntry[3] = registers
								.get(regA);
						resEntry[5] = "$$$$$$$$$$$$$$$$";
					}else{
						resEntry[3] = "$$$$$$$$$$$$$$$$";
						resEntry[5] = to16BinaryStringValue(registerStatus.get(regA));
					}
					resEntry[6] = to16BinaryStringValue(tail);
					resEntry[7] = immediateValue;
					resEntry[8] = to16BinaryStringValue(instBuffer[i]);
					String robEntry[] = { "branch", regA + "", "$$$$$$$$$$$$$$$$", "N" ,to16BinaryStringValue(instBuffer[i])};
					reorderBuffer.put(tail, robEntry);
					registerStatus.put(regA, tail);
					reservationStations.put("branch"+j, resEntry);
					// increment tail correctly
					tail = (tail == numberOfRobEntries) ? 1 : tail + 1;
				}

			} else if (data.startsWith("111")) {
				// AddI
				int regA = Integer.parseInt(data.substring(3, 6), 2);
				int regB = Integer.parseInt(data.substring(6, 9), 2);
				String immediateValue = data.substring(9, 16);

				int j = 1;
				for (j = 1; j <= integerAddSubRs ; j++)
					if (reservationStations.get("add" + j)[0]
							.equalsIgnoreCase("n"))
						break;

				if (j <= integerAddSubRs
						&& !(head == tail && !reorderBuffer.get(head)[0]
								.contains("$"))) {
					issued[i]=true;
					String [] resEntry = reservationStations.get("add" + j);
					
					resEntry[0] = "Y";
					resEntry[1] = "addi";
					if (registerStatus.get(regB) == 0) {
						resEntry[2] = registers
								.get(regB);
						resEntry[4] = "$$$$$$$$$$$$$$$$";
					}else{
						resEntry[2] = "$$$$$$$$$$$$$$$$";
						resEntry[4] = to16BinaryStringValue(registerStatus.get(regB));
					}
					if (registerStatus.get(regA) == 0) {
						resEntry[3] = registers
								.get(regA);
						resEntry[5] = "$$$$$$$$$$$$$$$$";
					}else{
						resEntry[3] = "$$$$$$$$$$$$$$$$";
						resEntry[5] = to16BinaryStringValue(registerStatus.get(regA));
					}
					resEntry[6] = to16BinaryStringValue(tail);
					resEntry[7] = immediateValue;
					resEntry[8] = to16BinaryStringValue(instBuffer[i]);
					String robEntry[] = { "addi", regA + "", "$$$$$$$$$$$$$$$$", "N" ,to16BinaryStringValue(instBuffer[i])};
					reorderBuffer.put(tail, robEntry);
					registerStatus.put(regA, tail);
					reservationStations.put("add"+j, resEntry);
					// increment tail correctly
					tail = (tail == numberOfRobEntries) ? 1 : tail + 1;
				}
			} else if (data.startsWith("0000000")) {
				// Add
				int regA = Integer.parseInt(data.substring(7, 10), 2);
				int regB = Integer.parseInt(data.substring(10, 13), 2);
				int regC = Integer.parseInt(data.substring(13, 16), 2);
				int j = 1;
				for (j = 1; j <= doublePrecisionAddSubRs ; j++)
					if (reservationStations.get("addd" + j)[0]
							.equalsIgnoreCase("n"))
						break;

				if (j <= doublePrecisionAddSubRs
						&& !(head == tail && !reorderBuffer.get(head)[0]
								.contains("$"))) {
					issued[i]=true;
					String [] resEntry = reservationStations.get("addd" + j);
					
					resEntry[0] = "Y";
					resEntry[1] = "addd";
					if (registerStatus.get(regB) == 0) {
						resEntry[2] = registers
								.get(regB);
						resEntry[4] = "$$$$$$$$$$$$$$$$";
					}else{
						resEntry[2] = "$$$$$$$$$$$$$$$$";
						resEntry[4] = to16BinaryStringValue(registerStatus.get(regB));
					}
					if (registerStatus.get(regA) == 0) {
						resEntry[3] = registers
								.get(regA);
						resEntry[5] = "$$$$$$$$$$$$$$$$";
					}else{
						resEntry[3] = "$$$$$$$$$$$$$$$$";
						resEntry[5] = to16BinaryStringValue(registerStatus.get(regA));
					}
					resEntry[6] = to16BinaryStringValue(tail);
					resEntry[7] = "$$$$$$$$$$$$$$$$$$$$";
					resEntry[8] = to16BinaryStringValue(instBuffer[i]);
					String robEntry[] = { "addd", regA + "", "$$$$$$$$$$$$$$$$", "N" ,to16BinaryStringValue(instBuffer[i])};
					reorderBuffer.put(tail, robEntry);
					registerStatus.put(regA, tail);
					reservationStations.put("addd"+j, resEntry);
					// increment tail correctly
					tail = (tail == numberOfRobEntries) ? 1 : tail + 1;
				}

			} else if (data.startsWith("0000001")) {
				// SUB
				int regA = Integer.parseInt(data.substring(7, 10), 2);
				int regB = Integer.parseInt(data.substring(10, 13), 2);
				int regC = Integer.parseInt(data.substring(13, 16), 2);

				int j = 1;
				for (j = 1; j <= doublePrecisionAddSubRs ; j++)
					if (reservationStations.get("addd" + j)[0]
							.equalsIgnoreCase("n"))
						break;

				if (j <= doublePrecisionAddSubRs
						&& !(head == tail && !reorderBuffer.get(head)[0]
								.contains("$"))) {
					issued[i]=true;
					String [] resEntry = reservationStations.get("addd" + j);
					
					resEntry[0] = "Y";
					resEntry[1] = "subb";
					if (registerStatus.get(regB) == 0) {
						resEntry[2] = registers
								.get(regB);
						resEntry[4] = "$$$$$$$$$$$$$$$$";
					}else{
						resEntry[2] = "$$$$$$$$$$$$$$$$";
						resEntry[4] = to16BinaryStringValue(registerStatus.get(regB));
					}
					if (registerStatus.get(regA) == 0) {
						resEntry[3] = registers
								.get(regA);
						resEntry[5] = "$$$$$$$$$$$$$$$$";
					}else{
						resEntry[3] = "$$$$$$$$$$$$$$$$";
						resEntry[5] = to16BinaryStringValue(registerStatus.get(regA));
					}
					resEntry[6] = to16BinaryStringValue(tail);
					resEntry[7] = "$$$$$$$$$$$$$$$$$$$$";
					resEntry[8] = to16BinaryStringValue(instBuffer[i]);
					String robEntry[] = { "subb", regA + "", "$$$$$$$$$$$$$$$$", "N" ,to16BinaryStringValue(instBuffer[i])};
					reorderBuffer.put(tail, robEntry);
					registerStatus.put(regA, tail);
					reservationStations.put("addd"+j, resEntry);
					// increment tail correctly
					tail = (tail == numberOfRobEntries) ? 1 : tail + 1;
				}
			} else if (data.startsWith("0000010")) {
				// NAND
				int regA = Integer.parseInt(data.substring(7, 10), 2);
				int regB = Integer.parseInt(data.substring(10, 13), 2);
				int regC = Integer.parseInt(data.substring(13, 16), 2);

				int j = 1;
				for (j = 1; j <= doublePrecisionAddSubRs ; j++)
					if (reservationStations.get("addd" + j)[0]
							.equalsIgnoreCase("n"))
						break;

				if (j <= doublePrecisionAddSubRs
						&& !(head == tail && !reorderBuffer.get(head)[0]
								.contains("$"))) {
					issued[i]=true;
					String [] resEntry = reservationStations.get("addd" + j);
					
					resEntry[0] = "Y";
					resEntry[1] = "nand";
					if (registerStatus.get(regB) == 0) {
						resEntry[2] = registers
								.get(regB);
						resEntry[4] = "$$$$$$$$$$$$$$$$";
					}else{
						resEntry[2] = "$$$$$$$$$$$$$$$$";
						resEntry[4] = to16BinaryStringValue(registerStatus.get(regB));
					}
					if (registerStatus.get(regA) == 0) {
						resEntry[3] = registers
								.get(regA);
						resEntry[5] = "$$$$$$$$$$$$$$$$";
					}else{
						resEntry[3] = "$$$$$$$$$$$$$$$$";
						resEntry[5] = to16BinaryStringValue(registerStatus.get(regA));
					}
					resEntry[6] = to16BinaryStringValue(tail);
					resEntry[7] = "$$$$$$$$$$$$$$$$$$$$";
					resEntry[8] = to16BinaryStringValue(instBuffer[i]);
					String robEntry[] = { "nand", regA + "", "$$$$$$$$$$$$$$$$", "N" ,to16BinaryStringValue(instBuffer[i])};
					reorderBuffer.put(tail, robEntry);
					registerStatus.put(regA, tail);
					reservationStations.put("addd"+j, resEntry);
					// increment tail correctly
					tail = (tail == numberOfRobEntries) ? 1 : tail + 1;
				}
			} else if (data.startsWith("0000011")) {
				// MUL
				int regA = Integer.parseInt(data.substring(7, 10), 2);
				int regB = Integer.parseInt(data.substring(10, 13), 2);
				int regC = Integer.parseInt(data.substring(13, 16), 2);

				int j = 1;
				for (j = 1; j <= multDivRs ; j++)
					if (reservationStations.get("multd" + j)[0]
							.equalsIgnoreCase("n"))
						break;

				if (j <= multDivRs
						&& !(head == tail && !reorderBuffer.get(head)[0]
								.contains("$"))) {
					issued[i]=true;
					String [] resEntry = reservationStations.get("multd" + j);
					
					resEntry[0] = "Y";
					resEntry[1] = "multd";
					if (registerStatus.get(regB) == 0) {
						resEntry[2] = registers
								.get(regB);
						resEntry[4] = "$$$$$$$$$$$$$$$$";
					}else{
						resEntry[2] = "$$$$$$$$$$$$$$$$";
						resEntry[4] = to16BinaryStringValue(registerStatus.get(regB));
					}
					if (registerStatus.get(regA) == 0) {
						resEntry[3] = registers
								.get(regA);
						resEntry[5] = "$$$$$$$$$$$$$$$$";
					}else{
						resEntry[3] = "$$$$$$$$$$$$$$$$";
						resEntry[5] = to16BinaryStringValue(registerStatus.get(regA));
					}
					resEntry[6] = to16BinaryStringValue(tail);
					resEntry[7] = "$$$$$$$$$$$$$$$$$$$$";
					resEntry[8] = to16BinaryStringValue(instBuffer[i]);
					String robEntry[] = { "multd", regA + "", "$$$$$$$$$$$$$$$$", "N" ,to16BinaryStringValue(instBuffer[i])};
					reorderBuffer.put(tail, robEntry);
					registerStatus.put(regA, tail);
					reservationStations.put("multd"+j, resEntry);
					// increment tail correctly
					tail = (tail == numberOfRobEntries) ? 1 : tail + 1;
				}
			} else if (data.startsWith("001000")) {
				// JMP
				int regA = Integer.parseInt(data.substring(6, 9), 2);
				String immediateValue = data.substring(9, 16);

				int j = 1;
				for (j = 1; j <= integerAddSubRs ; j++)
					if (reservationStations.get("add" + j)[0]
							.equalsIgnoreCase("n"))
						break;

				if (j <= integerAddSubRs
						&& !(head == tail && !reorderBuffer.get(head)[0]
								.contains("$"))) {
					issued[i]=true;
					String [] resEntry = reservationStations.get("add" + j);
					
					resEntry[0] = "Y";
					resEntry[1] = "jmp";
					resEntry[2]="$$$$$$$$$$$$$$$$";
					resEntry[4]="$$$$$$$$$$$$$$$$";
					if (registerStatus.get(regA) == 0) {
						resEntry[3] = registers
								.get(regA);
						resEntry[5] = "$$$$$$$$$$$$$$$$";
					}else{
						resEntry[3] = "$$$$$$$$$$$$$$$$";
						resEntry[5] = to16BinaryStringValue(registerStatus.get(regA));
					}
					resEntry[6] = to16BinaryStringValue(tail);
					resEntry[7] = "$$$$$$$$$$$$$$$$$$$$";
					resEntry[8] = to16BinaryStringValue(instBuffer[i]);
					String robEntry[] = { "jmp", regA + "", "$$$$$$$$$$$$$$$$", "N" ,to16BinaryStringValue(instBuffer[i])};
					reorderBuffer.put(tail, robEntry);
					registerStatus.put(regA, tail);
					reservationStations.put("add"+j, resEntry);
					// increment tail correctly
					tail = (tail == numberOfRobEntries) ? 1 : tail + 1;
				}

			} else if (data.startsWith("0100000000")) {
				// JALR
				int regA = Integer.parseInt(data.substring(10, 13), 2);
				int regB = Integer.parseInt(data.substring(13, 16), 2);

				int j = 1;
				for (j = 1; j <= doublePrecisionAddSubRs ; j++)
					if (reservationStations.get("addd" + j)[0]
							.equalsIgnoreCase("n"))
						break;

				if (j <= doublePrecisionAddSubRs
						&& !(head == tail && !reorderBuffer.get(head)[0]
								.contains("$"))) {
					issued[i]=true;
					String [] resEntry = reservationStations.get("addd" + j);
					
					resEntry[0] = "Y";
					resEntry[1] = "jalr";
					if (registerStatus.get(regB) == 0) {
						resEntry[2] = registers
								.get(regB);
						resEntry[4] = "$$$$$$$$$$$$$$$$";
					}else{
						resEntry[2] = "$$$$$$$$$$$$$$$$";
						resEntry[4] = to16BinaryStringValue(registerStatus.get(regB));
					}
					if (registerStatus.get(regA) == 0) {
						resEntry[3] = registers
								.get(regA);
						resEntry[5] = "$$$$$$$$$$$$$$$$";
					}else{
						resEntry[3] = "$$$$$$$$$$$$$$$$";
						resEntry[5] = to16BinaryStringValue(registerStatus.get(regA));
					}
					resEntry[6] = to16BinaryStringValue(tail);
					resEntry[7] = "$$$$$$$$$$$$$$$$$$$$";
					resEntry[8] = to16BinaryStringValue(instBuffer[i]);
					String robEntry[] = { "jalr", regA + "", "$$$$$$$$$$$$$$$$", "N" ,to16BinaryStringValue(instBuffer[i])};
					reorderBuffer.put(tail, robEntry);
					registerStatus.put(regA, tail);
					reservationStations.put("addd"+j, resEntry);
					// increment tail correctly
					tail = (tail == numberOfRobEntries) ? 1 : tail + 1;
				}

			} else if (data.startsWith("0110000000000")) {
				// RET
				int regA = Integer.parseInt(data.substring(13, 16), 2);
				int j = 1;
				for (j = 1; j <= integerAddSubRs ; j++)
					if (reservationStations.get("addd" + j)[0]
							.equalsIgnoreCase("n"))
						break;

				if (j <= integerAddSubRs
						&& !(head == tail && !reorderBuffer.get(head)[0]
								.contains("$"))) {
					issued[i]=true;
					String [] resEntry = reservationStations.get("add" + j);
					
					resEntry[0] = "Y";
					resEntry[1] = "ret";
					resEntry[2]="$$$$$$$$$$$$$$$$";
					resEntry[4]="$$$$$$$$$$$$$$$$";
					if (registerStatus.get(regA) == 0) {
						resEntry[3] = registers
								.get(regA);
						resEntry[5] = "$$$$$$$$$$$$$$$$";
					}else{
						resEntry[3] = "$$$$$$$$$$$$$$$$";
						resEntry[5] = to16BinaryStringValue(registerStatus.get(regA));
					}
					resEntry[6] = to16BinaryStringValue(tail);
					resEntry[7] = "$$$$$$$$$$$$$$$$$$$$";
					resEntry[8] = to16BinaryStringValue(instBuffer[i]);
					String robEntry[] = { "ret", regA + "", "$$$$$$$$$$$$$$$$", "N" ,to16BinaryStringValue(instBuffer[i])};
					reorderBuffer.put(tail, robEntry);
					registerStatus.put(regA, tail);
					reservationStations.put("add"+j, resEntry);
					// increment tail correctly
					tail = (tail == numberOfRobEntries) ? 1 : tail + 1;
				}
			}
		}
		int index=0;
		int [] newInstBuffer = new int[numberOfWays];
		for(int i =0 ;i<numberOfWays;i++){
			if(!issued[i]){
				newInstBuffer[index]=instBuffer[i];
				index++;
			}else
				updateClockCycle(instBuffer[i], programCycles, 1);
		}
		instBuffer=newInstBuffer;
		fetch(index);
	}
}
