import java.io.File;
import java.util.HashMap;


public class cache {
	
	HashMap<Integer, int[]> cache; 
	File configuration_file;
	int s;
	int l;
	int m;
	String write_policy;
	int number_of_cycles;
	int memory_access_time;
	int tag_bits;
	int index_bits;
	int offset_bits;

	
	public cache(File configuration_file){
		this.configuration_file = configuration_file;	
		
	}

}
