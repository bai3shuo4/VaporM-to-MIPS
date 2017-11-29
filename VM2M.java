import cs132.util.*;
import cs132.vapor.ast.*;
import cs132.vapor.ast.VBuiltIn.Op;
import cs132.vapor.parser.VaporParser;

//import vapor.parser.jar;

import java.util.*;
import java.io.*;

public class VM2M extends VInstr.Visitor<Throwable>{

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
    		System.out.println(function.ident);
    	}


	
	}

	public void visit(VCall v) throws Throwable {
	
	}

	public void visit(VAssign v) throws Throwable{

	}

	public void visit(VBuiltIn v) throws Throwable{

	}

	public void visit(VMemWrite v) throws Throwable{

	}

	public void visit(VMemRead v) throws Throwable{

	}

	public void visit(VBranch v) throws Throwable{

	}

	public void visit(VGoto v) throws Throwable{ 

	}

	public void visit(VReturn v) throws Throwable{

	}
}