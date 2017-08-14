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
import compiler.ast.tree.AstStatement;
import compiler.ast.tree.DefinitionModifier;

import java.util.HashSet;

public abstract class AstDefinition extends AstStatement {

	public String name;
	protected AstDefinition parentDefinition;
	public final boolean isMutable;
	protected HashSet<DefinitionModifier> modifiers;

	public AstDefinition(Position pos, String name) {
		super(pos);
		
		this.parentDefinition = null;
		this.name = name;
		this.isMutable = false;

		this.modifiers = new HashSet<>();
		this.modifiers.add(DefinitionModifier.isPublic);
	}

	public AstDefinition(Position pos, String name, boolean isMutable) {
		super(pos);

		this.parentDefinition = null;
		this.name = name;
        this.isMutable = isMutable;

        this.modifiers = new HashSet<>();
        this.modifiers.add(DefinitionModifier.isPublic);
	}

	public AstDefinition(Position pos, String name, AstDefinition parent) {
        this(pos, name);

        this.parentDefinition = parent;
    }

	public void setParentDefinition(AstDefinition parent) {
		this.parentDefinition = parent;
	}

	public AstDefinition getParentDefinition() {
		return this.parentDefinition;
	}

	public String getName() {
		return name;
	}

    public void setModifiers(HashSet<DefinitionModifier> modifiers) {
        this.modifiers.addAll(modifiers);
    }

    public void setModifier(DefinitionModifier modifier) {
        this.modifiers.add(modifier);
    }

	public boolean isPublic() {
        return modifiers.isEmpty() || modifiers.contains(DefinitionModifier.isPublic);
	}

    public boolean isPrivate() {
        return modifiers.contains(DefinitionModifier.isPrivate);
    }

    public boolean isOverriding() {
        return modifiers.contains(DefinitionModifier.isOverriding);
    }

    public boolean isFinal() {
        return modifiers.contains(DefinitionModifier.isFinal);
    }

    public boolean isStatic() {
        return modifiers.contains(DefinitionModifier.isStatic);
    }

    public boolean isDynamic() {
        return !(isFinal() || isStatic());
    }
}
