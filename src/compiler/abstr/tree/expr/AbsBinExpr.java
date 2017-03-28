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

import compiler.*;
import compiler.abstr.*;

/**
 * Binarni izraz.
 * 
 * @author sliva
 */
public class AbsBinExpr extends AbsExpr {

	public static final int IOR = 0;
	public static final int AND = 1;
	public static final int EQU = 2;
	public static final int NEQ = 3;
	public static final int LEQ = 4;
	public static final int GEQ = 5;
	public static final int LTH = 6;
	public static final int GTH = 7;
	public static final int ADD = 8;
	public static final int SUB = 9;
	public static final int MUL = 10;
	public static final int DIV = 11;
	public static final int MOD = 12;
	public static final int DOT = 13;
	public static final int ARR = 14;
	public static final int ASSIGN = 15;

	/** Operator. */
	public int oper;

	/** Prvi podizraz. */
	public AbsExpr expr1;

	/** Drugi podizraz. */
	public AbsExpr expr2;

	/**
	 * Ustvari nov binarni izraz.
	 * 
	 * @param pos
	 *            Polozaj stavcne oblike tega drevesa.
	 * @param oper
	 *            Operator.
	 * @param expr1
	 *            Prvi podizraz.
	 * @param expr2
	 *            Drugi podizraz.
	 */
	public AbsBinExpr(Position pos, int oper, AbsExpr expr1, AbsExpr expr2) {
		super(pos);
		this.oper = oper;
		this.expr1 = expr1;
		this.expr2 = expr2;
	}

	@Override public void accept(ASTVisitor aSTVisitor) { aSTVisitor.visit(this); }

}
