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
import compiler.abstr.tree.AbsStmt;
import compiler.abstr.tree.AccessControl;

/**
 * Definicija.
 * 
 * @author toni kocjan
 */
public abstract class AbsDef extends AbsStmt {

	/** 
	 * Definition name.
	 */
	public final String name;
	
	/**
	 * Parent definition.
	 */
	protected AbsDef parentDef;

	/** Definition's access control (used for member definitions) */
	protected AccessControl accessControl;

	/** Is this definition mutable */
	public final boolean isMutable;

    /** Is this definition overriding another definitition */
    private boolean isOverriding;
	
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
		this.accessControl = AccessControl.Public;
		this.isMutable = false;
		this.isOverriding = false;
	}
	
	/**
	 * Create new definition.
	 *
	 * @param pos
	 *            Position.
	 */
	public AbsDef(Position pos, String name, AccessControl visibility) {
		super(pos);
		
		this.parentDef = null;
		this.name = name;
		this.accessControl = visibility;
        this.isMutable = false;
        this.isOverriding = false;
	}

    /**
     * Create new definition.
     * @param pos
     * @param name
     * @param isMutable
     * @param isOverriding
     * @param visibility
     */
    public AbsDef(Position pos, String name, boolean isMutable, boolean isOverriding, AccessControl visibility) {
        super(pos);

        this.parentDef = null;
        this.name = name;
        this.accessControl = visibility;
        this.isMutable = isMutable;
        this.isOverriding = isOverriding;
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
		this.accessControl = AccessControl.Public;
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
	public AbsDef getParemtDefinition() {
		return this.parentDef;
	}
	
	/**
	 * Get name of the definition.
	 * @return name
	 */
	public String getName() {
		return name;
	}
	
	/**
	 * 
	 * @return
	 */
	public AccessControl getAccessControl() {
		return accessControl;
	}
	
	/**
	 * 
	 * @param accessControl
	 */
	public void setAccessControl(AccessControl accessControl) {
		this.accessControl = accessControl;
	}

    /**
     *
     * @return
     */
    public boolean isOverriding() {
        return isOverriding;
    }

    /**
     *
     * @param isOverriding
     */
    public void setOverriding(boolean isOverriding) {
        this.isOverriding = isOverriding;
    }
}
