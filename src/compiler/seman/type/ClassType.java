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
public class ClassType extends ObjectType implements ReferenceType {

    public ClassType(AbsClassDef definition, LinkedList<String> names, LinkedList<Type> types, CanType baseClass) {
        super(definition, names, types, baseClass);
    }

    public ClassType(AbsClassDef definition, LinkedList<String> names, LinkedList<Type> types) {
        super(definition, names, types, null);
    }

    public int indexForMember(String name) {
        int index = 0;

        // first check in base class
        if (base != null) {
            index = ((ClassType) base).indexForMember(name);

            if (index < ((ClassType) base).instanceMethodCount())
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

    public int virtualTableSize() {
        int size = 0;

        if (base != null) {
            size = ((ClassType) base).virtualTableSize();
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
            baseIterator = ((ClassType) base).generateVirtualTable();
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

}
