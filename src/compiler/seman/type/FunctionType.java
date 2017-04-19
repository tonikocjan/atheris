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

package compiler.seman.type;

import java.util.*;

import compiler.abstr.tree.def.AbsDef;
import compiler.abstr.tree.def.AbsFunDef;

/**
 * Function type.
 * 
 * @author toni
 */
public class FunctionType extends Type implements ReferenceType {

	/** 
	 * Parameter types. 
	 */
	public final Vector<Type> parameterTypes;

	/** 
	 * Result type. 
	 */
	public Type resultType;

	/**
	 * Function definition.
	 */
	public final AbsFunDef functionDefinition;

	/**
	 * Create new function type.
	 * 
	 * @param parTypes
	 *            Parameter types.
	 * @param resultType
	 *            Result type.
	 * @param definition
	 * 			  Function definition.
	 */
	public FunctionType(Vector<Type> parTypes, Type resultType, AbsFunDef definition) {
		this.parameterTypes = parTypes;
		this.resultType = resultType;
		this.functionDefinition = definition;
	}

	/**
	 * Get parameter count.
	 * 
	 * @return Parameter count.
	 */
	public int getParamaterCount() {
		return parameterTypes.size();
	}

	/**
	 * Get parameter type at given index.
	 * 
	 * @param index
	 *            Indeks of parameter.
	 * @return 
	 * 			  Parameter type.
	 */
	public Type getParType(int index) {
		return parameterTypes.elementAt(index);
	}

	@Override
	public boolean sameStructureAs(Type type) {
		if (type.isFunctionType()) {
			FunctionType funType = (FunctionType) type;
			if (this.getParamaterCount() != funType.getParamaterCount())
				return false;
			for (int par = 0; par < getParamaterCount(); par++)
				if (!this.getParType(par).sameStructureAs(funType.getParType(par)))
					return false;
			if (!this.resultType.sameStructureAs(funType.resultType))
				return false;
			return true;
		}
		return false;
	}
	
	@Override
	public String toString() {
		String str = "";
		str += "(";
		for (Type t : parameterTypes)
			str += t.friendlyName() + (t == parameterTypes.lastElement() ? "" : ",") ;
		String res = resultType == null ? "?" : resultType.toString();
		str += ") -> " + res;
		return str;
	}
	
	@Override
	public String friendlyName() {
		return toString();
	}

	@Override
	public int size() {
		int size = 4;

		for (Type t : parameterTypes)
			size += t.size();
		
		return Math.max(resultType.size(), size);
	}

	@Override
	public boolean canCastTo(Type t) {
		return false;
	}

	@Override
	public boolean containsMember(String name) {
		return false;
	}

	@Override
	public AbsDef findMemberForName(String name) {
		return null;
	}

}
