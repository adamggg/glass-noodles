import java.io.File;
import java.util.HashMap;


public class assembler {
	String [] memory;
	int baseAddress;
	HashMap<Integer, Integer> addressesMapping;
	
	public assembler(File file) {
		this.memory = new String[4];
		this.memory[0] = "add";
		this.memory[1] = "sub";
		this.memory[2] = "and";
		
		this.baseAddress = 100;
	}
	
	public String[] getMemoryArray() {
		return this.memory;
	}
	
	public int getBaseAddress() {
		return this.baseAddress;
	}
	
	public HashMap<Integer, Integer> getAddressesMapping(){
		return this.addressesMapping;
	}
	
	
}
