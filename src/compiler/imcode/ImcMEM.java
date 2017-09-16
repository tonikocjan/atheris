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
 * Dostop do pomnilnika.
 * 
 * @author sliva
 */
public class ImcMEM extends ImcExpr {

	/** Opis dostopa do pomnilnika.  */
	public ImcExpr expr;

	/**
	 * Ustvari nov dostop do pomnilnika.
	 * 
	 * @param expr Naslov.
	 */
	public ImcMEM(ImcExpr expr) {
		this.expr = expr;
	}

	@Override
	public void dump(int indent) {
        logger.dump(indent, "MEM");
		expr.dump(indent + 2);
	}

	@Override
	public ImcESEQ linear() {
		ImcESEQ lin = expr.linear();
		lin.expr = new ImcMEM(lin.expr);
		return lin;
	}

}
