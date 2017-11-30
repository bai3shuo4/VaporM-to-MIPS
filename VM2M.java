import cs132.util.*;
import cs132.vapor.ast.*;
import cs132.vapor.ast.VBuiltIn.Op;
import cs132.vapor.parser.VaporParser;

//import vapor.parser.jar;

import java.util.*;
import java.io.*;

public class VM2M extends VInstr.Visitor<Throwable>{
	//static int local_count;
	//static int out_count;
	//static int in_count;
	static int local_out;

	public static void main(String [] args) throws Throwable{

		Op[] ops = {
		    Op.Add, Op.Sub, Op.MulS, Op.Eq, Op.Lt, Op.LtS,
		    Op.PrintIntS, Op.HeapAllocZ, Op.Error,
		};
		boolean allowLocals = false;
		String[] registers = {
		    "v0", "v1",
		    "a0", "a1", "a2", "a3",
		    "t0", "t1", "t2", "t3", "t4", "t5", "t6", "t7",
		    "s0", "s1", "s2", "s3", "s4", "s5", "s6", "s7",
		    "t8",
	  	};
		boolean allowStack = true;

		VaporProgram program = null;
		try {

		    program = VaporParser.run(new InputStreamReader(System.in), 1, 1,
		                              java.util.Arrays.asList(ops),
		                              allowLocals, registers, allowStack);
		}
		catch (Exception ex) {
		    ex.printStackTrace();
		    System.out.println("Error");
		}

		System.out.println(".data");
		for(VDataSegment class_name : program.dataSegments){
			System.out.println(class_name.ident + ":");
			
			for(VOperand.Static function_name : class_name.values){
				System.out.println("	" + function_name.toString().substring(1));
			}
			System.out.println();

    	}

    	System.out.println(".text");
    	System.out.println("jal Main");
    	System.out.println("li $v0 10");
    	System.out.println("syscall");

    	VM2M vm2m = new VM2M();
    	for(VFunction function : program.functions){
    		System.out.println(function.ident + ":");

    		int in = function.stack.in;
    		int out = function.stack.out;
    		int local = function.stack.local;

    		local_out = local * 4;

    		int reserve = (out + local)*4 + 8;

    		System.out.println("sw $fp -8($sp)");
    		System.out.println("move $fp $sp");
    		System.out.println("subu $sp $sp " + Integer.toString(reserve));
    		System.out.println("sw $ra -4($fp)");


    		LinkedList<String> label_instruction = new LinkedList<>();
            LinkedList<Integer> label_line = new LinkedList<>();

            for(VCodeLabel tmp : function.labels){
	            label_instruction.add(tmp.ident + ":");
	            label_line.add(tmp.sourcePos.line);
            }

    		for(VInstr instruction : function.body){
    			int current_line = instruction.sourcePos.line;
                while(!label_line.isEmpty() && current_line > label_line.getFirst()){
		            System.out.println(label_instruction.getFirst());
		            label_line.pop();
		            label_instruction.pop();
                }
    			instruction.accept(vm2m);
    		}

    		System.out.println("lw $ra -4($fp)");
    		System.out.println("lw $fp -8($fp)");
    		System.out.println("addu $sp $sp " + Integer.toString(reserve));
    		System.out.println("jr $ra");

    		System.out.println();

    	}

    	System.out.println();

    	System.out.println("_print:\n" +
    						"li $v0 1   # syscall: print integer\n" +
						 "syscall							   \n" +
						 "la $a0 _newline					   \n" +
						 "li $v0 4   # syscall: print string   \n" +
						 "syscall                              \n" +
						 "jr $ra                               \n" +
						 "\n" +
						 "_error:                                \n" +
						 "li $v0 4   # syscall: print string   \n" +
						 "syscall   						   \n" +
						 "li $v0 10  # syscall: exit           \n" +
						 "syscall							   \n" +
						 "\n" +
						 "_heapAlloc:                            \n" +
						 "li $v0 9   # syscall: sbrk           \n" +
						 "syscall                              \n" +
						 "jr $ra                               \n" +
						 "\n" +
						".data                                  \n" +
						".align 0                               \n" +
						"_newline: .asciiz \"\\n\"              \n" +
						"_str0: .asciiz \"null pointer\\n\"     \n" +
						"_str1: .asciiz \"array index out of bounds\\n\"");


	
	}

	public void visit(VCall v) throws Throwable {
		if(v.addr instanceof VAddr.Var){
			System.out.println("jalr " + v.addr.toString());
		}
		else{
			System.out.println("jal " + v.addr.toString().substring(1));
		}
	}

	public void visit(VAssign v) throws Throwable{
		if(v.source instanceof VVarRef.Register){
			System.out.println("move " + v.dest.toString() + " " + v.source.toString());
		}
		else if(v.source instanceof VLabelRef){
			System.out.println("la " + v.dest.toString() + " " + v.source.toString().substring(1));
		}
		else{
			System.out.println("li " + v.dest.toString() + " " + v.source.toString());
		}
	}

	public void visit(VBuiltIn v) throws Throwable{
		String op_name = v.op.name;

		if(op_name.equals("PrintIntS")){
			for(VOperand print : v.args){
				if(print instanceof VVarRef.Register){
					System.out.println("move $a0 " + print.toString());
				}
				else{
					System.out.println("li $a0 " + print.toString());
				}
				
			}
			System.out.println("jal _print");
			return;
			
		}

		if(op_name.equals("Error")){
			for(VOperand error : v.args){
				if((error.toString()).equals("\"null pointer\"")){
					System.out.println("la $a0 _str0");
				}
				else if((error.toString()).equals("\"array index out of bounds\"")){
					System.out.println("la $a0 _str1");
				}
			}
			System.out.println("j _error");
			return;
		}

		if(op_name.equals("HeapAllocZ")){
			for(VOperand heapalloc : v.args){
				if(heapalloc instanceof VVarRef.Register){
					System.out.println("move $a0 " + heapalloc.toString());
				}
				else{
					System.out.println("li $a0 " + heapalloc.toString());
				}
			}

			System.out.println("jal _heapAlloc");
			System.out.println("move " + v.dest.toString() + " $v0");
			return;
		}

		if(op_name.equals("MulS")){
			VOperand operation1 = (v.args)[0];
			VOperand operation2 = (v.args)[1];

			if(operation1 instanceof VLitInt && operation2 instanceof VLitInt){
				System.out.println("li " + v.dest.toString() + " " + Integer.toString(((VLitInt)operation1).value * ((VLitInt)operation2).value));
			}
			else {
				if(operation1 instanceof VLitInt){
					System.out.println("li $t9 " + operation1.toString());
					System.out.println("mul " + v.dest.toString() + " " + "$t9" + " " + operation2.toString());
				}
				else{
					System.out.println("mul " + v.dest.toString() + " " + operation1.toString() + " " + operation2.toString());
				}
			}
		}

		if(op_name.equals("Add")){
			VOperand operation1 = (v.args)[0];
			VOperand operation2 = (v.args)[1];

			if(operation1 instanceof VLitInt && operation2 instanceof VLitInt){
				System.out.println("li " + v.dest.toString() + " " + Integer.toString(((VLitInt)operation1).value + ((VLitInt)operation2).value));
			}
			else {
				if(operation1 instanceof VLitInt){
					System.out.println("li $t9 " + operation1.toString());
					System.out.println("addu " + v.dest.toString() + " " + "$t9" + " " + operation2.toString());
				}
				else{
					System.out.println("addu " + v.dest.toString() + " " + operation1.toString() + " " + operation2.toString());
				}
			}
		}

		if(op_name.equals("Sub")){
			VOperand operation1 = (v.args)[0];
			VOperand operation2 = (v.args)[1];

			if(operation1 instanceof VLitInt && operation2 instanceof VLitInt){
				System.out.println("li " + v.dest.toString() + " " + Integer.toString(((VLitInt)operation1).value - ((VLitInt)operation2).value));
			}
			else {
				if(operation1 instanceof VLitInt){
					System.out.println("li $t9 " + operation1.toString());
					System.out.println("subu " + v.dest.toString() + " " + "$t9" + " " + operation2.toString());
				}
				else{
					System.out.println("subu " + v.dest.toString() + " " + operation1.toString() + " " + operation2.toString());
				}
			}
		}

		if(op_name.equals("LtS")){
			VOperand operation1 = (v.args)[0];
			VOperand operation2 = (v.args)[1];

			
				if(operation1 instanceof VLitInt){
					System.out.println("li $t9 " + operation1.toString());
					System.out.println("slt " + v.dest.toString() + " " + "$t9" + " " + operation2.toString());
				}
				else if(operation2 instanceof VLitInt){
					System.out.println("li $t9 " + operation2.toString());
					System.out.println("slt " + v.dest.toString() + " " + operation1.toString()+ " " + "$t9");
				}
				else{
					System.out.println("slt " + v.dest.toString() + " " + operation1.toString() + " " + operation2.toString());
				}
			
		}

		if(op_name.equals("Lt")){
			VOperand operation1 = (v.args)[0];
			VOperand operation2 = (v.args)[1];

			
				if(operation1 instanceof VLitInt){
					System.out.println("li $t9 " + operation1.toString());
					System.out.println("sltu " + v.dest.toString() + " " + "$t9" + " " + operation2.toString());
				}
				else if(operation2 instanceof VLitInt){
					System.out.println("li $t9 " + operation2.toString());
					System.out.println("sltu " + v.dest.toString() + " " + operation1.toString()+ " " + "$t9");
				}
				else{
					System.out.println("sltu " + v.dest.toString() + " " + operation1.toString() + " " + operation2.toString());
				}
		}





	}

	public void visit(VMemWrite v) throws Throwable{
		if(v.dest instanceof VMemRef.Stack){
			VMemRef.Stack stack = (VMemRef.Stack)(v.dest);
			switch(stack.region){
				case In:					
					break;
				case Out:
					if(v.source instanceof VVarRef.Register){
						System.out.println("sw " + v.source.toString() + " " + Integer.toString(stack.index*4 + local_out) + "($sp)");
					}
					else if(v.source instanceof VLabelRef){
						System.out.println("la $t9 " + v.source.toString().substring(1));
						System.out.println("sw $t9 " + Integer.toString(stack.index*4 + local_out) + "($sp)");
					}
					else{
						System.out.println("li $t9 " + v.source.toString());
						System.out.println("sw $t9 " + Integer.toString(stack.index*4 + local_out) + "($sp)");
					}
					break;
				case Local:
				 	if(v.source instanceof VVarRef.Register){
						System.out.println("sw " + v.source.toString() + " " + Integer.toString(stack.index*4) + "($sp)");
					}
					else if(v.source instanceof VLabelRef){
						System.out.println("la $t9 " + v.source.toString().substring(1));
						System.out.println("sw $t9 " + Integer.toString(stack.index*4) + "($sp)");
					}
					else{
						System.out.println("li $t9 " + v.source.toString());
						System.out.println("sw $t9 " + Integer.toString(stack.index*4) + "($sp)");
					}
					break;
			}

		}

		else{
			VMemRef.Global global = (VMemRef.Global)(v.dest);
			if(v.source instanceof VVarRef.Register){
				System.out.println("sw " + v.source.toString() + " " + Integer.toString(global.byteOffset) + "(" + global.base.toString() + ")");
			}
			else if(v.source instanceof VLabelRef){
				System.out.println("la $t9 " + v.source.toString().substring(1));
				System.out.println("sw $t9 " + Integer.toString(global.byteOffset) + "(" + global.base.toString() + ")");
			}
			else{
				System.out.println("li $t9 " + v.source.toString());
				System.out.println("sw $t9 " + Integer.toString(global.byteOffset) + "(" + global.base.toString() + ")");
			}
		}
	}

	public void visit(VMemRead v) throws Throwable{
		if(v.source instanceof VMemRef.Stack){
			VMemRef.Stack stack = (VMemRef.Stack)(v.source);
			switch(stack.region){
				case In:
					if(v.dest instanceof VVarRef.Register){
						System.out.println("lw " + v.dest.toString() + " " + Integer.toString(stack.index*4) + "($fp)");
					}
					// else if(v.dest instanceof VLabelRef){
					// 	System.out.println("la $t9 " + v.dest.toString().substring(1));
					// 	System.out.println("lw $t9 " + Integer.toString(stack.index*4) + "($fp)");
					// }
					else{
						System.out.println("li $t9 " + v.dest.toString());
						System.out.println("lw $t9 " + Integer.toString(stack.index*4) + "($fp)");
					}					
					break;
				case Out:
					break;
				case Local:
				 	if(v.dest instanceof VVarRef.Register){
						System.out.println("lw " + v.dest.toString() + " " + Integer.toString(stack.index*4) + "($sp)");
					}
					// else if(v.dest instanceof VLabelRef){
					// 	System.out.println("la $t9 " + v.dest.toString().substring(1));
					// 	System.out.println("lw $t9 " + Integer.toString(stack.index*4) + "($sp)");
					// }
					else{
						System.out.println("li $t9 " + v.dest.toString());
						System.out.println("lw $t9 " + Integer.toString(stack.index*4) + "($sp)");
					}
					break;
			}
		}

		else{
					
			VMemRef.Global global = (VMemRef.Global)(v.source);
			if(v.dest instanceof VVarRef.Register){
				System.out.println("lw " + v.dest.toString() + " " + Integer.toString(global.byteOffset) + "(" + global.base.toString() + ")");
			}
			else{
				System.out.println("li $t9 " + v.dest.toString());
				System.out.println("lw $t9 " + Integer.toString(global.byteOffset) + "(" + global.base.toString() + ")");
			}
		
		}
	}

	public void visit(VBranch v) throws Throwable{
		boolean positive = v.positive;
		String target = v.target.toString().substring(1);

		if(positive){
			System.out.println("bnez " + v.value.toString() + " " + target);
		}
		else{
			System.out.println("beqz " + v.value.toString() + " " + target);
		}

	}

	public void visit(VGoto v) throws Throwable{ 
		System.out.println("j " + v.target.toString().substring(1));
	}

	public void visit(VReturn v) throws Throwable{

	}
}