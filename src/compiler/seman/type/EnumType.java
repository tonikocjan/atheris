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

import compiler.Report;
import compiler.ast.tree.def.AbsDef;
import compiler.ast.tree.def.AbsEnumDef;
import compiler.ast.tree.def.AbsEnumMemberDef;

/**
 * Enumeration type.
 * @author toni kocjan
 *
 */
public class EnumType extends Type implements ReferenceType {

	/**
	 * Enumeration definition.
	 */
	public final AbsEnumDef enumDefinition;
	
	/**
	 * Enum members.
	 */
	private final LinkedHashMap<String, ClassType> members = new LinkedHashMap<>();
	
	/**
	 * Number of members.
	 */
	public final int membersCount;
	
	/**
	 * Selected member (i.e.: Languages.Java.rawValue -> selected member is Java).
	 */
	public String selectedMember;

	/**
	 * Create new enumeration.
	 */
	public EnumType(AbsEnumDef definition, 
			ArrayList<String> names, ArrayList<ClassType> types) {
		if (names.size() != types.size())
			Report.error("Internal error :: compiler.seman.type.EnumType: "
					+ "names count not equal types count");
		
		this.enumDefinition = definition;
		this.membersCount = names.size();
		this.selectedMember = null;

		for (int i = 0; i < names.size(); i++)
			members.put(names.get(i), types.get(i));
	}
	
	/**
	 * Copy another Enum Type
	 * @param copyType Type to copy.
	 * @param selectedMember Selected member for this type.
	 */
	public EnumType(EnumType copyType, String selectedMember) {
		this.enumDefinition = copyType.enumDefinition;
		this.membersCount = copyType.membersCount;
		this.selectedMember = selectedMember;
		
		for (Map.Entry<String, ClassType> entry : copyType.members.entrySet()) {
			members.put(entry.getKey(), entry.getValue());
		}
	}

	/**
	 * Get type for member.
	 * @param name Name of member.
	 * @return Type of found member (null if member with such name doesn't exist).
	 */
	public ClassType getMemberTypeForName(String name) {
		return members.get(name);
	}
	
	/**
	 * 
	 * @param name
	 * @return
	 */
	public int memberOffsetForName(String name) {
		int offset = 0;
		for (AbsDef def : enumDefinition.definitions) {
			if (def.getName().equals(name)) return offset;
			offset++;
		}
		return -1;
	}
	
	@Override
	public AbsDef findMemberForName(String name) {
		for (AbsDef def : enumDefinition.definitions) {
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
		for (AbsDef def : enumDefinition.definitions) {
			sb.append(def.getName());
			if (def instanceof AbsEnumMemberDef) {
				if (((AbsEnumMemberDef)def).value != null)
					sb.append(" - Raw value: " + ((AbsEnumMemberDef)def).value.value);
			}
			
			if (def != enumDefinition.definitions.get(enumDefinition.definitions.size() - 1))
				sb.append(", ");
		}
		sb.append(")");
		return sb.toString();
	}

	@Override
	public int size() {
		return 4;
	}

	@Override
	public boolean canCastTo(Type t) {
		return false;
	}

	@Override
	public String friendlyName() {
		return getName();
	}

}
