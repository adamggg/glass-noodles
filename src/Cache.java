import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;

public class Cache {

	HashMap<Integer, String[][]> cache; 
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

	public Cache(int s, int l, int m, String writePolicy, int cacheAccessTime){
		this.s = s;
		this.l = l;
		this.m = m;
		this.writePolicy = writePolicy;
		this.cacheAccessTime = cacheAccessTime;
		
		numberOfLines = (s * 1024) / l;
		offsetBits = (int) (Math.log(l) / Math.log(2));
		indexBits = (int) (Math.log(numberOfLines / m) / Math.log(2));
		tagBits = 16 - (offsetBits + indexBits);
		numberOfSets = numberOfLines / m;
		numberOfHits = 0;
		numberOfMisses = 0;
		
		this.cache = new HashMap<Integer, String[][]>();


		for(int i = 0; i < numberOfSets ; i++){
			String[][] initialArray = new String [m][4];
			for(int k = 0; k < m ; k++){
				initialArray[k][0]="0";
			}
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
		String [][] cacheSetToBeWrittenTo=cache.get(Integer.parseInt(splittedAddress.get("index"),2));
		boolean writeInMemory=false;
		for(String [] entry : cacheSetToBeWrittenTo){
			if(entry[2]!=null && entry[2].equalsIgnoreCase(splitAddress(address).get("tag"))){
				entry[1]=(writePolicy.equalsIgnoreCase("wb")?"1":"0");
				entry[3]=data;
			}
		}
		int i=0;
		for( i = 0;i<cacheSetToBeWrittenTo.length;i++)
			if(cacheSetToBeWrittenTo[i][2]==null)
				break;
		if(i<m){
			cacheSetToBeWrittenTo[i][0]="1";
			cacheSetToBeWrittenTo[i][1]=(dirty)?"1":"0";
			cacheSetToBeWrittenTo[i][2]=splittedAddress.get("tag");
			cacheSetToBeWrittenTo[i][3]=data;

		}else{
			 writeInMemory=replace(cacheSetToBeWrittenTo,address,data,dirty);

		}
		return writeInMemory;
	}

	public boolean replace(String [][] cacheSetToBeWrittenTo,String address,String data,boolean dirty){
		boolean memoryAcess=false;
		int chosenToReplaceWith = (int) Math.random()%m;
		String [] dataTobeReplaced = cacheSetToBeWrittenTo[chosenToReplaceWith];
		if(dataTobeReplaced[1]!=null &&dataTobeReplaced[1].equals("1")){
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

	public double getMissPenalty(){
		return numberOfMisses*1.0/(numberOfMisses+numberOfHits)*1.0;
	}
	public double getHitRatio(){
		return numberOfHits*1.0/(numberOfMisses+numberOfHits)*1.0;
	}

}


