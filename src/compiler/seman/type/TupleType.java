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

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;

import compiler.abstr.tree.def.AbsDef;
import compiler.abstr.tree.def.AbsTupleDef;

public class TupleType extends PointerType {
	
	/**
	 * Definition for this type.
	 */
	public final AbsTupleDef definition;

	/**
	 * Tuple member types.
	 */
	private final LinkedHashMap<String, Type> members = new LinkedHashMap<>();

	/**
	 * Sum of sizes of all members.
	 */
	private final int size;
	
	/**
	 * Create new tuple type.
	 * @param types
	 * @param names
	 */
	public TupleType(LinkedList<Type> types, LinkedList<String> names) {
		int size = 0;
		
		for (int i = 0; i < names.size(); i++) {
			members.put(names.get(i), types.get(i));
			size += types.get(i).size();
		}
		
		this.definition = null;
		this.size = size;
	}
	
	/**
	 * Create new tuple type.
	 * @param definition
	 * @param types
	 * @param names
	 */
	public TupleType(AbsTupleDef definition, LinkedList<Type> types, LinkedList<String> names) {
		int size = 0;
		
		for (int i = 0; i < names.size(); i++) {
			members.put(names.get(i), types.get(i));
			size += types.get(i).size();
		}
		
		this.definition = definition;
		this.size = size;
	}

	/**
	 * Get type for given name.
	 * @param name
	 * @return
	 */
	public Type typeForName(String name) {
		return members.get(name);
	}
	
	/**
	 * Calculate offset for member.
	 * @param name member name
	 * @return offset of that member
	 */
	public int offsetOf(String name) {
		int offset = 0;
		
		for (Map.Entry<String, Type> entry : members.entrySet()) {
			if (name.equals(entry.getKey())) break;
			offset += entry.getValue().size();
		}
		
		return offset;
	}

	@Override
	public boolean sameStructureAs(Type type) {
		if (!type.isTupleType()) return false;
		
		TupleType t2 = (TupleType) type;
		if (t2.members.size() != members.size()) return false;
		
		// FIXME: - This should be improved
		Object[] thisKeySet = members.keySet().toArray();
		Object[] otherKeySet = t2.members.keySet().toArray();
		
		for (int i = 0; i < thisKeySet.length; i++) {
			if (!members.get(thisKeySet[i]).sameStructureAs(t2.members.get(otherKeySet[i])))
				return false;
		}

		return true;
	}

	@Override
	public boolean canCastTo(Type t) {
		return false;
	}

	@Override
	public int size() {
		return size;
	}

	@Override
	public boolean containsMember(String name) {
		return members.containsKey(name);
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		int i = 0;
		sb.append("Tuple: (");
		for (Map.Entry<String, Type> entry : members.entrySet()) {
			sb.append(entry.getKey());
			sb.append(":");
			sb.append(entry.getValue().toString());
			if (++i < members.size()) sb.append(", ");
		}
		sb.append(")");
		return sb.toString();
	}
	
	@Override
	public String friendlyName() {
		return toString();
	}

	@Override
	public AbsDef findMemberForName(String name) {
		return null;
	}

}
