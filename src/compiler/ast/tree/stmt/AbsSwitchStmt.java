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

import java.util.Vector;

import compiler.Position;
import compiler.ast.ASTVisitor;
import compiler.ast.tree.AbsStmts;
import compiler.ast.tree.expr.AbsExpr;

public class AbsSwitchStmt extends AbsConditionalStmt {
	
	/**
	 * Expression to be compared.
	 * Must be typed as Int, String (or Enum)
	 */
	public final AbsExpr subjectExpr;
	
	/**
	 * Cases.
	 */
	public final Vector<AbsCaseStmt> cases;
	
	/**
	 * Default body code.
	 */
	public final AbsStmts defaultBody;

	/**
	 * 
	 * @param pos
	 */
	public AbsSwitchStmt(Position pos, AbsExpr subjectExpr, Vector<AbsCaseStmt> cases, 
			AbsStmts defaultBody) {
		super(pos);

		this.subjectExpr = subjectExpr;
		this.cases = cases;
		this.defaultBody = defaultBody;
	}

	@Override public void accept(ASTVisitor aSTVisitor) { aSTVisitor.visit(this); }

}
