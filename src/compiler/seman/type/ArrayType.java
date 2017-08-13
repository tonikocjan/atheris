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

import compiler.ast.tree.def.AbsDef;

/**
 * Array type.
 * 
 * @author toni
 */
public class ArrayType extends Type implements ReferenceType {

	/**
	 * Type of array member.
	 */
	public final Type type;

	/**
	 * Size (number of elements) in array.
	 */
	public final int count;

	/**
	 * Create new Array Type.
	 * @param type Type for each member.
	 * @param count Number of elements.
	 */
	public ArrayType(Type type, int count) {
		this.type = type;
		this.count = count;
	}

	@Override
	public boolean sameStructureAs(Type type) {
		if (type.isArrayType()) {
			ArrayType listType = (ArrayType) type;
			return this.type.sameStructureAs(listType.type);
		}

		return false;
	}

	@Override
	public String toString() {
		return "LIST(" + type.toString() + ")";
	}

	@Override
	public int sizeInBytes() {
		return count * type.sizeInBytes();
	}

	@Override
	public boolean canBeCastedToType(Type type) {
        if (type.isArrayType()) {
            ArrayType listType = (ArrayType) type;
            return this.type.canBeCastedToType(listType.type);
        }

        return false;
	}

	@Override
	public boolean containsMember(String name) {
		return false;
	}

	@Override
	public String friendlyName() {
		return "[" + type.friendlyName() + "]";
	}

	@Override
	public AbsDef findMemberDefinitionForName(String name) {
		return null;
	}
}
