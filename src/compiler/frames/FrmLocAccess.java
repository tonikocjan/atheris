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

import compiler.abstr.tree.def.AbsVarDef;
import compiler.seman.*;
import compiler.seman.type.*;

/**
 * Dostop do lokalne spremenljivke.
 * 
 * @author sliva
 */
public class FrmLocAccess extends FrmAccess {

	/** Opis spremenljivke.  */
	public final AbsVarDef var;

	/** Klicni zapis funkcije, v kateri je spremenljivka deklarirana.  */
	public final FrmFrame frame;

	/** Odmik od FPja.  */
	public final int offset;

	/** Ustvari nov dostop do lokalne spremenljivke.
	 * 
	 * @param var Lokalna spremenljivka.
	 * @param frame Klicni zapis.
	 */
	public FrmLocAccess(AbsVarDef var, FrmFrame frame) {
		this.var = var;
		this.frame = frame;
		
		Type type = SymbDesc.getType(this.var).actualType();
		this.offset = 0 - frame.sizeLocs - type.size();
		frame.sizeLocs = frame.sizeLocs + type.size();
	}

	@Override
	public String toString() {
		return "LOC(" + var.name + ": offset=" + offset + ")";
	}
	
}
