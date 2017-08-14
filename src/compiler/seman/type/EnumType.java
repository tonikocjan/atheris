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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

import compiler.Logger;
import compiler.ast.tree.def.AstDefinition;
import compiler.ast.tree.def.AstEnumDefinition;
import compiler.ast.tree.def.AstEnumMemberDefinition;

public class EnumType extends Type implements ReferenceType {

    private final LinkedHashMap<String, ClassType> members = new LinkedHashMap<>();
	public final AstEnumDefinition enumDefinition;
	public final int membersCount;
	public String selectedMember;

	public EnumType(AstEnumDefinition definition,
			ArrayList<String> names, ArrayList<ClassType> types) {
		if (names.size() != types.size())
			Logger.error("Internal error :: compiler.seman.memberType.EnumType: "
					+ "names elementCount not equal types elementCount");
		
		this.enumDefinition = definition;
		this.membersCount = names.size();
		this.selectedMember = null;

		for (int i = 0; i < names.size(); i++)
			members.put(names.get(i), types.get(i));
	}

	public static EnumType copyType(EnumType typeToCopy, String selectedMember) {
	    return new EnumType(typeToCopy, selectedMember);
    }

	private EnumType(EnumType copyType, String selectedMember) {
        this.enumDefinition = copyType.enumDefinition;
        this.membersCount = copyType.membersCount;
        this.selectedMember = selectedMember;

        for (Map.Entry<String, ClassType> entry : copyType.members.entrySet()) {
            members.put(entry.getKey(), entry.getValue());
        }
	}

	public ClassType getMemberTypeForName(String name) {
		return members.get(name);
	}

	public int offsetForMember(String name) {
		int offset = 0;
		for (AstDefinition def : enumDefinition.definitions) {
			if (def.getName().equals(name)) return offset;
			offset++;
		}
		return -1;
	}
	
	@Override
	public AstDefinition findMemberDefinitionForName(String name) {
		for (AstDefinition def : enumDefinition.definitions) {
			String definitionsName = def.getName();
			
			if (definitionsName.equals(name))
				return def;
		}
		return null;
	}
	
	@Override
	public boolean containsMember(String name) {
		return members.containsKey(name);
	}

	public String getName() {
		return enumDefinition.name;
	}
	
	@Override
	public boolean sameStructureAs(Type type) {
		if (!(type instanceof EnumType))
			return false;
		
		EnumType otherEnumType = (EnumType) type;
		return otherEnumType.enumDefinition.name.equals(enumDefinition.name);
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("Enum: ");
		sb.append(enumDefinition.name + "(");
		for (AstDefinition def : enumDefinition.definitions) {
			sb.append(def.getName());
			if (def instanceof AstEnumMemberDefinition) {
				if (((AstEnumMemberDefinition)def).value != null)
					sb.append(" - Raw value: " + ((AstEnumMemberDefinition)def).value.value);
			}
			
			if (def != enumDefinition.definitions.get(enumDefinition.definitions.size() - 1))
				sb.append(", ");
		}
		sb.append(")");
		return sb.toString();
	}

	@Override
	public int sizeInBytes() {
		return 4;
	}

	@Override
	public boolean canBeCastedToType(Type t) {
		return false;
	}

	@Override
	public String friendlyName() {
		return getName();
	}

}
