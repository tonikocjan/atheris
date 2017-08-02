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

package compiler.ast.tree.expr;

import compiler.*;
import compiler.ast.*;

/**
 * Unarni izraz.
 * 
 * @author sliva
 */
public class AbsUnExpr extends AbsExpr {

	public static final int ADD = 0;
	public static final int SUB = 1;
	public static final int MEM = 2;
	public static final int VAL = 3;
	public static final int NOT = 4;

	/** Operator. */
	public final int oper;

	/** Podizraz. */
	public final AbsExpr expr;

	/**
	 * Ustvari nov binarni izraz.
	 * 
	 * @param pos
	 *            Polozaj stavcne oblike tega drevesa.
	 * @param oper
	 *            Operator.
	 * @param expr
	 *            Podizraz.
	 */
	public AbsUnExpr(Position pos, int oper, AbsExpr expr) {
		super(pos);
		this.oper = oper;
		this.expr = expr;
	}

	@Override public void accept(ASTVisitor aSTVisitor) { aSTVisitor.visit(this); }

}
