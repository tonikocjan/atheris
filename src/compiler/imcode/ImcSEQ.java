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

import java.util.*;

import compiler.*;

/**
 * Zaporedje stavkov.
 * 
 * @author sliva
 */
public class ImcSEQ extends ImcStmt {

	/* Stavki.  */
	public LinkedList<ImcStmt> stmts;

	/**
	 * Ustvari zaporedje stavkov.
	 */
	public ImcSEQ() {
		stmts = new LinkedList<>();
	}

	@Override
	public void dump(int indent) {
		Report.dump(indent, "SEQ");
		Iterator<ImcStmt> stmts = this.stmts.iterator();
		while (stmts.hasNext()) {
			ImcStmt stmt = stmts.next();
			stmt.dump(indent + 2);
		}
	}

	@Override
	public ImcSEQ linear() {
		ImcSEQ lin = new ImcSEQ();
		Iterator<ImcStmt> stmts = this.stmts.iterator();
		while (stmts.hasNext()) {
			ImcStmt stmt = stmts.next();
			ImcSEQ linStmt = stmt.linear();
			lin.stmts.addAll(linStmt.stmts);
		}
		return lin;
	}

}
