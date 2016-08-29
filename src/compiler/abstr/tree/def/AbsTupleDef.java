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

package compiler.abstr.tree.def;

import java.util.LinkedList;

import compiler.Position;
import compiler.abstr.ASTVisitor;
import compiler.abstr.tree.AbsDefs;

/**
 * Tuple definition.
 * @author toni kocjan
 *
 */
public class AbsTupleDef extends AbsTypeDef {
	
	/** Definitions inside tuple */
	public final AbsDefs definitions;

	/**
	 * Create new tuple type definition
	 * @param pos position
	 * @param expressions member expressions
	 * @param names name of each member
	 */
	public AbsTupleDef(Position pos, LinkedList<AbsDef> defs) {
		super(pos, "");

		// TODO: position
		this.definitions = new AbsDefs(pos, defs);
	}

	@Override
	public void accept(ASTVisitor aSTVisitor) { aSTVisitor.visit(this); }

}
