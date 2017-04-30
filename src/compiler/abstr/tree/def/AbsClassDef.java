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
import compiler.abstr.tree.*;
import compiler.abstr.tree.type.AbsAtomType;
import compiler.abstr.tree.type.AbsType;

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

	/** Default constructor */
	public final AbsFunDef defaultConstructor;

	/** Base class type name (null if no base class) */
	public final AbsType baseClass;

	/** Conforming interfaces. */
	public final LinkedList<AbsType> conformances;


    /**
     * Create new class definition.
     * @param name
     * @param pos
     * @param baseClass
     * @param conformances
     * @param definitions
     * @param defaultConstructor
     * @param constructors
     */
    public AbsClassDef(String name, Position pos, AbsType baseClass, LinkedList<AbsType> conformances, LinkedList<AbsDef> definitions,
                       LinkedList<AbsStmt> defaultConstructor, LinkedList<AbsFunDef> constructors) {
        super(pos, name);
        this.contrustors = constructors;
        this.definitions = new AbsDefs(position, definitions);
        this.baseClass = baseClass;
        this.conformances = conformances;

        // set this definition as parent for all member definitions
        for (AbsDef def : this.definitions.definitions) {
            def.setParentDefinition(this);
        }

        String constructorName = name;
        String defaultConstructorName = name + "()";

        // add default constructor
        AbsFunDef constructor = new AbsFunDef(pos,
                constructorName,
                new LinkedList<>(),
                new AbsAtomType(pos, AtomTypeKind.VOID),
                new AbsStmts(pos, defaultConstructor),
                true);
        constructor.setParentDefinition(this);
        this.defaultConstructor = constructor;

        // if set to true, don't add defaultConstructor into constructors
        boolean hasDefaultConstructor = false;

        // default constructor code is added to every constructor
        for (AbsFunDef c : constructors) {
            c.setParentDefinition(this);
            c.func.statements.addAll(0, constructor.func.statements);

            if (c.getStringRepresentation(name).equals(defaultConstructorName)) {
                hasDefaultConstructor = true;
            }
        }

        if (!hasDefaultConstructor) {
            contrustors.add(constructor);
        }
    }

    /**
     * * Create new class definition.
     * @param name Definition's name
     * @param pos Position in file
     * @param definitions Member definitions
     * @param defaultConstructor Initializing expressions for default constructor
     * @param constructors Other constructors
     */
	public AbsClassDef(String name, Position pos, AbsType baseClass, LinkedList<AbsDef> definitions,
                       LinkedList<AbsStmt> defaultConstructor, LinkedList<AbsFunDef> constructors) {
        this(name, pos, baseClass, null, definitions, defaultConstructor, constructors);
	}

    /**
     *
     * @param name
     * @param pos
     * @param definitions
     * @param defaultConstructor
     * @param constructors
     */
    public AbsClassDef(String name, Position pos, LinkedList<AbsDef> definitions,
                       LinkedList<AbsStmt> defaultConstructor, LinkedList<AbsFunDef> constructors) {
        this(name, pos, null, new LinkedList<>(), definitions, defaultConstructor, constructors);
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
    public String toString() {
        return "AbsClassDef " + position.toString() + ": " + getName();
    }
}
