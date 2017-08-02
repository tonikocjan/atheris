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

import compiler.ast.tree.def.AbsParDef;

/**
 * Dostop do argumenta funkcije.
 * 
 * @author sliva
 */
public class FrmParAccess implements FrmAccess {

	/** Opis argumenta.  */
	public AbsParDef par;

	/** Klicni zapis funkcije, v kateri je parameter deklariran.  */
	public FrmFrame frame;

	/** Odmik od FPja.  */
	public int offset;


	/**
	 * Create new paramater access.
	 * @param par Parameter definition
	 * @param frame Frame
	 * @param offset Offset from FP
	 */
	public FrmParAccess(AbsParDef par, FrmFrame frame, int offset) {
		this.par = par;
		this.frame = frame;
		this.offset = offset;
	}

	@Override
	public String toString() {
		return "PAR(" + par.name + ": offset=" + offset + ")";
	}
	
}
