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

package compiler.abstr.tree.type;

import compiler.*;
import compiler.abstr.*;
import compiler.abstr.tree.AtomTypeKind;

/**
 * Opis atomarnega tipa.
 * 
 * @author sliva
 */
public class AbsAtomType extends AbsType {
	
	/** Tip. */
	public final AtomTypeKind type;

	/**
	 * Ustvari opis konkretnega tipa.
	 * 
	 * @param pos
	 *            Polozaj stavcne oblike tega drevesa.
	 * @param type
	 *            Konkretni tip.
	 */
	public AbsAtomType(Position pos, AtomTypeKind type) {
		super(pos);
		this.type = type;
	}
	
	public String toString() {
		switch (type) {
		case INT:
			return "Int";
		case DOB:
			return "Double";
		case CHR:
			return "Char";
		case VOID:
			return "Void";
		case STR:
			return "String";
		case NIL:
			Report.error("Internal error @ AtomType toString");
		default:
			return null;
		}
	}
	
	@Override public void accept(ASTVisitor aSTVisitor) { aSTVisitor.visit(this); }

}
