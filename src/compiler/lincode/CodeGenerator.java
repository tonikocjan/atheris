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

package compiler.lincode;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;

import compiler.frames.FrmFrame;
import compiler.frames.FrmLabel;
import compiler.imcode.*;
import compiler.interpreter.Interpreter;
import compiler.seman.type.ClassType;

public class CodeGenerator {
	
	private static HashMap<FrmLabel, ImcCodeChunk> dict = new HashMap<>();

	public static FrmFrame getFrameForLabel(FrmLabel label) {
		return dict.get(label).frame;
	}
	
	public static ImcCode getCodeForLabel(FrmLabel label) {
		return dict.get(label).lincode;
	}
	
	public static void insertCodeForLabel(FrmLabel label, ImcCodeChunk code) {
		dict.put(label, code);
	}

	public static ImcCodeChunk linearize(LinkedList<ImcChunk> chunks) {
		ImcCodeChunk mainFrame = null;
		for (ImcChunk chnk : chunks) {
			if (chnk instanceof ImcCodeChunk) {
				ImcCodeChunk fn = (ImcCodeChunk) chnk;
				fn.lincode = fn.imcode.linear();
				
				CodeGenerator.insertCodeForLabel(fn.frame.label, fn);
				
				Interpreter.locations.put(((ImcCodeChunk) chnk).frame.label, 
						Interpreter.heapPointer);
				Interpreter.heapPointer += 4;

				if (fn.frame.label.name().equals("__main__")) {
					mainFrame = fn;
				}
			}
			else {
				ImcDataChunk data = (ImcDataChunk) chnk;
				Interpreter.locations.put(data.label, Interpreter.heapPointer);

				if (data.data != null) {
                    Interpreter.stM(Interpreter.heapPointer, data.data);
                }
				else {
                    if (data instanceof ImcVirtualTableDataChunk) {
                        // load vtable
                        ImcVirtualTableDataChunk vtableChunk = (ImcVirtualTableDataChunk) data;
                        ClassType type = vtableChunk.classType;

                        int heap = Interpreter.heapPointer;
                        Interpreter.debug = true;
                        Interpreter.stM(heap, type.descriptor);
                        Interpreter.stM(heap + 4, 0);
                        heap += 8;

                        for (Iterator<Integer> it = type.getOffsets(); it.hasNext(); ) {
                            Integer offset = it.next();
                            System.out.println("offset " + offset);

                            Interpreter.stM(heap, offset);
                            heap += 4;
                        }
                        System.out.println("end");
                        Interpreter.debug = false;
                    }
                    else {
                        Interpreter.stM(Interpreter.heapPointer, 0);
                    }
                }
					
				Interpreter.heapPointer += data.size;
			}
		}
		return mainFrame;
	}
	
}
