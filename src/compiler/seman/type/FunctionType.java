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

import compiler.ast.tree.def.AstDefinition;
import compiler.ast.tree.def.AstFunctionDefinition;

public class FunctionType extends Type implements ReferenceType {

	public final List<Type> parameterTypes;
	public Type resultType;
	public final AstFunctionDefinition functionDefinition;

	public FunctionType(Vector<Type> parTypes, Type resultType, AstFunctionDefinition definition) {
		this.parameterTypes = parTypes;
		this.resultType = resultType;
		this.functionDefinition = definition;
	}

	public int getParamaterCount() {
		return parameterTypes.size();
	}

	public Type getTypeForParameterAtIndex(int index) {
		return parameterTypes.get(index);
	}

	@Override
	public boolean sameStructureAs(Type type) {
		if (type.isFunctionType()) {
			FunctionType funType = (FunctionType) type;
			if (this.getParamaterCount() != funType.getParamaterCount())
				return false;
			for (int par = 0; par < getParamaterCount(); par++)
				if (!this.getTypeForParameterAtIndex(par).sameStructureAs(funType.getTypeForParameterAtIndex(par)))
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
			str += t.friendlyName() + (t == parameterTypes.get(getParamaterCount() - 1) ? "" : ",") ;
		String res = resultType == null ? "?" : resultType.toString();
		str += ") -> " + res;
		return str;
	}
	
	@Override
	public String friendlyName() {
		return toString();
	}

	@Override
	public int sizeInBytes() {
		int size = 4;

		for (Type t : parameterTypes)
			size += t.sizeInBytes();
		
		return Math.max(resultType.sizeInBytes(), size);
	}

	@Override
	public boolean canBeCastedToType(Type t) {
		return false;
	}

	@Override
	public boolean containsMember(String name) {
		return false;
	}

	@Override
	public AstDefinition findMemberDefinitionWithName(String name) {
		return null;
	}

}
