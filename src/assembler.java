import java.io.File;


public class assembler {
	String [] memory;
	int baseAddress;
	
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
	
	
}
