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

package compiler.abstr.tree;

import java.util.*;

import compiler.*;
import compiler.abstr.*;
import compiler.abstr.tree.def.AbsDef;
import compiler.abstr.tree.def.AbsFunDef;
import compiler.abstr.tree.def.AbsVarDef;

/**
 * List of definitions.
 * 
 * @author toni kocjan
 */
public class AbsDefs extends AbsTree {

	/** Definitions. */
	public final LinkedList<AbsDef> definitions;

	/**
	 * Create new definitions list.
	 * 
	 * @param pos
	 *            Position.
	 * @param defs
	 *            Definitions.
	 */
	public AbsDefs(Position pos, LinkedList<AbsDef> defs) {
		super(pos);
		this.definitions = defs;
	}

    /**
     *
     * @param pos
     */
    public AbsDefs(Position pos) {
        super(pos);
        this.definitions = new LinkedList<>();
    }

    /**
	 * Find definition for given name.
	 * @param name Name of the definition
	 * @return Definition if found, otherwise null
	 */
	// TODO: - O(n), should optimize??
	public AbsDef findDefinitionForName(String name) {
		for (AbsDef d : definitions) {
			if (d instanceof AbsVarDef && ((AbsVarDef) d).name.equals(name))
				return d;
			if (d instanceof AbsFunDef && ((AbsFunDef) d).name.equals(name))
				return d;
		}
		return null;
	}
	
	@Override public void accept(ASTVisitor aSTVisitor) { aSTVisitor.visit(this); }

}
