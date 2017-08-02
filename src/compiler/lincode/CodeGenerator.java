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

import compiler.Report;
import compiler.abstr.tree.def.AbsClassDef;
import compiler.abstr.tree.def.AbsDef;
import compiler.abstr.tree.def.AbsFunDef;
import compiler.frames.FrmDesc;
import compiler.frames.FrmFrame;
import compiler.frames.FrmLabel;
import compiler.frames.FrmVirtualTableAccess;
import compiler.imcode.*;
import compiler.interpreter.Interpreter;
import compiler.seman.type.CanType;
import compiler.seman.type.ClassType;

public class CodeGenerator {

    /**
     *
     * @param chunks
     * @return
     */
	public ImcCodeChunk linearize(LinkedList<ImcChunk> chunks) {
		ImcCodeChunk mainFrame = null;

		for (ImcChunk chnk : chunks) {
			if (chnk instanceof ImcCodeChunk) {
                storeFunction((ImcCodeChunk) chnk);

                if (chnk.name().equals("__main__")) {
                    mainFrame = (ImcCodeChunk) chnk;
                }
			}
			else {
				ImcDataChunk data = (ImcDataChunk) chnk;
				Interpreter.locations.put(data.label, Interpreter.heapPointer);

				if (data instanceof ImcVirtualTableDataChunk) {
				    storeVirtualTable((ImcVirtualTableDataChunk) data);
                }
                else {
                    storeVariable(data);
                }
			}
		}

		return mainFrame;
	}

	private void storeFunction(ImcCodeChunk fn) {
        fn.lincode = fn.imcode.linear();

        Interpreter.locations.put(fn.frame.label, Interpreter.heapPointer);
        Interpreter.stM(Interpreter.heapPointer, fn);

        Interpreter.heapPointer += 4;
    }

    private void storeVariable(ImcDataChunk data) {
        if (data.data == null) {
            Interpreter.stM(Interpreter.heapPointer, 0);
        }
        else {
            Interpreter.stM(Interpreter.heapPointer, data.data);
        }

        Interpreter.heapPointer += data.size;
    }

	private void storeVirtualTable(ImcVirtualTableDataChunk vtableChunk) {
        ClassType type = vtableChunk.classType;
        CanType baseClass = type.baseClass;

        int baseClassVirtualTablePointer = 0;
        if (baseClass != null) {
            FrmVirtualTableAccess baseVirtualTable = FrmDesc.getVirtualTable((ClassType) baseClass.childType);
            baseClassVirtualTablePointer = baseVirtualTable.location;
        }

        Interpreter.stM(Interpreter.heapPointer, type.descriptor);
        Interpreter.stM(Interpreter.heapPointer + 4, baseClassVirtualTablePointer);
        Interpreter.heapPointer += 8;

        for (Iterator<FrmLabel> it = generateVirtualTableForClass(type); it.hasNext(); ) {
            FrmLabel label = it.next();

            Interpreter.stM(Interpreter.heapPointer, label);
            Interpreter.heapPointer += 4;
        }
    }

    private Iterator<FrmLabel> generateVirtualTableForClass(ClassType classType) {
        return new Iterator<FrmLabel>() {

            Iterator<AbsFunDef> iter = classType.generateVirtualTable();

            @Override
            public boolean hasNext() {
                return iter.hasNext();
            }

            @Override
            public FrmLabel next() {
                return FrmDesc.getFrame(iter.next()).label;
            }
        };
    }
}
