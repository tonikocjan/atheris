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

import java.util.ArrayList;
import java.util.LinkedList;

import compiler.Position;
import compiler.ast.ASTVisitor;
import compiler.ast.tree.type.AbsAtomType;

/**
 * Enum definition.
 * @author toni kocjan
 *
 */
public class AbsEnumDef extends AbsTypeDef {
	
	/**
	 * Enumeration member definitions.
	 */
	public final ArrayList<AbsDef> definitions;
	
	/**
	 * Type of raw values.
	 * If null, definitions dont have raw values.
	 */
	public final AbsAtomType type;

	/**
	 * Construct enum definition.
	 * @param pos position
	 * @param name name
	 * @param definitions enum definitions
	 * @param type type for each definitions' raw value
	 */
	public AbsEnumDef(Position pos, String name, ArrayList<AbsDef> definitions, AbsAtomType type) {
		super(pos, name);
		
		this.definitions = definitions;
		this.type = type;
	}

	@Override
	public void accept(ASTVisitor aSTVisitor) {
		aSTVisitor.visit(this);
	}

}
