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

import Utils.Constants;
import compiler.Position;
import compiler.abstr.ASTVisitor;
import compiler.abstr.tree.*;
import compiler.abstr.tree.type.AbsAtomType;
import compiler.abstr.tree.type.AbsType;
import compiler.abstr.tree.type.AbsTypeName;

/**
 * Class definition.
 * @author toni kocjan
 *
 */
public class AbsClassDef extends AbsTypeDef {
	
	/** Definitions. */
	public final AbsDefs definitions;
	
	/** Constructors (initializers) */
	public final LinkedList<AbsFunDef> contrustors;

    /**
     * * Create new class definition.
     * @param name Definition's name
     * @param pos Position in file
     * @param definitions Member definitions
     * @param initExpressions Initializing expressions for default constructor
     * @param constructors Other constructors
     */
	public AbsClassDef(String name, Position pos, LinkedList<AbsDef> definitions, 
			LinkedList<AbsStmt> initExpressions, LinkedList<AbsFunDef> constructors) {
		super(pos, name);
        this.contrustors = constructors;
        this.definitions = new AbsDefs(position, definitions);

        // set this definition as parent for all member definitions
        for (AbsDef def : this.definitions.definitions) {
            def.setParentDefinition(this);
        }

		String constructorName = name;
		String defaultConstructorName = name + "()";

		// add default constructor
		AbsFunDef defaultConstructor = new AbsFunDef(pos,
                constructorName,
				new LinkedList<>(),
				new AbsAtomType(pos, AtomTypeKind.VOID),
				new AbsStmts(pos, initExpressions),
                true);
		defaultConstructor.setParentDefinition(this);

        // if set to true, don't add defaultConstructor again into constructors array
		boolean hasDefaultConstructor = false;

		// default constructor code is added to every constructor
        for (AbsFunDef constructor : constructors) {
            constructor.setParentDefinition(this);
            constructor.func.statements.addAll(0, defaultConstructor.func.statements);

            if (constructor.getStringRepresentation(name).equals(defaultConstructorName)) {
                hasDefaultConstructor = true;
            }
        }

        if (!hasDefaultConstructor ) {
            contrustors.add(defaultConstructor);
        }
	}
	
	public AbsDef findDefinitionForName(String name) {
		return definitions.findDefinitionForName(name);
	}
	
	@Override public void accept(ASTVisitor aSTVisitor) { aSTVisitor.visit(this); }

}
