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

package compiler.frames;

import java.util.*;

import compiler.ast.tree.*;
import compiler.ast.tree.def.AstDefinition;
import compiler.seman.type.ClassType;

public class FrameDescription implements FrameDescriptionMap {

	private HashMap<AstDefinition, FrmFrame> frames = new HashMap<>();
    private HashMap <AstDefinition, FrmAccess> acceses = new HashMap<>();
    private HashMap<ClassType, FrmVirtualTableAccess> virtualTables = new HashMap<>();

	public void setFrame(AstDefinition fun, FrmFrame frame) {
		frames.put(fun, frame);
	}

	public FrmFrame getFrame(AstNode fun) {
		return frames.get(fun);
	}

	public void setAccess(AstDefinition var, FrmAccess access) {
		acceses.put(var, access);
	}

	public FrmAccess getAccess(AstDefinition var) {
		return acceses.get(var);
	}

    public void setVirtualTable(ClassType classType, FrmVirtualTableAccess virtualTable) {
        virtualTables.put(classType, virtualTable);
    }

    public FrmVirtualTableAccess getVirtualTable(ClassType classType) {
        return virtualTables.get(classType);
    }
}
