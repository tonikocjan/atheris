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

package compiler.imcode;

import compiler.*;

/**
 * Izraz kot stavek.
 * 
 * @author sliva
 */
public class ImcEXP extends ImcStmt {

	/** Izraz.  */
	public ImcExpr expr;

	/**
	 * Ustvari izraz kot stavek.
	 * 
	 * @param expr
	 */
	public ImcEXP(ImcExpr expr) {
		this.expr = expr;
	}

	@Override
	public void dump(int indent) {
		Report.dump(indent, "EXP");
		expr.dump(indent + 2);
	}

	@Override
	public ImcSEQ linear() {
		ImcSEQ lin = new ImcSEQ();
		ImcESEQ linExpr = expr.linear();
		lin.stmts.addAll(((ImcSEQ)linExpr.stmt).stmts);
		lin.stmts.add(new ImcEXP(linExpr.expr));
		return lin;
	}

}

