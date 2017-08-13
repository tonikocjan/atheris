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
import compiler.frames.*;

/**
 * Pogojni skok.
 * 
 * @author sliva
 */
public class ImcCJUMP extends ImcStmt {

	/** Koda pogoja.  */
	public ImcExpr cond;

	/** Labela skoka, ce je pogoj izpolnjen.  */
	public FrmLabel trueLabel;

	/** Lanbela skoka, ce pogoj ni izpolnjen. */
	public FrmLabel falseLabel;

	/**
	 * Ustvari nov pogojni skok.
	 * 
	 * @param cond Pogoj.
	 * @param trueLabel Labela skoka, ce je pogoj izpolnjen.
	 * @param falseLabel Labela skoka, ce pogoj ni izpolnjen.
	 */
	public ImcCJUMP(ImcExpr cond, FrmLabel trueLabel, FrmLabel falseLabel) {
		this.cond = cond;
		this.trueLabel = trueLabel;
		this.falseLabel = falseLabel;
	}

	@Override
	public void dump(int indent) {
		Logger.dump(indent, "CJUMP labels=" + trueLabel.getName() + "," + falseLabel.getName());
		cond.dump(indent + 2);
	}

	@Override
	public ImcSEQ linear() {
		ImcSEQ lin = new ImcSEQ();
		ImcESEQ linCond = cond.linear();
		FrmLabel newFalseLabel = FrmLabel.newAnonymousLabel();
		lin.stmts.addAll(((ImcSEQ)linCond.stmt).stmts);
		lin.stmts.add(new ImcCJUMP(linCond.expr, trueLabel, newFalseLabel));
		lin.stmts.add(new ImcLABEL(newFalseLabel));
		lin.stmts.add(new ImcJUMP(falseLabel));
		return lin;
	}

}
