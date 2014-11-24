import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;

public class Cache {

	HashMap<Integer, String[][]> cache; 
	File configurationFile;
	int s;
	int l;
	int m;
	String writePolicy;
	int cacheAccessTime;
	int tagBits;
	int indexBits;
	int offsetBits;
	int baseAddress;
	int numberOfLines;
	int numberOfSets;
	int numberOfHits;
	int numberOfMisses;
	
	public Cache() {
		// TODO Auto-generated constructor stub
	}

	public Cache(File configurationFile) throws NumberFormatException, IOException{
		this.configurationFile = configurationFile;	
		BufferedReader configurationFileReader = new BufferedReader(new FileReader(this.configurationFile)); 
		s = Integer.parseInt(configurationFileReader.readLine());
		l = Integer.parseInt(configurationFileReader.readLine());
		m = Integer.parseInt(configurationFileReader.readLine());
		writePolicy = configurationFileReader.readLine();
		cacheAccessTime = Integer.parseInt(configurationFileReader.readLine());
		configurationFileReader.close();

		numberOfLines = (s * 1024) / l;
		offsetBits = (int) (Math.log(l) / Math.log(2));
		indexBits = (int) (Math.log(numberOfLines / m) / Math.log(2));
		tagBits = 16 - (offsetBits + indexBits);
		numberOfSets = numberOfLines / m;
		numberOfHits = 0;
		numberOfMisses = 0;
		
		String[][] initialArray = new String [m][4];
		for(int i = 0; i < m ; i++){
			initialArray[i][0]="0";
		}

		for(int i = 0; i < numberOfSets ; i++){
			cache.put(i, initialArray);
		}
	}
		
	public int getCacheAccessTime(){
		return cacheAccessTime;
	}
	
	public int getNumberOfHits(){
		return numberOfHits;
	}
	
	public int getNumberOfMisses(){
		return numberOfMisses;
	}

	public String readCache(String address){

		int index = Integer.parseInt(splitAddress(address).get("index"), 2);		
		String[][] cacheSet = cache.get(index);

		for(int i = 0 ; i < cacheSet.length ; i++){
			if (cacheSet[i][0].equalsIgnoreCase("1") && cacheSet[i][2].equalsIgnoreCase(splitAddress(address).get("tag"))) {
				numberOfHits++;
				return cacheSet[i][3];
			}
		}
		
		numberOfMisses++;
		return null;
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


	public boolean writeCache(String address , String data, boolean dirty){
		HashMap<String, String> splittedAddress = splitAddress(address);
		String [][] cacheSetToBeWrittenTo=cache.get(splittedAddress.get("index"));
		boolean writeInMemory=false;
		for(String [] entry : cacheSetToBeWrittenTo){
			if(entry[2].equalsIgnoreCase(splitAddress(address).get("tag"))){
				entry[1]=(writePolicy.equalsIgnoreCase("wb")?"1":"0");
				entry[3]=data;
			}
		}
		if(cacheSetToBeWrittenTo.length < m){
			cacheSetToBeWrittenTo[cacheSetToBeWrittenTo.length][0]="1";
			cacheSetToBeWrittenTo[cacheSetToBeWrittenTo.length][1]=(dirty)?"1":"0";
			cacheSetToBeWrittenTo[cacheSetToBeWrittenTo.length][2]=splittedAddress.get("tag");
			cacheSetToBeWrittenTo[cacheSetToBeWrittenTo.length][3]=data;

		}else{
			
			 writeInMemory=replace(cacheSetToBeWrittenTo,address,data,dirty);

		}
		return writeInMemory;
	}

	public boolean replace(String [][] cacheSetToBeWrittenTo,String address,String data,boolean dirty){
		boolean memoryAcess=false;
		int chosenToReplaceWith = (int) Math.random()%m;
		String [] dataTobeReplaced = cacheSetToBeWrittenTo[chosenToReplaceWith];
		if(writePolicy.equalsIgnoreCase("wt") || dataTobeReplaced[1].equals("1")){
			memoryAcess=true;
		}
		cacheSetToBeWrittenTo[chosenToReplaceWith][0]="1";
		cacheSetToBeWrittenTo[chosenToReplaceWith][1]=(dirty)?"1":"0";
		cacheSetToBeWrittenTo[chosenToReplaceWith][2]=splitAddress(address).get("tag");
		cacheSetToBeWrittenTo[chosenToReplaceWith][3]=data;
		return memoryAcess;
	}
	public String trimData(String data){
		return data.substring(0, (int) (Math.pow(2, offsetBits)*8));
	}
}


