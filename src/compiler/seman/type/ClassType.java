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

import compiler.Report;
import compiler.abstr.tree.def.AbsClassDef;
import compiler.abstr.tree.def.AbsDef;

/**
 * Class type. 
 * @author toni kocjan
 */
public class ClassType extends ReferenceType {
	
	/**
	 * Class definition.
	 */
	public final AbsClassDef classDefinition;

	/**
	 * Class member names and types.
	 */
	public final LinkedList<String> memberNames;
    public final LinkedList<Type> memberTypes;

	/**
	 * Mapping for definitions.
	 */
	public final HashMap<String, AbsDef> definitions = new HashMap<>();

	/**
	 * Sum of sizes of all members.
	 */
	private final int size;

    /**
     * Base class (null if no base class).
     */
    public final CanType baseClass;
    private final ClassType base;

	/**
	 * Create new class type.
	 * @param definition Class definition.
	 * @param names Name for each member.
	 * @param types Type for each member.
     * @param baseClass Base class for this class type.
	 */
	public ClassType(AbsClassDef definition, LinkedList<String> names, LinkedList<Type> types, CanType baseClass) {
		if (names.size() != types.size()) {
            Report.error("Internal error :: compiler.seman.type.ClassType: "
                    + "names count not equal types count");
        }

		int size = 0;
		for (int i = 0; i < names.size(); i++) {
			definitions.put(names.get(i), definition.definitions.definitions.get(i));
			size += types.get(i).size();
		}

        this.size = size;
		this.memberNames = names;
		this.memberTypes = types;
		this.classDefinition = definition;
		this.baseClass = baseClass;
		this.base = baseClass == null ? null : (ClassType) baseClass.childType;

        descriptorMapping.put(this, descriptor - 1);
	}

    /**
     * Create new class type.
     * @param definition Class definition.
     * @param names Name for each member.
     * @param types Type for each member.
     */
    public ClassType(AbsClassDef definition, LinkedList<String> names, LinkedList<Type> types) {
        this(definition, names, types, null);
    }

	
	/**
	 * Get type for member.
	 * @param name Name of the member.
	 * @return Type of the member (or null if member with such name doesn't exist).
	 */
	public Type getMemberTypeForName(String name) {
	    // first check in base class
        if (baseClass != null) {
            Type type = base.getMemberTypeForName(name);

            if (type != null) {
                return type;
            }
        }

		int index = memberNames.indexOf(name);

		if (index >= 0) {
		    return memberTypes.get(index);
        }

        return null;
	}
	
	/**
	 * Calculate offset for member.
	 * @param name member name
	 * @return offset of that member
	 */
	public int offsetForMember(String name) {
		int offset = 0;

        // first check in base class
        if (baseClass != null) {
            offset = base.offsetForMember(name);
        }

		Iterator<String> namesIterator = memberNames.iterator();
        Iterator<Type> typesIterator = memberTypes.iterator();
		
		while (namesIterator.hasNext()) {
			if (name.equals(namesIterator.next())) break;
			offset += typesIterator.next().size();
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
	    // first check in base class
        if (base != null) {
            boolean contains = base.containsMember(name);

            if (contains) return true;
        }

		return memberNames.contains(name);
	}

	@Override
	public boolean sameStructureAs(Type type) {
        if (!type.isClassType())
            return false;

        return type.friendlyName().equals(friendlyName());
    }
	
	@Override
	public AbsDef findMemberForName(String name) {
	    if (base != null) {
            AbsDef member = base.findMemberForName(name);

            if (member != null) return member;
        }

		return definitions.get(name);
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();

		sb.append("Class: ");
		sb.append(classDefinition.name + "(");
		sb.append("Base: (");
		sb.append(classDefinition.baseClass == null ? "/" : classDefinition.baseClass.toString());
		sb.append("), ");

        Iterator<String> namesIterator = memberNames.iterator();
        Iterator<Type> typesIterator = memberTypes.iterator();

        while (namesIterator.hasNext()) {
			sb.append(namesIterator.next() + ":" + typesIterator.next().toString());
			if (!namesIterator.hasNext()) sb.append(";");
		}

		sb.append(")");
		return sb.toString();
	}

	@Override
	public int size() {
	    int size = this.size;

	    if (base != null) {
	        size += base.size();
        }

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

	public void debugPrint() {
        System.out.println(friendlyName());

        Iterator<String> namesIterator = memberNames.iterator();
        Iterator<Type> typesIterator = memberTypes.iterator();

        while (namesIterator.hasNext()) {
            String name = namesIterator.next();
            System.out.println("  " + name + ": " + typesIterator.next().friendlyName());
            System.out.println("    Offset: " + offsetForMember(name));
        }
    }
}
