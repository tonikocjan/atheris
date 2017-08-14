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

package compiler.ast.tree.def;

import compiler.*;
import compiler.ast.*;
import compiler.ast.tree.type.AbsType;

/**
 * Variable definition.
 * 
 * @author sliva
 */
public class AbsVarDef extends AbsDef {

	/** Variable memberType. */
	public final AbsType type;

    /**
     *
     * @param pos
     * @param name
     * @param type
     */
    public AbsVarDef(Position pos, String name, AbsType type) {
        this(pos, name, type, false);
    }

    /**
     *
     * @param pos
     * @param name
     * @param type
     * @param isMutable
     */
	public AbsVarDef(Position pos, String name, AbsType type, boolean isMutable) {
		super(pos, name, isMutable);
		
		this.type = type;
	}


	@Override public void accept(ASTVisitor aSTVisitor) { aSTVisitor.visit(this); }
}
