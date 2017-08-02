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

import compiler.ast.tree.def.AbsVarDef;

/**
 * Dostop do globalne spremenljivke.
 * 
 * @author sliva
 */
public class FrmVarAccess implements FrmAccess {

	/** Opis spremenljivke.  */
	public final AbsVarDef var;

	/** Labela spremenljivke.  */
	public final FrmLabel label;

	/**
	 * Ustvari nov dostop do globalne spremenljivke.
	 * 
	 * @param var Globalna spremenljivka.
	 */
	public FrmVarAccess(AbsVarDef var) {
		this.var = var;
		label = FrmLabel.newLabel(var.name);
	}

	@Override
	public String toString() {
		return "VAR(" + var.name + ": label=" + label.name() + ")";
	}
}
