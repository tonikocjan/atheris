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

import compiler.Position;
import compiler.ast.ASTVisitor;
import compiler.ast.tree.*;
import compiler.ast.tree.type.AstAtomType;
import compiler.ast.tree.type.AstType;

public class AstClassDefinition extends AstTypeDefinition {

	public final AstDefinitions memberDefinitions;
	public final ArrayList<AstFunctionDefinition> construstors;
	public final AstFunctionDefinition defaultConstructor;
    public final AstFunctionDefinition staticConstructor;
	public final AstType baseClass;
	public final ArrayList<AstType> conformances;

	public AstClassDefinition(String name) {
	    super(new Position(0, 0), name);
	    this.construstors = new ArrayList<>();
	    this.conformances = new ArrayList<>();
	    this.baseClass = null;
	    this.memberDefinitions = new AstDefinitions(position, new ArrayList<>());

        AstFunctionDefinition constructor = new AstFunctionDefinition(position,
                name,
                new ArrayList<>(),
                new AstAtomType(position, AtomTypeKind.VOID),
                new AstStatements(position, new ArrayList<>()),
                true);
        constructor.setParentDefinition(this);
        this.defaultConstructor = constructor;
        this.staticConstructor = constructor;
    }

    public AstClassDefinition(String name, Position pos, AstType baseClass, ArrayList<AstType> conformances, ArrayList<AstDefinition> definitions,
                              ArrayList<AstStatement> defaultConstructor, ArrayList<AstFunctionDefinition> constructors, ArrayList<AstStatement> staticConstructor) {
        super(pos, name);
        this.construstors = constructors;
        this.memberDefinitions = new AstDefinitions(position, definitions);
        this.baseClass = baseClass;
        this.conformances = conformances;

        // set this definition as parent for all member memberDefinitions
        for (AstDefinition def : this.memberDefinitions.definitions) {
            def.setParentDefinition(this);
        }

        String constructorName = name;
        String defaultConstructorName = name + "()";

        // add default constructor
        AstFunctionDefinition constructor = new AstFunctionDefinition(pos,
                constructorName,
                new ArrayList<>(),
                new AstAtomType(pos, AtomTypeKind.VOID),
                new AstStatements(pos, defaultConstructor),
                true);
        constructor.setParentDefinition(this);
        this.defaultConstructor = constructor;

        // if set to true, don't add defaultConstructor into constructors
        boolean hasDefaultConstructor = false;

        // default constructor code is added to every constructor
        for (AstFunctionDefinition c : constructors) {
            c.setParentDefinition(this);
            c.functionCode.statements.addAll(0, constructor.functionCode.statements);

            if (c.getStringRepresentation(name).equals(defaultConstructorName)) {
                hasDefaultConstructor = true;
            }
        }

        if (!hasDefaultConstructor) {
            constructors.add(constructor);
        }

        // add static constructor
        constructor = new AstFunctionDefinition(pos,
                constructorName,
                new ArrayList<>(),
                new AstAtomType(pos, AtomTypeKind.VOID),
                new AstStatements(pos, staticConstructor),
                true);
        constructor.setParentDefinition(this);
        this.staticConstructor = constructor;
    }

	public AstClassDefinition(String name, Position pos, AstType baseClass, ArrayList<AstDefinition> definitions,
                              ArrayList<AstStatement> defaultConstructor, ArrayList<AstFunctionDefinition> constructors) {
        this(name, pos, baseClass, new ArrayList<>(), definitions, defaultConstructor, constructors, new ArrayList<>());
	}

    public AstClassDefinition(String name, Position pos, ArrayList<AstDefinition> definitions,
                              ArrayList<AstStatement> defaultConstructor, ArrayList<AstFunctionDefinition> constructors) {
        this(name, pos, null, new ArrayList<>(), definitions, defaultConstructor, constructors, new ArrayList<>());
    }

	@Override public void accept(ASTVisitor aSTVisitor) { aSTVisitor.visit(this); }

    @Override
    public String toString() {
        return "AstClassDefinition " + position.toString() + ": " + getName();
    }
}
