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
 * Binarna operacija.
 * 
 * @author sliva
 */
public class ImcBINOP extends ImcExpr {
	public static final int OR = 0;
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
    public static final int IS = 16;

	/** Operator.  */
	public int op;

	/** Koda levega podizraza.  */
	public ImcExpr limc;

	/** Koda desnega podizraza.  */
	public ImcExpr rimc;

	/**
	 * Ustvari novo binarno operacijo.
	 * 
	 * @param op Operator.
	 * @param limc Levi podizraz.
	 * @param rimc Desni podizraz.
	 */
	public ImcBINOP(int op, ImcExpr limc, ImcExpr rimc) {
		this.op = op;
		this.limc = limc;
		this.rimc = rimc;
	}

	@Override
	public void dump(int indent) {
		String op = null;
		switch (this.op) {
            case ADD: op = "+" ; break;
            case SUB: op = "-" ; break;
            case MUL: op = "*" ; break;
            case DIV: op = "/" ; break;
            case EQU: op = "=="; break;
            case NEQ: op = "!="; break;
            case LTH: op = "<" ; break;
            case GTH: op = ">" ; break;
            case LEQ: op = "<="; break;
            case GEQ: op = ">="; break;
            case AND: op = "&" ; break;
            case OR : op = "|" ; break;
            case ASSIGN : op = "="; break;
            case IS: op = "IS"; break;
		}
		Report.dump(indent, "BINOP op=" + op);
		limc.dump(indent + 2);
		rimc.dump(indent + 2);
	}

	@Override
	public ImcESEQ linear() {
		ImcESEQ limc = this.limc.linear();
		ImcESEQ rimc = this.rimc.linear();
		ImcSEQ stmt = new ImcSEQ();
		stmt.stmts.addAll(((ImcSEQ)limc.stmt).stmts);
		stmt.stmts.addAll(((ImcSEQ)rimc.stmt).stmts);
		ImcESEQ lin = new ImcESEQ(stmt, new ImcBINOP(op, limc.expr, rimc.expr));
		return lin;
	}
}
