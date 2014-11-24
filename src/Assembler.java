import java.io.BufferedReader;
import java.io.File;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Scanner;


public class Assembler {
	
	String[] outArray;
	int startAddress;
	//HashMap<Integer, Integer> hashMemory;

	public Assembler() {

	}

	public Assembler(File f) throws IOException {
		
		BufferedReader b = new BufferedReader(new FileReader(f));
		String l = b.readLine();
		outArray = new String[65536];
		

		
		if(l.equalsIgnoreCase("CODE")){
			
			String a = b.readLine();
			a = a.substring(2, a.length());
			startAddress = Integer.parseInt(a , 16);
			int address = startAddress;
			String data = b.readLine();
			
			while(!data.equalsIgnoreCase("DATA")){
				
				String instr = assembleInstruction(data);
				String i1 = "";
				String i2 = "";
				int i;
				for(i = 0 ; i<=7 ; i++){
					
					i1+=instr.charAt(i);
				}
				//first byte of the instr
				outArray[address++] = i1;
				for(i=8 ; i<=15 ; i++){
					
					i2+=instr.charAt(i);
				}
				//second byte of the instr 
				outArray[address++] = i2;
				data = b.readLine();
				
			}
			
			if(data.equalsIgnoreCase("DATA")){
				
				
				int val;
				int valAddress;
				while(b.ready()){
					data = b.readLine();
					String[] d = data.split(",");
					String s = d[0];
					String s1 = d[1];
					
					if(s.length()>1 && s.charAt(1)=='x'){
						s = s.substring(2, s.length());
						val = Integer.parseInt(s , 16);
					}
					else{
						val = Integer.parseInt(s);
					}
				
					s1 = s1.substring(2, s1.length());
					valAddress = Integer.parseInt(s1 , 16);
					String binVal = signExtend(val, 16, 's');
					String Val1 = "";
					String Val2 = "";
					int i;
					//getting first byte of data
					for(i=0 ; i<=7 ; i++){
						
						Val1 += binVal.charAt(i);
					}
				
					
					//getting second byte of data 
					for(i=8 ; i<=15 ; i++){
						
						Val2 += binVal.charAt(i);
					}
					
					outArray[valAddress++] = Val1;
					outArray[valAddress] = Val2;
				
					/*
					//checking if specified address and the one after it are ready to hold 2 bytes data 
					if(outArray[valAddress]==null && outArray[valAddress+1]==null && (valAddress+1) < outArray.length){
						
						outArray[valAddress++] = Val1;
						outArray[valAddress] = Val2;
						
					}
					else{
						//checking for two empty consecutive cells in memory to hold 2 bytes data and save the new address
						boolean flag = false;
						for(int j = 0 ; j<outArray.length ; j++){
							
							if(outArray[j]==null && outArray[j+1]==null && (j+1) < outArray.length){
								hashMemory.put(valAddress , j);
								outArray[j++] = Val1;
								outArray[j] = Val2;
								flag = true;
								break;
							}
							
						}
						if(flag==false)
							System.out.println("memory is full");
					}
					*/
					
					
					
				}
			}	
			else{
				
				System.out.print("The file you entered is in the wrong format");
			}
			
		
			
			
			
		}
		else{
			
			System.out.print("The file you entered is in the wrong format");
		}
			
			
		b.close(); 
		

	}

	private static String assembleInstruction(String instruction) {
		String[] parsedInst = instruction.toUpperCase().split(" ");
		
		String inst = parsedInst[0];
		String s = parsedInst[1];
		String[] operands = s.toUpperCase().split(",");

	
		if (inst.equals("JMP") || inst.equals("JALR")) {
			
			String o1 = operands[0];
			String o2 = operands[1];
			int op1 = Integer.parseInt(o1.charAt(o1.length()-1)+"");
			
			String op1B = signExtend(op1 , 3 , 'r');
			if(inst.equals("JALR")){
				//2 operands
				int op2 = Integer.parseInt(o2.charAt(o2.length()-1)+"");
				String op2B = signExtend(op2 , 3 , 'r');
				
				//Adding inst opcode
				inst  = "010"+"0000000"+op1B+op2B;
				
			}
			else{
				//1 operand and imm val
				int op2 = Integer.parseInt(o2);
				String op2B = signExtend(op2 , 7 , 's');
				//JMP inst Adding opcode
				inst = "001"+"000"+op1B+op2B;
			}
			
			
							
		} else if (inst.equals("RET")) {
			
			//only 1 operand 
			String o1 = operands[0];
			int op = Integer.parseInt(o1.charAt(o1.length()-1)+"");
			String opB = signExtend(op , 3 , 'r');
			inst = "011"+"0000000000"+opB;
			

		} else {
			
			String o1 = operands[0];
			String o2 = operands[1];
			String o3 = operands[2];
			int op1 = Integer.parseInt(o1.charAt(o1.length()-1)+"");
			int op2 = Integer.parseInt(o2.charAt(o1.length()-1)+"");
			
			String op1B = signExtend(op1 , 3 , 'r');
			String op2B = signExtend(op2 , 3 , 'r');
			if(inst.equals("LW") || inst.equals("SW") || inst.equals("BEQ") || inst.equals("ADDI")){
				
				//1 operand 1 address and imm val
				int op3 = Integer.parseInt(o3);
				String op3B = signExtend(op3 , 7 , 's');
				switch(inst.charAt(0)){
					
					case 'L' : inst = "1"+"00"+op1B+op2B+op3B; break;
					case 'S' : inst = "1"+"01"+op1B+op2B+op3B; break;
					case 'B' : inst = "1"+"10"+op1B+op2B+op3B; break;
					case 'A' : inst = "1"+"11"+op1B+op2B+op3B; break;
					
				
				
				}
			}
			else{
				//3 operands
				int op3 = Integer.parseInt(o3.charAt(o1.length()-1)+"");
				String op3B = signExtend(op3 , 3 , 'r');
				switch(inst.charAt(0)){
					
					case 'A' : inst = "000"+"0000"+op1B+op2B+op3B; break;
					case 'S' : inst = "000"+"0001"+op1B+op2B+op3B; break;
					case 'N' : inst = "000"+"0010"+op1B+op2B+op3B; break;
					case 'M' : inst = "000"+"0011"+op1B+op2B+op3B; break;
					
				
				
				}
			}
			

		}
		return inst;

	}
	
	private static String signExtend(int num , int n , char c){
		
		String bNum = Integer.toBinaryString(num);
		if(c=='s'){
			int k = n-bNum.length();
			if(num >=0){
				
				for(int i=1 ; i<=k ; i++){
					
					bNum = "0"+bNum;
				}
			}
			else{
				
				bNum = bNum.substring((32-n), 32);
			}
		}
		else{
			int k = n-bNum.length();
			for(int i=1 ; i<=k ; i++){
				
				bNum = "0"+bNum;
			}
			
		}
		return bNum;
		
	}
	protected int getBaseAddress(){
		
		return startAddress;
	}
	protected String[] getMemoryArray(){
		
		return outArray;
	}
	/*protected HashMap getAddressesMapping(){
		
		return hashMemory;
	}*/
	
	
	public static void main(String...args) throws IOException {
		
		
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
				+"\n"+"**ANY TEXT FILE VIOLATING ONE OF THE ABOVE CONDITIONS WILL NOT BE ACCEPTED :)"+"\n"+"Enter Directory here : ");
		String f = sc.nextLine();
		File file = new File(f);
		Assembler a = new Assembler(file);
		
		/*String x = "reg1";
		
		
		System.out.println(Integer.parseInt(x.charAt(x.length()-1)+""));
		int y = -1;
		String z = Integer.toBinaryString(y);
		System.out.println(z.length());
		System.out.println(z);
		String bin = "110";
		System.out.println(extend(bin , 7 , 's'));
		String bin1 = "000";
		String bin2 = "111";
		System.out.println(bin2+bin1+bin);
		String f = "MUL";
		switch(f.charAt(0)){
		
		case 'A' : if(f.charAt(f.length()-1)=='I')System.out.println(f);else System.out.println("ADD");break; 
		case 'S' : 
		case 'B' : 
		
		
		
		}
		
		String y = "JALR R1,R2";
		String z = "RET R6";	
		String x = "BEQ R1,R2,-64";
		
		System.out.println(assembleInstruction(x));
		System.out.println(assembleInstruction(x).length());
		
		String h = "A";
		int x = Integer.parseInt(h , 16);
		System.out.println(x);
		
		int x = 6 ;
		System.out.println(Integer.toBinaryString(x));
		int x1 = -6 ;
		System.out.println(Integer.toBinaryString(x1));
		
		
		String a = "0110110000110011";
		String i3 = "";
		int i;
		for(i = 0; i<=7 ; i++){
			
			i3 += a.charAt(i);
		}
		String i4 = "";
		for(i =i; i<=15 ; i++){
		
			i4 += a.charAt(i);
		}
		System.out.println(i3 +','+i4);
		
		//testing the assembler with a giving file 
		File f = new File("file.txt");
		assembler a = new assembler(f);
		for(int i = 0 ; i<=120 ; i++){
			
			System.out.println(a.getMemoryArray()[i]);
		}
		System.out.println("hello"+a.getMemoryArray()[100]);
		System.out.println("hello"+a.getBaseAddress());
		
				String x = "0x0000000A";
		if(x.charAt(1)=='x'){
			x = x.substring(2, x.length());
			System.out.println(x);
			System.out.println(Integer.parseInt(x , 16));
		}
		else
			System.out.println("no");
	

		
		File x = new File("file3.txt");
		Assembler y = new Assembler(x);
		for(int i = 0 ; i<=120 ; i++){
		
			System.out.println(y.getMemoryArray()[i]);
			
		}	*/


		
	}
}
