/**
 * Copyright 2016 Toni Kocjan
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package compiler.interpreter;

import java.io.PrintStream;
import java.util.*;

import compiler.frames.*;
import compiler.imcode.*;
import compiler.logger.LoggerFactory;
import compiler.logger.LoggerInterface;
import utils.Constants;

public class Interpreter {

    private static LoggerInterface logger = LoggerFactory.logger();

	public static boolean debug = false;
	public static boolean printMemory = false;
    public static boolean checkMemory = true;
    public static PrintStream interpreterOutput = System.out;
	public static int heapPointer = 4;
    public static int stackSize = 1000;
    private static int framePointer = stackSize;
    private static int stackPointer = stackSize;
    public static Memory memory; // TODO: - How to make sure that memory is initialized?

    public static int getFP() { return framePointer; }
	
	public static void printMemory() {
	    int address = 0;
		for (int i = 0; i < memory.memory.size(); i++, address += 1) {
		    if (address > stackSize + Constants.Byte)
                logger.error("Memory overflow");

		    Object value = memory.ldM(address);
		    if (value == null) {
		        i--;
		        continue;
            }
            System.out.println("Address [" + address + "]: " + value);
        }
	}
	public Interpreter(FrmFrame frame, ImcSEQ code) {
		if (debug) {
			System.out.println("[START OF " + frame.entryLabel.getName() + "]");
		}

		memory.stM(stackPointer - frame.blockSizeForLocalVariables - 4, framePointer);
		framePointer = stackPointer;
        memory.stT(frame.FP, framePointer);
		stackPointer = stackPointer - frame.size();

		if (stackPointer < 0) {
            logger.error("Error, stack overflow");
		}

		if (debug) {
			System.out.println("[FP=" + framePointer + "]");
			System.out.println("[SP=" + stackPointer + "]");
		}

		int pc = 0;
		while (pc < code.stmts.size()) {
			if (debug) System.out.println("pc=" + pc);
			ImcCode instruction = code.stmts.get(pc);
			Object result = execute(instruction);
			if (result instanceof FrmLabel) {
				for (pc = 0; pc < code.stmts.size(); pc++) {
					instruction = code.stmts.get(pc);
					if ((instruction instanceof ImcLABEL) && (((ImcLABEL) instruction).label.getName().equals(((FrmLabel) result).getName())))
						break;
				}
			}
			else
				pc++;

			if (printMemory) {
				System.out.println(frame.entryLabel.getName() + ": " + pc);
				printMemory();
				System.out.println();
			}
		}

		framePointer = (Integer) memory.ldM(framePointer - frame.blockSizeForLocalVariables - 4);
		stackPointer = stackPointer + frame.size();

		if (debug) {
			System.out.println("[FP=" + framePointer + "]");
			System.out.println("[SP=" + stackPointer + "]");
		}

		memory.stM(stackPointer, memory.ldT(frame.RV));

		if (debug) {
			System.out.println("[RV=" + memory.ldT(frame.RV) + "]");
		}

		if (debug) {
			System.out.println("[END OF " + frame.entryLabel.getName() + "]");
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
            case ImcBINOP.IS:
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
            logger.error("Internal error.");
			return null;
		}
		
		if (instruction instanceof ImcCALL) {
			ImcCALL instr = (ImcCALL) instruction;
			int offset = 0;

			memory.stM(stackPointer, execute(instr.args.getFirst()));
			
			offset += Constants.Byte;
			
			for (int i = 1; i < instr.args.size(); i++) {
				memory.stM(stackPointer + offset, execute(instr.args.get(i)));
				offset += Constants.Byte;
			}

			if (instr.label.getName().equals("_print")) {
				interpreterOutput.println(memory.ldM(stackPointer + Constants.Byte));
				return 0;
			}
			if (instr.label.getName().equals("_time")) {
				return (int) System.currentTimeMillis();
			}
			if (instr.label.getName().equals("_rand")) {
				return new Random().nextInt((Integer)memory.ldM(stackPointer + Constants.Byte));
			}
			if (instr.label.getName().equals("_mem")) {
			    return memory.ldM((Integer) memory.ldM(stackPointer + Constants.Byte));
            }

            Integer address = memory.labelToAddressMapping.get(instr.label);
            ImcCodeChunk function = (ImcCodeChunk) memory.ldM(address);
			new Interpreter(function.getFrame(), function.getLincode());
			return memory.ldM(stackPointer);
		}

        if (instruction instanceof ImcMethodCALL) {
            ImcMethodCALL instr = (ImcMethodCALL) instruction;
            int offset = 0;
            memory.stM(stackPointer, execute(instr.args.getFirst()));

            offset += Constants.Byte;

            for (int i = 1; i < instr.args.size(); i++) {
                memory.stM(stackPointer + offset, execute(instr.args.get(i)));
                offset += Constants.Byte;
            }

            FrmLabel label = (FrmLabel) memory.ldM((Integer) memory.ldT(instr.temp));

            Integer address = memory.labelToAddressMapping.get(label);
            ImcCodeChunk function = (ImcCodeChunk) memory.ldM(address);
            new Interpreter(function.getFrame(), function.getLincode());
            return memory.ldM(stackPointer);
        }
		
		if (instruction instanceof ImcCJUMP) {
			ImcCJUMP instr = (ImcCJUMP) instruction;
			Object cond = execute(instr.cond);

            if (((Integer) cond).intValue() != 0) {
                return instr.trueLabel;
            }
            else {
                return instr.falseLabel;
            }
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
			Integer address = (Integer) execute(instr.expr);
			if (address == 0)
                logger.error("Null pointer exception");
			return memory.ldM(address);
		}
		
		if (instruction instanceof ImcMALLOC) {
			ImcMALLOC malloc = (ImcMALLOC) instruction;
			int location = heapPointer;
			heapPointer += malloc.size;
            return location;
		}
		
		if (instruction instanceof ImcMOVE) {
            ImcMOVE instr = (ImcMOVE) instruction;

            if (instr.dst instanceof ImcTEMP) {
                FrmTemp temp = ((ImcTEMP) instr.dst).temp;
                Object srcValue = execute(instr.src);
                memory.stT(temp, srcValue);

                return srcValue;
            }
            if (instr.dst instanceof ImcMEM) {
                Object dstValue = execute(((ImcMEM) instr.dst).expr);
                Object srcValue = execute(instr.src);
                memory.stM((Integer) dstValue, srcValue);
                return srcValue;
            }
        }
		
		if (instruction instanceof ImcNAME) {
			ImcNAME instr = (ImcNAME) instruction;

			if (instr.label.getName().equals("FP")) return framePointer;
			if (instr.label.getName().equals("SP")) return stackPointer;

			return memory.labelToAddressMapping.get(instr.label);
		}
		
		if (instruction instanceof ImcTEMP) {
			ImcTEMP instr = (ImcTEMP) instruction;
			return memory.ldT(instr.temp);
		}
	
		return null;
	}
}
