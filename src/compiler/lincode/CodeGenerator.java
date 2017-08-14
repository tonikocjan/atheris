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

import java.util.Iterator;
import java.util.List;

import compiler.ast.tree.def.AbsFunDef;
import compiler.frames.FrameDescriptionMap;
import compiler.frames.FrmLabel;
import compiler.frames.FrmVirtualTableAccess;
import compiler.imcode.*;
import compiler.interpreter.Interpreter;
import compiler.interpreter.Memory;
import compiler.seman.type.CanType;
import compiler.seman.type.ClassType;
import utils.Constants;

public class CodeGenerator {

    private FrameDescriptionMap frameDescription;
    private Memory memory;
    private ImcCodeChunk mainFrame;

    public CodeGenerator(FrameDescriptionMap frameDescription, Memory memory) {
        this.frameDescription = frameDescription;
        this.memory = memory;
    }

	public ImcCodeChunk linearize(List<ImcChunk> chunks) {
		for (ImcChunk chunk : chunks) {
			if (chunk instanceof ImcCodeChunk) {
                storeFunctionAndLinearizeCode((ImcCodeChunk) chunk);
                saveMainCodeChunk((ImcCodeChunk) chunk);
			}
			else {
				ImcDataChunk data = (ImcDataChunk) chunk;
				memory.labelToAddressMapping.put(data.label, Interpreter.heapPointer);

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

	private void saveMainCodeChunk(ImcCodeChunk chunk) {
        if (chunk.name().equals("__main__")) {
            mainFrame = (ImcCodeChunk) chunk;
        }
    }

	private void storeFunctionAndLinearizeCode(ImcCodeChunk fn) {
        linearizeCode(fn);
        storeFunction(fn);
    }

    private  void linearizeCode(ImcCodeChunk codeChunk) {
        codeChunk.linearize();
    }

    private void storeFunction(ImcCodeChunk fn) {
        memory.labelToAddressMapping.put(fn.getFrame().entryLabel, Interpreter.heapPointer);
        memory.stM(Interpreter.heapPointer, fn);

        Interpreter.heapPointer += Constants.Byte;
    }

    private void storeVariable(ImcDataChunk data) {
        if (data.data == null) {
            memory.stM(Interpreter.heapPointer, 0);
        }
        else {
            memory.stM(Interpreter.heapPointer, data.data);
        }

        Interpreter.heapPointer += data.size;
    }

	private void storeVirtualTable(ImcVirtualTableDataChunk vtableChunk) {
        ClassType type = vtableChunk.classType;
        CanType baseClass = type.baseClass;

        int baseClassVirtualTablePointer = 0;
        if (baseClass != null) {
            FrmVirtualTableAccess baseVirtualTable = frameDescription.getVirtualTable((ClassType) baseClass.childType);
            baseClassVirtualTablePointer = baseVirtualTable.location;
        }

        memory.stM(Interpreter.heapPointer, type.descriptor);
        Interpreter.heapPointer += Constants.Byte;
        memory.stM(Interpreter.heapPointer, baseClassVirtualTablePointer);
        Interpreter.heapPointer += Constants.Byte;

        Iterator<FrmLabel> virtualTableIterator = generateVirtualTableForClass(type);
        for (Iterator<FrmLabel> it = virtualTableIterator; it.hasNext(); ) {
            FrmLabel label = it.next();

            memory.stM(Interpreter.heapPointer, label);
            Interpreter.heapPointer += Constants.Byte;
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
                return frameDescription.getFrame(iter.next()).entryLabel;
            }
        };
    }
}
