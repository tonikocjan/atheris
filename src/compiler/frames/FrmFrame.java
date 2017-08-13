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

import compiler.ast.tree.def.AbsFunDef;
import utils.Constants;

public class FrmFrame {

	public AbsFunDef functionDefinition;
	public int staticLevel;
	public FrmLabel entryLabel;
	public FrmLabel endLabel;
	public int parameterCount;
	public int parametersSize;
	LinkedList<FrmLocAccess> localVariables;
	public int blockSizeForLocalVariables;
	public int blockSizeForFPRA;
	public int blockSizeForTemporaryVariables;
	public int blockSizeForRegisters;
	public int sizeArgs;
	public FrmTemp FP;
	public FrmTemp RV;

	public FrmFrame(AbsFunDef fun, int level) {
		this.functionDefinition = fun;
		this.staticLevel = level;
		this.entryLabel = (level == 1 ? FrmLabel.newNamedLabel(fun.name) : FrmLabel.newAnonymousLabel());
		this.endLabel = FrmLabel.newAnonymousLabel();
		this.parameterCount = 0;
		this.parametersSize = Constants.Byte;
		this.localVariables = new LinkedList<>();
		this.blockSizeForLocalVariables = 0;
		this.blockSizeForFPRA = 8;
		this.blockSizeForTemporaryVariables = 0;
		this.blockSizeForRegisters = 0;
		this.sizeArgs = 0;

		FP = new FrmTemp();
		RV = new FrmTemp();
	}

	public int size() {
		return blockSizeForLocalVariables + blockSizeForFPRA + blockSizeForTemporaryVariables + blockSizeForRegisters + sizeArgs;
	}

	@Override
	public String toString() {
		return ("FRAME(" + functionDefinition.name + ": " +
					"staticLevel=" + staticLevel + "," +
					"entryLabel=" + entryLabel.getName() + "," +
					"blockSizeForLocalVariables=" + blockSizeForLocalVariables + "," +
					"sizeArgs=" + sizeArgs + "," +
					"sizeInBytes=" + size() + "," +
					"FP=" + FP.getName() + "," +
					"RV=" + RV.getName() + ")");
	}
	
}
