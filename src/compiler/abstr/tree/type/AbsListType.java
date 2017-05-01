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

package compiler.abstr.tree.type;

import compiler.*;
import compiler.abstr.*;

/**
 * Opis seznama.
 * 
 * @author sliva
 */
public class AbsListType extends AbsType {

	/** Dolzina seznama. */
	public final int count;

	/** Tip elementa seznama. */
	public final AbsType type;

	/**
	 * Ustvari nov opis tabele.
	 * 
	 * @param pos
	 *            Polozaj stavcne oblike tega drevesa.
	 * @param count
	 *            Dolzina seznama.
	 * @param type
	 *            Tip elementa tabele.
	 */
	public AbsListType(Position pos, int count, AbsType type) {
		super(pos);
		this.count = count;
		this.type = type;
	}

    /**
     *
     * @param aSTVisitor
     */
	@Override public void accept(ASTVisitor aSTVisitor) { aSTVisitor.visit(this); }

    /**
     *
     * @return
     */
    @Override
    public String getName() {
        return null;
    }
}
