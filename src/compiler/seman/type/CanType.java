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

import compiler.abstr.tree.def.AbsDef;

/**
 * This type is used for representing class definition, enum definition, ... nodes.
 * @author toni
 *
 */
// TODO: What should this type be called?
public class CanType extends Type {
	
	public final Type childType;
	
	public CanType(Type child) {
		this.childType = child;
	}

	@Override
	public boolean sameStructureAs(Type type) {
		if (type instanceof CanType)
			return ((CanType) type).childType.sameStructureAs(childType);
		return false;
	}

	@Override
	public boolean canCastTo(Type t) {
		return false;
	}

	@Override
	public int size() {
		return childType.size();
	}

	@Override
	public String toString() {
		return childType.friendlyName() + ".Type";
	}

	@Override
	public boolean containsMember(String name) {
		return childType.containsMember(name);
	}

	@Override
	public String friendlyName() {
        if (childType instanceof ClassType)
            return childType.friendlyName() + ".Type";
        else
            return ((EnumType) childType).enumDefinition.name + ".Type";
	}

	@Override
	public AbsDef findMemberForName(String name) {
		return childType.findMemberForName(name);
	}
}
