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
 * Stavki v izrazu.
 * 
 * @author sliva
 */
public class ImcESEQ extends ImcExpr {

	/** Stavki.  */
	public ImcStmt stmt;

	/** Vrednost.  */
	public ImcExpr expr;

	/**
	 * Ustvari stavke v izrazu.
	 * 
	 * @param stmt Stavki.
	 * @param expr Izraz.
	 */
	public ImcESEQ(ImcStmt stmt, ImcExpr expr) {
		this.stmt = stmt;
		this.expr = expr;
	}

	@Override
	public void dump(int indent) {
		Report.dump(indent, "ESEQ");
		stmt.dump(indent + 2);
		expr.dump(indent + 2);
	}

	@Override
	public ImcESEQ linear() {
		ImcSEQ linStmt = stmt.linear();
		ImcESEQ linExpr = expr.linear();
		linStmt.stmts.addAll(((ImcSEQ)linExpr.stmt).stmts);
		linExpr.stmt = linStmt;
		return linExpr;
	}
}
