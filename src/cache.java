import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;




public class cache {

	HashMap<Integer, String[][]> cache; 
	File configurationFile;
	int s;
	int l;
	int m;
	String writePolicy;
	int numberOfCycles;
	int memoryAccessTime;
	int tagBits;
	int indexBits;
	int offsetBits;
	int baseAddress;
	int numberOfLines;
	int numberOfSets;

	public cache(File configurationFile) throws NumberFormatException, IOException{
		this.configurationFile = configurationFile;	
		BufferedReader configurationFileReader = new BufferedReader(new FileReader(this.configurationFile)); 
		s = Integer.parseInt(configurationFileReader.readLine());
		l = Integer.parseInt(configurationFileReader.readLine());
		m = Integer.parseInt(configurationFileReader.readLine());
		writePolicy = configurationFileReader.readLine();
		numberOfCycles = Integer.parseInt(configurationFileReader.readLine());
		memoryAccessTime = Integer.parseInt(configurationFileReader.readLine()); // Heya di tefre2 eh 3an elly ablaha ?!
		configurationFileReader.close();

		numberOfLines = (s * 1024) / l;
		offsetBits = (int) (Math.log(l) / Math.log(2));
		indexBits = (int) (Math.log(numberOfLines / m) / Math.log(2));
		tagBits = 16 - (offsetBits + indexBits);
		numberOfSets = numberOfLines / m;

		for(int i = 0; i < numberOfSets ; i++){
			cache.put(i, new String[m][4]);
		}
	}

	public cache() {
		// TODO Auto-generated constructor stub
	}

	public String readCache(String address){

		int index = Integer.parseInt(splitAddress(address).get("index"), 2);		
		String[][] cacheSet = cache.get(index);

		for(int i = 0 ; i < cacheSet.length ; i++){
			if (cacheSet[i][2].equalsIgnoreCase(splitAddress(address).get("tag"))) {
				return cacheSet[i][3];
			}
		}

		String data = readMemory(address);
		writeCache(address, data);

		return data;
	}

	public HashMap<String, String> splitAddress(String address){
		HashMap<String, String> splittedAddress = new HashMap<String, String>();

		splittedAddress.put("tag", address.substring(0, tagBits));
		splittedAddress.put("index", address.substring(tagBits, tagBits + indexBits));
		splittedAddress.put("offset" , address.substring(tagBits + indexBits, tagBits + indexBits + offsetBits));

		return splittedAddress;		
	}
	
	public int getIndex() {
		return indexBits;
	}
	
	public int getOffset() {
		return offsetBits;
	}
	
	public int getTag() {
		return tagBits;
	}

	public String removeOffset(String address) {
		String tagIndex = address.substring(0,16 - offsetBits);

		for(int i = 0; i < offsetBits; i++){
			tagIndex+="0";
		}

		return tagIndex;

	}


	public boolean writeCache(String address , String data){
		HashMap<String, String> splittedAddress = splitAddress(address);
		String [][] cacheToBeWrittenToSet=cache.get(splittedAddress.get("index"));
		if(cacheToBeWrittenToSet.length < m){
			cacheToBeWrittenToSet[cacheToBeWrittenToSet.length][0]="1";
			cacheToBeWrittenToSet[cacheToBeWrittenToSet.length][1]="0";
			cacheToBeWrittenToSet[cacheToBeWrittenToSet.length][2]=splittedAddress.get("tag");
			cacheToBeWrittenToSet[cacheToBeWrittenToSet.length][3]=data;

		}else{
			replace(cacheToBeWrittenToSet,address,data);

		}
		return cacheToBeWrittenToSet.length-1 < m;
	}

	public boolean replace(String [][] cacheSetToBeWrittenTo,String address,String data){
		boolean dirty=false;
		int chosenToReplaceWith = (int) Math.random()%m;
		String [] dataTobeReplaced = cacheSetToBeWrittenTo[chosenToReplaceWith];
		if(writePolicy.equalsIgnoreCase("wt") || dataTobeReplaced[1].equals("1")){
			writeMemory(address,dataTobeReplaced);
			dirty=true;
		}
		cacheSetToBeWrittenTo[chosenToReplaceWith][0]="1";
		cacheSetToBeWrittenTo[chosenToReplaceWith][1]="0";
		cacheSetToBeWrittenTo[chosenToReplaceWith][2]=splitAddress(address).get("tag");
		cacheSetToBeWrittenTo[chosenToReplaceWith][3]=data;
		return dirty;
	}

}





