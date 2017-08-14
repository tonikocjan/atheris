package compiler.interpreter;

import compiler.Logger;
import compiler.frames.FrmLabel;
import compiler.frames.FrmTemp;

import java.util.HashMap;

import static compiler.interpreter.Interpreter.checkMemory;
import static compiler.interpreter.Interpreter.debug;

public class Memory {

    public final HashMap<Integer, Object> memory = new HashMap<>();
    public final HashMap<FrmLabel, Integer> labelToAddressMapping = new HashMap<>();
    public final HashMap<FrmTemp, Object> registers = new HashMap<>();

    public void stM(int address, Object value) {
        if (checkMemory && value == null)
            Logger.error("Storing null is illegal");
        if (debug)
            System.out.println(" [" + address + "] <= " + value);

        memory.put(address, value);
    }

    public Object ldM(int address) {
        Object value = memory.get(address);
        if (debug) System.out.println(" [" + address + "] => " + value);
        return value;
    }
    public void stT(FrmTemp temp, Object value) {
        if (checkMemory && value == null)
            Logger.error("Storing null is illegal");
        if (debug) System.out.println(" " + temp.getName() + " <= " + value);
        registers.put(temp, value);
    }

    public Object ldT(FrmTemp temp) {
        Object value = registers.get(temp);

        if (debug) System.out.println(" " + temp.getName() + " => " + value);
        return value;
    }
}
