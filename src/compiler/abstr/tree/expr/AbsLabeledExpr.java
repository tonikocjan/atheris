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

package compiler.abstr.tree.expr;

import compiler.Position;
import compiler.abstr.ASTVisitor;

public class AbsLabeledExpr extends AbsExpr {
	
	/**
	 * Expression's name.
	 */
	public final String name;
	
	/**
	 * Expression.
	 */
	public final AbsExpr expr;

	/**
	 * Create new labeled (named) expr.
	 * @param pos
	 * @param expr
	 * @param name
	 */
	public AbsLabeledExpr(Position pos, AbsExpr expr, String name) {
		super(pos);
		
		this.expr = expr;
		this.name = name;
	}

	@Override
	public void accept(ASTVisitor aSTVisitor) {
		aSTVisitor.visit(this);
	}

}
