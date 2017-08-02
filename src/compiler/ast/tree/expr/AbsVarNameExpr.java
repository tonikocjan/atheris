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

package compiler.ast.tree.expr;

import compiler.*;
import compiler.ast.*;

/**
 * Ime spremenljivke v izrazu.
 * 
 * @author sliva
 */
public class AbsVarNameExpr extends AbsExpr {
	
	/** Ime spremenljivke. */
	public final String name;

	/**
	 * Ustvari nov opis imena spremenljivke v izrazu.
	 * 
	 * @param pos
	 *            Polozaj stavcne oblike tega drevesa.
	 * @param name
	 *            Ime spremenljivke.
	 */
	public AbsVarNameExpr(Position pos, String name) {
		super(pos);
		this.name = name;
	}

	@Override public void accept(ASTVisitor aSTVisitor) { aSTVisitor.visit(this); }

}