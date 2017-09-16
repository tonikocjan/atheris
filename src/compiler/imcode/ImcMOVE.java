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

/**
 * Prenos.
 * 
 * @author sliva
 */
public class ImcMOVE extends ImcStmt {

	/** Ponor.  */
	public ImcExpr dst;

	/** Izvor.  */
	public ImcExpr src;

	/** Ustvari nov prenos.
	 * 
	 * @param dst Ponor.
	 * @param src Izvor.
	 */
	public ImcMOVE(ImcExpr dst, ImcExpr src) {
		this.dst = dst;
		this.src = src;
	}

	@Override
	public void dump(int indent) {
        logger.dump(indent, "MOVE");
		dst.dump(indent + 2);
		src.dump(indent + 2);
	}

	@Override
	public ImcSEQ linear() {
		ImcSEQ lin = new ImcSEQ();
		ImcESEQ dst = this.dst.linear();
		ImcESEQ src = this.src.linear();
		lin.stmts.addAll(((ImcSEQ)dst.stmt).stmts);
		lin.stmts.addAll(((ImcSEQ)src.stmt).stmts);
		lin.stmts.add(new ImcMOVE(dst.expr, src.expr));
		return lin;
	}

}
