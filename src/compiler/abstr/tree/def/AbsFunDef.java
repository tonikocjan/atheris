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

import java.util.*;

import compiler.*;
import compiler.abstr.*;
import compiler.abstr.tree.AbsStmts;
import compiler.abstr.tree.type.AbsType;

/**
 * Function definition.
 * 
 * @author toni kocjan
 */
public class AbsFunDef extends AbsDef {

    public enum FunctionModifier {
        staticMethod, instanceMethod, dynamicInstanceMethod
    }

    /** Parameters. */
	public final LinkedList<AbsParDef> pars;

	/** Return type. */
	public final AbsType type;
	
	/** Function code. */
	public final AbsStmts func;

	/** Is this function a constructor. */
	public boolean isConstructor;

    /** Modifier */
	public FunctionModifier modifier = FunctionModifier.staticMethod;

	/**
	 * Create new function definition.
	 * 
	 * @param pos
	 *            Position
	 * @param name
	 *            Function name
	 * @param parameters
	 *            Parameter list
	 * @param returnType
	 *            Return type
	 * @param bodyStatements
	 *            Function code
	 */
	public AbsFunDef(Position pos, String name, 
			LinkedList<AbsParDef> parameters, AbsType returnType, AbsStmts bodyStatements) {
		super(pos, name);
		
		this.pars = parameters;
		this.type = returnType;
		this.func = bodyStatements;
		this.isConstructor = false;
	}

    /**
     * Create new function definition.
     *
     * @param pos
     *            Position
     * @param name
     *            Function name
     * @param parameters
     *            Parameter list
     * @param returnType
     *            Return type
     * @param bodyStatements
     *            Function code
     * @param isConstructor
     *            Is this function a constructor
     */
    public AbsFunDef(Position pos, String name,
                     LinkedList<AbsParDef> parameters, AbsType returnType, AbsStmts bodyStatements, boolean isConstructor) {
        super(pos, name);

        this.pars = parameters;
        this.type = returnType;
        this.func = bodyStatements;
        this.isConstructor = isConstructor;
    }

    /**
     * Create new function definition.
     *
     * @param pos
     *            Position
     * @param name
     *            Function name
     * @param parameters
     *            Parameter list
     * @param returnType
     *            Return type
     * @param bodyStatements
     *            Function code
     * @param isConstructor
     *            Is this function a constructor
     */
    public AbsFunDef(Position pos, String name,
                     LinkedList<AbsParDef> parameters, AbsType returnType, AbsStmts bodyStatements, boolean isConstructor, boolean isOverriding) {
        super(pos, name);

        this.pars = parameters;
        this.type = returnType;
        this.func = bodyStatements;
        this.isConstructor = isConstructor;
    }
	
	@Override
	public String getName() {
		return getStringRepresentation();
	}

	/**
	 * Get parameters.
	 * @return Parameters list.
	 */
	public LinkedList<AbsParDef> getParamaters() {
		return pars;
	}
	
	/**
	 * Get parameter at given index.
	 * @param index Index.
	 * @return Parameter at index.
	 */
	public AbsParDef getParameterForIndex(int index) {
		return pars.get(index);
	}
	
	/**
	 * Add new parameter to this function.
	 * @param newPar Parameter to be added.
	 */
	public void addParamater(AbsParDef newPar) {
		pars.addFirst(newPar);
	}

	/**
	 * Get parameter count.
	 * @return Parameter count.
	 */
	public int numPars() {
		return pars.size();
	}
	
	/**
	 * Get string representation of this function definition
	 * @return
	 */
	public String getStringRepresentation() {
	    if (isConstructor)
	        return stringRepresentation(parentDef.name);
        return stringRepresentation(name);
	}

    /**
     * Get
     * @param otherName
     * @return
     */
    public String getStringRepresentation(String otherName) {
        return stringRepresentation(otherName);
    }

    /**
     * Get string representation of this function definition.
     * @param name Name of the function
     * @return String representation, i.e.: func name(x: Int, y: Double) Int ==> name(x:y:)
     */
    private String stringRepresentation(String name) {
        StringBuilder sb = new StringBuilder(name);
        sb.append('(');
        for (AbsParDef par : pars) {
            sb.append(par.argumentLabel);
            sb.append(':');
        }
        sb.append(')');
        return sb.toString();
    }

	@Override public void accept(ASTVisitor aSTVisitor) { aSTVisitor.visit(this); }
	
}
