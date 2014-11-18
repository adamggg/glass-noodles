import java.io.BufferedReader;
import java.io.File;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;


public class assembler {

	public assembler() {

	}

	public assembler(File f) throws IOException {
		
		BufferedReader b = new BufferedReader(new FileReader(f));
		String l = b.readLine();
		while(l != null){
			
			if(l.equalsIgnoreCase("DATA")){
				
				String data = b.readLine();
				while(data != "CODE"){
					
					
				}
				
			}
			
			
		} 
		

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
			int k = 7-bNum.length();
			if(num >=0){
				
				for(int i=1 ; i<=k ; i++){
					
					bNum = "0"+bNum;
				}
			}
			else{
				
				bNum = bNum.substring(25, 32);
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
	
	
	public static void main(String...args) {
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
		*/
		String y = "JALR R1,R2";
		String z = "RET R6";	
		String x = "BEQ R1,R2,-64";
		
		System.out.println(assembleInstruction(x));
		System.out.println(assembleInstruction(x).length());
		
		
		
	}
}
