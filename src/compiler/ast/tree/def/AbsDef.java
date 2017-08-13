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
import compiler.ast.tree.AbsStmt;
import compiler.ast.tree.Modifier;

import java.util.HashSet;

/**
 * Definicija.
 * 
 * @author toni kocjan
 */
public abstract class AbsDef extends AbsStmt {

	/** 
	 * Definition getName.
	 */
	public String name;
	
	/**
	 * Parent definition.
	 */
	protected AbsDef parentDef;

	/** Is this definition mutable */
	public final boolean isMutable;

	/***/
	protected HashSet<Modifier> modifiers;
	
	/**
	 * Create new definition.
	 *
	 * @param pos
	 *            Position.
	 */
	public AbsDef(Position pos, String name) {
		super(pos);
		
		this.parentDef = null;
		this.name = name;
		this.isMutable = false;

		this.modifiers = new HashSet<>();
		this.modifiers.add(Modifier.isPublic);
	}
	
	/**
	 * Create new definition.
	 *
	 * @param pos
	 *            Position.
	 */
	public AbsDef(Position pos, String name, boolean isMutable) {
		super(pos);

		this.parentDef = null;
		this.name = name;
        this.isMutable = isMutable;

        this.modifiers = new HashSet<>();
        this.modifiers.add(Modifier.isPublic);
	}
	
	/**
	 * Crate new definition.
	 *
	 * @param pos
	 *            Position.
	 * @param parent 
	 * 			  Parent definition for this definition
	 */
	public AbsDef(Position pos, String name, AbsDef parent) {
        this(pos, name);

        this.parentDef = parent;
    }

	/**
	 * Set parent definition.
	 * @param parent
	 */
	public void setParentDefinition(AbsDef parent) {
		this.parentDef = parent;
	}
	
	/**
	 * Get parent definition.
	 * @return Parent definition.
	 */
	public AbsDef getParentDefinition() {
		return this.parentDef;
	}
	
	/**
	 * Get getName of the definition.
	 * @return getName
	 */
	public String getName() {
		return name;
	}

    /**
     *
     * @param modifiers
     */
    public void setModifiers(HashSet<Modifier> modifiers) {
        this.modifiers.addAll(modifiers);
    }

    /**
     *
     * @param modifier
     */
    public void setModifier(Modifier modifier) {
        this.modifiers.add(modifier);
    }

    /**
	 * 
	 * @return
	 */
	public boolean isPublic() {
        return modifiers.isEmpty() || modifiers.contains(Modifier.isPublic);
	}

    /**
     *
     * @return
     */
    public boolean isPrivate() {
        return modifiers.contains(Modifier.isPrivate);
    }

    /**
     *
     * @return
     */
    public boolean isOverriding() {
        return modifiers.contains(Modifier.isOverriding);
    }

    /**
     *
     * @return
     */
    public boolean isFinal() {
        return modifiers.contains(Modifier.isFinal);
    }

    /**
     *
     * @return
     */
    public boolean isStatic() {
        return modifiers.contains(Modifier.isStatic);
    }

    /**
     *
     * @return
     */
    public boolean isDynamic() {
        return !(isFinal() || isStatic());
    }
}
