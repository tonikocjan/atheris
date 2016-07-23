package compiler.lincode;

import java.util.HashMap;
import java.util.LinkedList;

import compiler.frames.FrmFrame;
import compiler.frames.FrmLabel;
import compiler.imcode.ImcChunk;
import compiler.imcode.ImcCode;
import compiler.imcode.ImcCodeChunk;
import compiler.imcode.ImcDataChunk;
import compiler.interpreter.Interpreter;

public class CodeGenerator {
	
	private static HashMap<FrmLabel, ImcCodeChunk> dict = new HashMap<>();

	public static FrmFrame framesByFrmLabel(FrmLabel label) {
		return dict.get(label).frame;
	}
	
	public static ImcCode codesByFrmLabel(FrmLabel label) {
		return dict.get(label).lincode;
	}
	
	public static void insertCode(FrmLabel label, ImcCodeChunk code) {
		dict.put(label, code);
	}

	public static ImcCodeChunk linearize(LinkedList<ImcChunk> chunks) {
		ImcCodeChunk mainFrame = null;
		for (ImcChunk chnk : chunks) {
			if (chnk instanceof ImcCodeChunk) {
				ImcCodeChunk fn = (ImcCodeChunk) chnk;
				fn.lincode = fn.imcode.linear();
				if (fn.frame.label.name().equals("__main__")) {
					mainFrame = fn;
					
				}
				CodeGenerator.insertCode(fn.frame.label, fn);
				Interpreter.locations.put(((ImcCodeChunk) chnk).frame.label, 
						Interpreter.heapPointer);
				Interpreter.stM(Interpreter.heapPointer, ((ImcCodeChunk) chnk).frame.label);
				
				Interpreter.heapPointer += 4;
			}
			else {
				ImcDataChunk data = (ImcDataChunk) chnk;
				Interpreter.locations.put(data.label, Interpreter.heapPointer);
				if (data.data != null)
					Interpreter.stM(Interpreter.heapPointer, data.data);
				else
					Interpreter.stM(Interpreter.heapPointer, 0);
					
				Interpreter.heapPointer += data.size;
			}
		}
		return mainFrame;
	}
	
}
