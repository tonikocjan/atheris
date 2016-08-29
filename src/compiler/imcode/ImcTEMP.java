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
 * Zacasna spremenljivka.
 * 
 * @author sliva
 */
public class ImcTEMP extends ImcExpr {

	/** Zacasna spremenljivka.  */
	public FrmTemp temp;

	/**
	 * Ustvari novo zacasno spremenljivko.
	 * 
	 * @param temp Zacasna spremenljivka.
	 */
	public ImcTEMP(FrmTemp temp) {
		this.temp = temp;
	}

	@Override
	public void dump(int indent) {
		Report.dump(indent, "TEMP name=" + temp.name());
	}

	@Override
	public ImcESEQ linear() {
		return new ImcESEQ(new ImcSEQ(), this);
	}

}
