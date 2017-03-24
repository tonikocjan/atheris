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

package compiler.abstr.tree.def;

import compiler.*;
import compiler.abstr.*;
import compiler.abstr.tree.VisibilityKind;
import compiler.abstr.tree.type.AbsType;

/**
 * Variable definition.
 * 
 * @author sliva
 */
public class AbsVarDef extends AbsDef {

	/** Variable type. */
	public final AbsType type;

	/**
	 * Create new variable definition.
	 * 
	 * @param pos
	 *            Position.
	 * @param name
	 *            Name.
	 * @param type
	 *            Type.
	 */
	public AbsVarDef(Position pos, String name, AbsType type) {
		this(pos, name, type, true, VisibilityKind.Public);
	}
	
	/**
	 * Create new variable definition.
	 * 
	 * @param pos
	 *            Position.
	 * @param name
	 *            Name.
	 * @param type
	 *            Type.
	 * @param constant
	 * 			  Is this variable a constant.
	 */
	public AbsVarDef(Position pos, String name, AbsType type, boolean constant) {
		this(pos, name, type, constant, VisibilityKind.Public);
	}
	
	/**
	 * Create new variable definition.
	 * 
	 * @param pos
	 *            Position.
	 * @param name
	 *            Name.
	 * @param type
	 *            Type.
	 * @param isMutable
	 * 			  Is this variable mutable.
	 * @param visibilityKind
	 * 			  Visibility.
	 */
	public AbsVarDef(Position pos, String name, 
			AbsType type, boolean isMutable, VisibilityKind visibilityKind) {
		super(pos, name, isMutable, visibilityKind);
		
		this.type = type;
	}


	@Override public void accept(ASTVisitor aSTVisitor) { aSTVisitor.visit(this); }
}
