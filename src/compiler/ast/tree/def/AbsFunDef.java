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

import java.util.*;

import compiler.*;
import compiler.ast.*;
import compiler.ast.tree.AbsStmts;
import compiler.ast.tree.type.AbsType;

/**
 * Function definition.
 * 
 * @author toni kocjan
 */
public class AbsFunDef extends AbsDef {

    /** Parameters. */
	public final ArrayList<AbsParDef> pars;

	/** Return type. */
	public final AbsType type;
	
	/** Function code. */
	public final AbsStmts func;

	/** Is this function a constructor. */
	public boolean isConstructor;

	/**
	 * Create new function definition.
	 * 
	 * @param pos
	 *            Position
	 * @param name
	 *            Function getName
	 * @param parameters
	 *            Parameter list
	 * @param returnType
	 *            Return type
	 * @param bodyStatements
	 *            Function code
	 */
	public AbsFunDef(Position pos, String name, ArrayList<AbsParDef> parameters, AbsType returnType, AbsStmts bodyStatements) {
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
     *            Function getName
     * @param parameters
     *            Parameter list
     * @param returnType
     *            Return type
     * @param bodyStatements
     *            Function code
     * @param isConstructor
     *            Is this function a constructor
     */
    public AbsFunDef(Position pos, String name, ArrayList<AbsParDef> parameters, AbsType returnType, AbsStmts bodyStatements, boolean isConstructor) {
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
     *            Function getName
     * @param parameters
     *            Parameter list
     * @param returnType
     *            Return type
     * @param bodyStatements
     *            Function code
     * @param isConstructor
     *            Is this function a constructor
     */
    public AbsFunDef(Position pos, String name, ArrayList<AbsParDef> parameters, AbsType returnType, AbsStmts bodyStatements, boolean isConstructor, boolean isOverriding) {
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
	public ArrayList<AbsParDef> getParamaters() {
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
		pars.add(0, newPar);
	}

	/**
	 * Get parameter count.
	 * @return Parameter count.
	 */
	public int numPars() {
		return pars.size();
	}
	
	/**
	 * Get string representation of this function definition.
	 * @return
	 */
	public String getStringRepresentation() {
	    if (isConstructor)
	        return stringRepresentation(parentDef.name);
        return stringRepresentation(name);
	}

    /**
     * Get string representation of this function definition using different getName.
     * @param otherName
     * @return
     */
    public String getStringRepresentation(String otherName) {
        return stringRepresentation(otherName);
    }

    /**
     * Get string representation of this function definition.
     * @param name Name of the function
     * @return String representation, i.e.: func getName(x: Int, y: Double) Int ==> getName(x:y:)
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

    /**
     *
     * @param aSTVisitor
     */
	@Override public void accept(ASTVisitor aSTVisitor) { aSTVisitor.visit(this); }
}
