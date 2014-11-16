import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;


public class cache {
	
	HashMap<Integer, String[][]> cache; 
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
	int base_address;
	int number_of_lines;
	int number_of_sets;
	
	public cache(File configuration_file) throws NumberFormatException, IOException{
		this.configuration_file = configuration_file;	
		BufferedReader configuration_file_reader = new BufferedReader(new FileReader(this.configuration_file)); 
		s = Integer.parseInt(configuration_file_reader.readLine());
		l = Integer.parseInt(configuration_file_reader.readLine());
		m = Integer.parseInt(configuration_file_reader.readLine());
		write_policy = configuration_file_reader.readLine();
		number_of_cycles = Integer.parseInt(configuration_file_reader.readLine());
		memory_access_time = Integer.parseInt(configuration_file_reader.readLine()); // Heya di tefre2 eh 3an elly ablaha ?!
		configuration_file_reader.close();
		
		number_of_lines = (s * 1024) / l;
		offset_bits = (int) (Math.log(l) / Math.log(2));
		index_bits = (int) (Math.log(number_of_lines) / Math.log(2));
		tag_bits = 16 - (offset_bits + index_bits);
		number_of_sets = number_of_lines / m;
		
		for(int i = 0; i < number_of_sets ; i++){
			cache.put(i, new String[m][4]);
		}
	}
	
	public String read_cache(String address){
		return "";
		
	}
	
	public HashMap<String, String> split_address(String address){
		HashMap<String, String> splitted_address = new HashMap<String, String>();

		splitted_address.put("tag", address.substring(0, tag_bits));
		splitted_address.put("index", address.substring(tag_bits, tag_bits+index_bits));
		splitted_address.put("offset" , address.substring(tag_bits+index_bits, tag_bits+index_bits+offset_bits));
		
		return splitted_address;		
	}

}





