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

import compiler.*;
import compiler.ast.*;
import compiler.ast.tree.AbsStmts;
import compiler.ast.tree.expr.AbsExpr;
import compiler.ast.tree.expr.AbsVarNameExpr;

/**
 * Zanka z eksplicitnim stevcem.
 * 
 * @author sliva
 */
public class AbsForStmt extends AbsConditionalStmt {
	
//	/** Stevec. */
//	public final AbsVarName count;
	
	/** Vrednost v tabeli collection[count] */
	public final AbsVarNameExpr iterator;
	
	/** Zbirka preko katire iteriramo. */
	public final AbsExpr collection;
	
	/** Jedro stavka. */
	public final AbsStmts body;
	
	/**
	 * Ustvari zanko z eksplicitnim stevcem.
	 * 
	 * @param pos
	 *            Polozaj stavcne oblike tega drevesa.
	 * @param count
	 *            Stevec.
	 * @param collection
	 *            Zbirka.
	 * @param body
	 *            Jedro zanke.
	 */
	public AbsForStmt(Position pos, AbsVarNameExpr count, AbsExpr collection, AbsStmts body) {
		super(pos);
		this.iterator = count;
		this.collection = collection;
		this.body = body;
	}

	@Override public void accept(ASTVisitor aSTVisitor) { aSTVisitor.visit(this); }

}
