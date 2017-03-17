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

import java.util.LinkedList;

import compiler.Position;
import compiler.abstr.ASTVisitor;
import compiler.abstr.tree.AbsDefs;
import compiler.abstr.tree.AbsStmt;
import compiler.abstr.tree.AbsStmts;
import compiler.abstr.tree.AtomTypeKind;
import compiler.abstr.tree.type.AbsAtomType;

/**
 * Class definition.
 * @author toni kocjan
 *
 */
public class AbsClassDef extends AbsTypeDef {
	
	/** Definitions. */
	public final AbsDefs definitions;
	
	/** Constructors (initializers) */
	public final LinkedList<AbsFunDef> contrustors = new LinkedList<>();
	
	/**
	 * Create new class definition.
	 * @param name
	 * @param pos
	 * @param statements
	 */
	public AbsClassDef(String name, Position pos, LinkedList<AbsDef> definitions, 
			LinkedList<AbsStmt> initExpressions) {
		super(pos, name);
		
		if (definitions.size() > 0) {
			Position start = definitions.getFirst().position;
			Position end = definitions.getLast().position;
			this.definitions = new AbsDefs(new Position(start, end), definitions);
		}
		else
			this.definitions = new AbsDefs(pos, definitions);
		
		// set this definition as parent for all member definitions
		for (AbsDef def : this.definitions.definitions)
			def.setParentDefinition(this);
		
		// add default constructor
		AbsFunDef contructor = new AbsFunDef(pos, 
				name,
				new LinkedList<>(), 
				new AbsAtomType(pos, AtomTypeKind.VOID), 
				new AbsStmts(pos, initExpressions));
		contrustors.add(contructor);
	}
	
	public AbsDef findDefinitionForName(String name) {
		return definitions.findDefinitionForName(name);
	}
	
	@Override public void accept(ASTVisitor aSTVisitor) { aSTVisitor.visit(this); }

}
