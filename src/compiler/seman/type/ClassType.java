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
import compiler.abstr.tree.def.AbsFunDef;

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
	private final LinkedList<String> memberNames;
    private final LinkedList<Type> memberTypes;

	/**
	 * Mapping for definitions.
	 */
	private final HashMap<String, AbsDef> definitions = new HashMap<>();

	/**
	 * Sum of sizes of all members.
	 */
	private final int size;

    /**
     * Inital offset of all members (space used for type descriptor).
     */
    private final int reservedSize = 4;

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

        this.size = size + reservedSize;
		this.memberNames = names;
		this.memberTypes = types;
		this.classDefinition = definition;
		this.baseClass = baseClass;
		this.base = baseClass == null ? null : (ClassType) baseClass.childType;

        descriptorMapping.put(this, descriptor);
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
     *
     * @param definition
     * @param memberName
     * @param memberType
     * @return
     */
    public boolean addMember(AbsDef definition, String memberName, Type memberType) {
        memberNames.add(memberName);
        memberTypes.add(memberType);

        if (definitions.containsKey(memberName)) {
            return false;
        }

        definitions.put(memberName, definition);
        return true;
    }

    /////////
	
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
		int offset = reservedSize;

        // first check in base class
        if (base != null) {
            offset = base.offsetForMember(name);

            if (offset < base.size())
                return offset;
        }

		Iterator<String> namesIterator = memberNames.iterator();
        Iterator<Type> typesIterator = memberTypes.iterator();
		
		while (namesIterator.hasNext()) {
			if (name.equals(namesIterator.next())) break;
			offset += typesIterator.next().size();
		}
		
		return offset;
	}

	public int indexForMember(String name) {
        int index = 0;

        // first check in base class
        if (base != null) {
            index = base.indexForMember(name);

            if (index < base.instanceMethodCount())
                return index;
        }

        Iterator<String> namesIterator = memberNames.iterator();
        Iterator<Type> typeIterator = memberTypes.iterator();

        while (namesIterator.hasNext()) {
            String next = namesIterator.next();
            Type type = typeIterator.next();

            if (name.equals(next)) break;

            if (type.isFunctionType()) {
                index++;
            }
        }

        return index;
    }

    public int instanceMethodCount() {
	    // TODO: - Optimize
        int count = 0;
        Iterator<Type> typeIterator = memberTypes.iterator();

        while (typeIterator.hasNext()) {
            if (typeIterator.next().isFunctionType()) {
                count += 1;
            }
        }

        return count;
    }

    @Override
    public int size() {
        int size = this.size;

        if (base != null) {
            size += base.size() - reservedSize;
        }

        return size;
    }

    public int virtualTableSize() {
	    int size = 0;

	    if (base != null) {
	        size = base.virtualTableSize();
        }

        for (Type type : memberTypes) {
            if (type instanceof FunctionType) {
                size += 4;
            }
        }

        return size;
    }

    public Iterator<AbsFunDef> generateVirtualTable() {
        Iterator<AbsFunDef> baseIterator = null;

        if (base != null) {
            baseIterator = base.generateVirtualTable();
        }

        Iterator<AbsFunDef> finalBaseIterator = baseIterator;
        return new Iterator<AbsFunDef>() {

            public AbsFunDef current = null;
            Iterator<AbsDef> defIterator = classDefinition.definitions.definitions.iterator();

            @Override
            public boolean hasNext() {
                if (finalBaseIterator != null && finalBaseIterator.hasNext()) {
                    current = (AbsFunDef) finalBaseIterator.next();

                    AbsDef member = findMemberForName(current.getName(), false);
                    if (member != null) {
                        if (!(member instanceof AbsFunDef)) Report.error("Member must be AbsFunDef - FIXME");
                        current = (AbsFunDef) member;
                    }

                    return true;
                }

                while (defIterator.hasNext()) {
                    AbsDef next = defIterator.next();

                    if (next instanceof AbsFunDef) {
                        current = (AbsFunDef) next;
                        return true;
                    }
                }

                return false;
            }

            @Override
            public AbsFunDef next() {
                return current;
            }
        };
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

        return descriptor == type.descriptor;
    }

    @Override
    public boolean canCastTo(Type type) {
        if (!type.isClassType() || base == null) {
            return false;
        }

        ClassType baseClass = base;
        int targetDescriptor = type.descriptor;

        while (baseClass != null) {
            if (targetDescriptor == baseClass.descriptor) {
                return true;
            }

            baseClass = baseClass.base;
        }

        return false;
    }
	
	@Override
	public AbsDef findMemberForName(String name) {
	    return findMemberForName(name, true);
	}

    public AbsDef findMemberForName(String name, boolean fromBase) {
        if (fromBase && base != null) {
            AbsDef member = base.findMemberForName(name);

            if (member != null) return member;
        }

        return definitions.get(name);
    }

    /**
     * Get name of this type.
     * @return Classes name.
     */
    public String getName() {
        return classDefinition.name;
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
	public String friendlyName() {
		return classDefinition.name;
	}

	public void debugPrint() {
        if (base != null) {
            base.debugPrint();
        }

        System.out.println(friendlyName());

        Iterator<String> namesIterator = memberNames.iterator();
        Iterator<Type> typesIterator = memberTypes.iterator();

        while (namesIterator.hasNext()) {
            String name = namesIterator.next();
            System.out.println("  " + name + ": " + typesIterator.next().friendlyName());
            System.out.println("    Offset: " + offsetForMember(name));
        }
    }

    public Iterator<Type> getTypes() {
        return memberTypes.iterator();
    }
}
