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

package compiler.abstr.tree;

import java.util.LinkedList;

import compiler.Position;
import compiler.abstr.ASTVisitor;
import compiler.abstr.tree.def.AbsDef;

/**
 * List of statements.
 * @author toni kocjan
 *
 */
public class AbsStmts extends AbsTree {

	/** Statements. */
	public final LinkedList<AbsStmt> statements;
	
	/**
	 * Create new statements list.
	 * @param position
	 * 			Position.
	 * @param absStmts
	 * 			Statements.
	 */
	public AbsStmts(Position position, LinkedList<AbsStmt> absStmts) {
		super(position);
		
		this.statements = absStmts;
	}

	/**
	 * Find definition for given name.
	 * @param name Definition name
	 * @return Definition if found, otherwise null.
	 */
	public AbsDef findDefinitionForName(String name) {
		for (AbsStmt s : statements) {
			if (s instanceof AbsDef)
				if (((AbsDef) s).getName().equals(name))
					return (AbsDef) s;
		}
		return null;
	}
	
	@Override public void accept(ASTVisitor aSTVisitor) { aSTVisitor.visit(this); }

}
