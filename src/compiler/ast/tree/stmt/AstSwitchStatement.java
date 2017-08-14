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

import compiler.Position;
import compiler.ast.ASTVisitor;
import compiler.ast.tree.AstStatements;
import compiler.ast.tree.expr.AstExpression;

import java.util.List;

public class AstSwitchStatement extends AstConditionalStatement {

	public final AstExpression subjectExpr;
	public final List<AstCaseStatement> cases;
	public final AstStatements defaultBody;

	public AstSwitchStatement(Position pos, AstExpression subjectExpr, List<AstCaseStatement> cases, AstStatements defaultBody) {
		super(pos);

		this.subjectExpr = subjectExpr;
		this.cases = cases;
		this.defaultBody = defaultBody;
	}

	@Override public void accept(ASTVisitor aSTVisitor) { aSTVisitor.visit(this); }

}
