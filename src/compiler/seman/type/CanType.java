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
import compiler.abstr.tree.def.AbsFunDef;
import compiler.abstr.tree.def.AbsVarDef;

import java.util.Iterator;
import java.util.LinkedList;

/**
 * This type is used for representing class definition, enum definition, ... .
 * @author toni
 *
 */
// TODO: What should this type be called?
public class CanType extends Type {

    /** Child type. */
	public final Type childType;

	/** Static definitions in childType */
	private final LinkedList<AbsDef> staticDefinitions = new LinkedList<>();
    private final LinkedList<Type> staticTypes = new LinkedList<>();
    private final LinkedList<String> staticNames = new LinkedList<>();

    /**
     *
     * @param child
     */
	public CanType(Type child) {
		this.childType = child;
	}

	@Override
	public boolean sameStructureAs(Type type) {
		if (type instanceof CanType) {
            return ((CanType) type).childType.sameStructureAs(childType);
        }
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
        return childType.friendlyName() + ".Type";
	}

	@Override
	public AbsDef findMemberForName(String name) {
		return childType.findMemberForName(name);
	}

	public void addStaticDefinition(AbsDef def, String name, Type type) {
	    staticDefinitions.add(def);
	    staticNames.add(name);
	    staticTypes.add(type);
    }

    public int staticSize() {
        Iterator<AbsDef> defIterator = staticDefinitions.iterator();
        Iterator<Type> typeIterator = staticTypes.iterator();

        int size = 0;
        while (defIterator.hasNext()) {
            Type t = typeIterator.next();
            AbsDef def = defIterator.next();

            if (def instanceof AbsVarDef) {
                size += t.size();
            }
        }

        return size;
    }

    public boolean containsStaticMember(String name) {
        return staticNames.contains(name);
    }

    public AbsDef findStaticMemberForName(String memberName) {
        Iterator<String> namesIterator = staticNames.iterator();
        Iterator<AbsDef> defsIterator = staticDefinitions.iterator();

        while (defsIterator.hasNext()) {
            String name = namesIterator.next();
            AbsDef def = defsIterator.next();

            if (name.equals(memberName)) {
                return def;
            }
        }

        return null;
    }

    public int offsetForStaticMember(String name) {
        Iterator<String> namesIterator = staticNames.iterator();
        Iterator<Type> typesIterator = staticTypes.iterator();
        Iterator<AbsDef> defsIterator = staticDefinitions.iterator();

        int offset = 0;
        while (defsIterator.hasNext()) {
            AbsDef next = defsIterator.next();

            if (!(next instanceof AbsVarDef)) {
                namesIterator.next();
                typesIterator.next();
                continue;
            }

            if (name.equals(namesIterator.next())) break;
            offset += typesIterator.next().size();
        }

        return offset;
    }
    /**
     *
     * @param definition
     * @param memberName
     * @param memberType
     * @return
     */
    public boolean addStaticMember(AbsDef definition, String memberName, Type memberType) {
        if (staticDefinitions.contains(memberName)) {
            return false;
        }

        staticNames.add(memberName);
        staticTypes.add(memberType);
        staticDefinitions.add(definition);

        return true;
    }

    /**
     * Get type for member.
     * @param name Name of the member.
     * @return Type of the member (or null if member with such name doesn't exist).
     */
    public Type getStaticMemberTypeForName(String name) {
        Iterator<String> namesIterator = staticNames.iterator();
        Iterator<Type> typesIterator = staticTypes.iterator();

        while (namesIterator.hasNext()) {
            Type t = typesIterator.next();

            if (namesIterator.next().equals(name)) {
                return t;
            }
        }

        return null;
    }
}
