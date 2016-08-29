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
 * Brezpogojni skok.
 * 
 * @author sliva
 */
public class ImcJUMP extends ImcStmt {

	/** Labela skoka.  */
	public FrmLabel label;

	/** Ustvari brezpogojni skok. */
	public ImcJUMP(FrmLabel label) {
		this.label = label;
	}

	@Override
	public void dump(int indent) {
		Report.dump(indent, "JUMP label=" + label.name());
	}

	@Override
	public ImcSEQ linear() {
		ImcSEQ lin = new ImcSEQ();
		lin.stmts.add(this);
		return lin;
	}

}
