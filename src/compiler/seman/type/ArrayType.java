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

import compiler.ast.tree.def.AstDefinition;

public class ArrayType extends Type implements ReferenceType {

	public final Type memberType;
	public final int count;

	public ArrayType(Type type, int count) {
		this.memberType = type;
		this.count = count;
	}

	@Override
	public boolean sameStructureAs(Type type) {
		if (type.isArrayType()) {
			ArrayType listType = (ArrayType) type;
			return this.memberType.sameStructureAs(listType.memberType);
		}

		return false;
	}

	@Override
	public String toString() {
		return "LIST(" + memberType.toString() + ")";
	}

	@Override
	public int sizeInBytes() {
		return count * memberType.sizeInBytes();
	}

	@Override
	public boolean canBeCastedToType(Type type) {
        if (type.isArrayType()) {
            ArrayType listType = (ArrayType) type;
            return this.memberType.canBeCastedToType(listType.memberType);
        }

        return false;
	}

	@Override
	public boolean containsMember(String name) {
		return false;
	}

	@Override
	public String friendlyName() {
		return "[" + memberType.friendlyName() + "]";
	}

	@Override
	public AstDefinition findMemberDefinitionWithName(String name) {
		return null;
	}
}
