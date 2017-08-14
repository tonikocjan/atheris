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
import compiler.ast.tree.AstStatements;
import compiler.ast.tree.type.AstType;

public class AstFunctionDefinition extends AstDefinition {

	public final List<AstParameterDefinition> pars;
	public final AstType returnType;
	public final AstStatements functionCode;
	public boolean isConstructor;

	public AstFunctionDefinition(Position pos, String name, ArrayList<AstParameterDefinition> parameters, AstType returnType, AstStatements bodyStatements) {
		super(pos, name);
		
		this.pars = parameters;
		this.returnType = returnType;
		this.functionCode = bodyStatements;
		this.isConstructor = false;
	}

    public AstFunctionDefinition(Position pos, String name, ArrayList<AstParameterDefinition> parameters, AstType returnType, AstStatements bodyStatements, boolean isConstructor) {
        super(pos, name);

        this.pars = parameters;
        this.returnType = returnType;
        this.functionCode = bodyStatements;
        this.isConstructor = isConstructor;
    }

    public AstFunctionDefinition(Position pos, String name, ArrayList<AstParameterDefinition> parameters, AstType returnType, AstStatements bodyStatements, boolean isConstructor, boolean isOverriding) {
        super(pos, name);

        this.pars = parameters;
        this.returnType = returnType;
        this.functionCode = bodyStatements;
        this.isConstructor = isConstructor;
    }
	
	@Override
	public String getName() {
		return getStringRepresentation();
	}

	public List<AstParameterDefinition> getParamaters() {
		return pars;
	}

	public AstParameterDefinition getParameterAtIndex(int index) {
		return pars.get(index);
	}

	public void addParamater(AstParameterDefinition newPar) {
		pars.add(0, newPar);
	}

	public int getParameterCount() {
		return pars.size();
	}

	public String getStringRepresentation() {
	    if (isConstructor)
	        return stringRepresentation(parentDefinition.name);
        return stringRepresentation(name);
	}

    public String getStringRepresentation(String otherName) {
        return stringRepresentation(otherName);
    }

    private String stringRepresentation(String name) {
        StringBuilder sb = new StringBuilder(name);
        sb.append('(');
        for (AstParameterDefinition par : pars) {
            sb.append(par.argumentLabel);
            sb.append(':');
        }
        sb.append(')');
        return sb.toString();
    }

	@Override public void accept(ASTVisitor aSTVisitor) { aSTVisitor.visit(this); }
}
