package compiler.interpreter;

import java.util.*;

import compiler.*;
import compiler.frames.*;
import compiler.imcode.*;
import compiler.lincode.CodeGenerator;

public class Interpreter {

	public static boolean debug = false;
	
	/*--- staticni del navideznega stroja ---*/
	
	/** Pomnilnik navideznega stroja. */
	public static HashMap<Integer, Object> mems = new HashMap<Integer, Object>();
	public static HashMap<FrmLabel, Integer> locations = new HashMap<>();
	
	/** Vrhnji naslov kopice */
	public static int offset = 4;
	
	public static void stM(Integer address, Object value) {
		if (debug) System.out.println(" [" + address + "] <= " + value);
		mems.put(address, value);
	}

	public static Object ldM(Integer address) {
		Object value = mems.get(address);
		if (debug) System.out.println(" [" + address + "] => " + value);
		return value;
	}
	
	public static int getFP() { return fp; }
	
	/** Velikost sklada */
	public  static int STACK_SIZE = 1000;
	
	/** Kazalec na vrh klicnega zapisa. */
	private static int fp = STACK_SIZE;

	/** Kazalec na dno klicnega zapisa. */
	private static int sp = STACK_SIZE;
	
	/*--- dinamicni del navideznega stroja ---*/
	
	/** Zacasne spremenljivke (`registri') navideznega stroja. */
	public HashMap<FrmTemp, Object> temps = new HashMap<FrmTemp, Object>();
		
	public void stT(FrmTemp temp, Object value) {
		if (debug) System.out.println(" " + temp.name() + " <= " + value);
		temps.put(temp, value);
	}

	public Object ldT(FrmTemp temp) {
		Object value = temps.get(temp);
		if (debug) System.out.println(" " + temp.name() + " => " + value);
		return value;
	}
	
	/**
	 * Debug print memory
	 */
	private void printMemory() {
		for (Map.Entry<FrmLabel, Integer> entry : locations.entrySet()) {
			System.out.println("Label: " + entry.getKey() + 
								", Adress: " + entry.getValue() + 
								", Value: " + mems.get(entry.getValue()));
			if (entry.getKey().name().equals("L8")) {
				System.out.println("Label: " + entry.getKey() + 
						", Adress: " + (entry.getValue() + 4) + 
						", Value: " + mems.get(entry.getValue() + 4));
			}
		}
	}
	
	/*--- Izvajanje navideznega stroja. ---*/
	
	public Interpreter(FrmFrame frame, ImcSEQ code) {
		if (debug) {
			System.out.println("[START OF " + frame.label.name() + "]");
		}
	
		stM(sp - frame.sizeLocs - 4, fp);
		fp = sp;
		stT(frame.FP, fp);
		sp = sp - frame.size();
		
		if (sp < 0) {
			Report.error("Error, stack overflow");
		}
		
		if (debug) {
			System.out.println("[FP=" + fp + "]");
			System.out.println("[SP=" + sp + "]");
		}

		int pc = 0;
		while (pc < code.stmts.size()) {
			if (debug) System.out.println("pc=" + pc);
			ImcCode instruction = code.stmts.get(pc);
			Object result = execute(instruction);
			if (result instanceof FrmLabel) {
				for (pc = 0; pc < code.stmts.size(); pc++) {
					instruction = code.stmts.get(pc);
					if ((instruction instanceof ImcLABEL) && (((ImcLABEL) instruction).label.name().equals(((FrmLabel) result).name())))
						break;
				}
			}
			else
				pc++;
//			System.out.println(pc);
//			printMemory();
//			System.out.println();
		}
		
		fp = (Integer) ldM(fp - frame.sizeLocs - 4);
		sp = sp + frame.size();
		if (debug) {
			System.out.println("[FP=" + fp + "]");
			System.out.println("[SP=" + sp + "]");
		}
		
		stM(sp, ldT(frame.RV));
		if (debug) {
			System.out.println("[RV=" + ldT(frame.RV) + "]");
		}

		if (debug) {
			System.out.println("[END OF " + frame.label.name() + "]");
		}
	}
	
	public Object execute(ImcCode instruction) {
		if (instruction instanceof ImcBINOP) {
			ImcBINOP instr = (ImcBINOP) instruction;
			Object fstSubValue = execute(instr.limc);
			Object sndSubValue = execute(instr.rimc);
			switch (instr.op) {
			case ImcBINOP.OR:
				return ((((Integer) fstSubValue).intValue() != 0) || (((Integer) sndSubValue).intValue() != 0) ? 1 : 0);
			case ImcBINOP.AND:
				return ((((Integer) fstSubValue).intValue() != 0) && (((Integer) sndSubValue).intValue() != 0) ? 1 : 0);
			case ImcBINOP.EQU:
				return (((Integer) fstSubValue).intValue() == ((Integer) sndSubValue).intValue() ? 1 : 0);
			case ImcBINOP.NEQ:
				return (((Integer) fstSubValue).intValue() != ((Integer) sndSubValue).intValue() ? 1 : 0);
			case ImcBINOP.LTH:
				return (((Integer) fstSubValue).intValue() < ((Integer) sndSubValue).intValue() ? 1 : 0);
			case ImcBINOP.GTH:
				return (((Integer) fstSubValue).intValue() > ((Integer) sndSubValue).intValue() ? 1 : 0);
			case ImcBINOP.LEQ:
				return (((Integer) fstSubValue).intValue() <= ((Integer) sndSubValue).intValue() ? 1 : 0);
			case ImcBINOP.GEQ:
				return (((Integer) fstSubValue).intValue() >= ((Integer) sndSubValue).intValue() ? 1 : 0);
			case ImcBINOP.ADD:
				return (((Integer) fstSubValue).intValue() + ((Integer) sndSubValue).intValue());
			case ImcBINOP.SUB:
				return (((Integer) fstSubValue).intValue() - ((Integer) sndSubValue).intValue());
			case ImcBINOP.MUL:
				return (((Integer) fstSubValue).intValue() * ((Integer) sndSubValue).intValue());
			case ImcBINOP.DIV:
				return (((Integer) fstSubValue).intValue() / ((Integer) sndSubValue).intValue());
			case ImcBINOP.MOD:
				return (((Integer) fstSubValue).intValue() % ((Integer) sndSubValue).intValue());
			}
			Report.error("Internal error.");
			return null;
		}
		
		if (instruction instanceof ImcCALL) {
			ImcCALL instr = (ImcCALL) instruction;
			int offset = 0;
			stM(sp + offset, execute(instr.args.getFirst()));
			
			offset += 4;
			
			for (int i = 1; i < instr.args.size(); i++) {
				stM(sp + offset, execute(instr.args.get(i)));
				offset += 4;
			}
			if (instr.label.name().equals("_print")) {
				System.out.println(ldM(sp + 4));
				return null;
			}
			if (instr.label.name().equals("_getInt")) {
				Scanner scanner = new Scanner(System.in);
				stM((Integer) ldM (sp + 4),scanner.nextInt());
				scanner.close();
				return null;
			}
			if (instr.label.name().equals("_putString")) {
				System.out.println((String) ldM(sp + 4));
				return null;
			}
			if (instr.label.name().equals("_getString")) {
				Scanner scanner = new Scanner(System.in);
				stM((Integer) ldM (sp + 4),scanner.next());
				scanner.close();
				return null;
			}
			
			new Interpreter(CodeGenerator.framesByFrmLabel(instr.label), (ImcSEQ) CodeGenerator.codesByFrmLabel(instr.label));
			return ldM(sp);
		}
		
		if (instruction instanceof ImcCJUMP) {
			ImcCJUMP instr = (ImcCJUMP) instruction;
			Object cond = execute(instr.cond);
			if (cond instanceof Integer) {
				if (((Integer) cond).intValue() != 0)
					return instr.trueLabel;
				else
					return instr.falseLabel;
			}
			else Report.error("CJUMP: illegal condition type.");
		}
		
		if (instruction instanceof ImcCONST) {
			ImcCONST instr = (ImcCONST) instruction;
			return instr.value;
		}
		
		if (instruction instanceof ImcJUMP) {
			ImcJUMP instr = (ImcJUMP) instruction;
			return instr.label;
		}
		
		if (instruction instanceof ImcLABEL) {
			return null;
		}
		
		if (instruction instanceof ImcEXP) {
			execute(((ImcEXP) instruction).expr);
		}
		
		if (instruction instanceof ImcMEM) {
			ImcMEM instr = (ImcMEM) instruction;
			Integer adress = (Integer) execute(instr.expr);
			if (adress == 0)
				Report.error("Nil pointer exception");
			return ldM(adress);
		}
		
		if (instruction instanceof ImcMALLOC) {
			ImcMALLOC malloc = (ImcMALLOC) instruction;
			ImcDataChunk data = new ImcDataChunk(malloc.chunkLabel, malloc.size);
			Interpreter.locations.put(data.label, offset);
			Interpreter.stM(offset, data.data);
		}
		
		if (instruction instanceof ImcMOVE) {
			ImcMOVE instr = (ImcMOVE) instruction;
			if (instr.dst instanceof ImcTEMP) {
				FrmTemp temp = ((ImcTEMP) instr.dst).temp;
				Object srcValue = execute(instr.src);
				stT(temp, srcValue);
				
				return srcValue;
			}
			if (instr.dst instanceof ImcMEM) {
				Object dstValue = execute(((ImcMEM) instr.dst).expr);
				Object srcValue = execute(instr.src);;
				stM((Integer) dstValue, srcValue);
				return srcValue;
			}
		}
		
		if (instruction instanceof ImcNAME) {
			ImcNAME instr = (ImcNAME) instruction;
			if (instr.label.name().equals("FP")) return fp;
			if (instr.label.name().equals("SP")) return sp;

			return locations.get(instr.label);
		}
		
		if (instruction instanceof ImcTEMP) {
			ImcTEMP instr = (ImcTEMP) instruction;
			return ldT(instr.temp);
		}
	
		return null;
	}
	
}
