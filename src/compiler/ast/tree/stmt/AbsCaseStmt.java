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

package compiler.ast.tree.stmt;

import java.util.ArrayList;
import java.util.LinkedList;

import compiler.Position;
import compiler.ast.ASTVisitor;
import compiler.ast.tree.AbsStmt;
import compiler.ast.tree.AbsStmts;
import compiler.ast.tree.expr.AbsExpr;

public class AbsCaseStmt extends AbsStmt {

	/**
	 * Expressions.
	 */
	public final ArrayList<AbsExpr> exprs;
	
	/**
	 * Body to be executed if expression matches parent's subjectExpr.
	 */
	public final AbsStmts body;
	
	/**
	 * 
	 * @param pos
	 */
	public AbsCaseStmt(Position pos, ArrayList<AbsExpr> exprs, AbsStmts body) {
		super(pos);
		
		this.exprs = exprs;
		this.body = body;
	}

	@Override public void accept(ASTVisitor aSTVisitor) { aSTVisitor.visit(this); }

}
