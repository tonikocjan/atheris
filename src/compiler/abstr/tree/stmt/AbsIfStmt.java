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

package compiler.abstr.tree.stmt;

import java.util.Vector;

import compiler.*;
import compiler.abstr.*;
import compiler.abstr.tree.AbsStmts;
import compiler.abstr.tree.Condition;

/**
 * Conditional expression.
 * 
 * @author sliva
 */
// TODO: this should be AbsStmt
public class AbsIfStmt extends AbsConditionalStmt {
	
	/**
	 * Vector holding all of the conditions for this if expression.
	 */
	public final Vector<Condition> conditions;
	
	/**
	 * Code to be executed when all of the conditions fail.
	 */
	public final AbsStmts elseBody;
		
	/**
	 * Create new conditional statement.
	 * 
	 * @param pos
	 *            Position of this node.
	 * @param conditions
	 *            If and else-if code.
	 * @param elseBody
	 *            Code to be executed when conditions fail.
	 */
	public AbsIfStmt(Position pos, Vector<Condition> conditions, AbsStmts elseBody) {
		super(pos);
		this.conditions = conditions;
		this.elseBody = elseBody;
	}

	@Override public void accept(ASTVisitor aSTVisitor) { aSTVisitor.visit(this); }

}
