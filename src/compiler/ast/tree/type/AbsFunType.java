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

package compiler.ast.tree.type;

import java.util.ArrayList;
import java.util.List;

import compiler.Position;
import compiler.ast.ASTVisitor;

public class AbsFunType extends AbsType {
	
	/** Types of paramaters */
	public final ArrayList<AbsType> parameterTypes;
	
	/** Return memberType */
	public final AbsType returnType;

    /**
     *
     * @param pos
     * @param parameters
     * @param returnType
     */
	public AbsFunType(Position pos, ArrayList<AbsType> parameters, AbsType returnType) {
		super(pos);
		this.parameterTypes = parameters;
		this.returnType = returnType;
	}

    /**
     *
     * @return
     */
	public int parameterCount() {
		return parameterTypes.size();
	}

    /**
     *
     * @param t
     * @return
     */
	public AbsType type(int t) {
		return parameterTypes.get(t);
	}

    /**
     *
     * @return
     */
	public String toString() {
		String str = "(";
		for (AbsType p : parameterTypes) {
			str += p.toString();
			if (p != parameterTypes.get(parameterCount() - 1)) str += ", ";
		}
		str += ") -> " + returnType.toString();
		return str;
	}

    /**
     *
     * @param aSTVisitor
     */
	@Override
	public void accept(ASTVisitor aSTVisitor) {
		aSTVisitor.visit(this);
	}

    /**
     *
     * @return
     */
    @Override
    public String getName() {
        return null;
    }
}
