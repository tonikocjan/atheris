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
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import compiler.Report;
import compiler.abstr.tree.def.AbsClassDef;
import compiler.abstr.tree.def.AbsDef;

/**
 * Class type. 
 * @author toni kocjan
 */
public class ClassType extends PointerType {
	
	/**
	 * Class definition.
	 */
	public final AbsClassDef classDefinition;

	/**
	 * Class member types.
	 */
	private final LinkedHashMap<String, Type> members = new LinkedHashMap<>();
	
	/**
	 * Mapping for definitions.
	 */
	private final HashMap<String, AbsDef> definitions = new HashMap<>();
	
	/**
	 * Sum of sizes of all members.
	 */
	private final int size;

	/**
	 * Create new class type.
	 * @param definition Class definition.
	 * @param names Name for each member.
	 * @param types Type for each member.
	 */
	public ClassType(AbsClassDef definition, 
			ArrayList<String> names, ArrayList<Type> types) {
		if (names.size() != types.size())
			Report.error("Internal error :: compiler.seman.type.SemClassType: "
					+ "names count not equal types count");

		int size = 0;
		for (int i = 0; i < names.size(); i++) {
			members.put(names.get(i), types.get(i));
			definitions.put(names.get(i), definition.definitions.definitions.get(i));
			size += types.get(i).size();
		}
		
		this.size = size;
		this.classDefinition = definition;
	}
	
	/**
	 * Get type for member.
	 * @param name Name of the member.
	 * @return Type of the member (or null if member with such name doesn't exist).
	 */
	public Type getMemberTypeForName(String name) {
		return members.get(name);
	}
	
	/**
	 * Calculate offset for member.
	 * @param name member name
	 * @return offset of that member
	 */
	public int offsetForMember(String name) {
		int offset = 0;
		
		for (Map.Entry<String, Type> entry : members.entrySet()) {
			if (name.equals(entry.getKey())) break;
			offset += entry.getValue().size();
		}
		
		return offset;
	}

	/**
	 * Get name of this type.
	 * @return Class name.
	 */
	public String getName() {
		return classDefinition.name;
	}

	@Override
	public boolean containsMember(String name) {
		return members.containsKey(name);
	}

	@Override
	public boolean sameStructureAs(Type type) {
		return false;
	}
	
	@Override
	public AbsDef findMemberForName(String name) {
		return definitions.get(name);
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		int i = 0;
		sb.append("Class: ");
		sb.append(classDefinition.name + "(");
		for (Map.Entry<String, Type> entry : members.entrySet()) {
			sb.append(entry.getKey() + ":" + entry.getValue().toString());
			if (++i < members.size()) sb.append(";");
		}
		sb.append(")");
		return sb.toString();
	}

	@Override
	public int size() {
		return size;
	}

	@Override
	public boolean canCastTo(Type t) {
		return false;
	}

	@Override
	public String friendlyName() {
		return classDefinition.name;
	}

}
