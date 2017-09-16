package compiler.seman.type;

import compiler.logger.LoggerFactory;
import compiler.logger.LoggerInterface;
import compiler.ast.tree.def.AstClassDefinition;
import compiler.ast.tree.def.AstDefinition;
import compiler.ast.tree.def.AstVariableDefinition;
import compiler.ast.tree.type.AstType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

public abstract class ObjectType extends Type {

    private static LoggerInterface logger = LoggerFactory.logger();

    public final AstClassDefinition classDefinition;
    protected final ArrayList<String> memberNames;
    protected final ArrayList<Type> memberTypes;
    protected final HashMap<String, AstDefinition> definitions = new HashMap<>();
    protected final int size;
    protected final int reservedSize;
    public final CanType baseClass;
    protected final ObjectType base;

    public ObjectType(AstClassDefinition definition, ArrayList<String> names, ArrayList<Type> types, CanType baseClass, int reservedSize) {
        if (names.size() != types.size()) {
            logger.error("Internal error :: compiler.seman.memberType.ObjectType: "
                    + "names elementCount not equal types elementCount");
        }

        int size = 0;
        for (int i = 0; i < names.size(); i++) {
            definitions.put(names.get(i), definition.memberDefinitions.definitions.get(i));

            if (definition.memberDefinitions.definitions.get(i) instanceof AstVariableDefinition) {
                size += types.get(i).sizeInBytes();
            }
        }

        this.size = size + reservedSize;
        this.memberNames = names;
        this.memberTypes = types;
        this.classDefinition = definition;
        this.baseClass = baseClass;
        this.base = baseClass == null ? null : (ObjectType) baseClass.childType;
        this.reservedSize = reservedSize;
    }

    public ObjectType(AstClassDefinition definition, ArrayList<String> names, ArrayList<Type> types, int reservedSize) {
        this(definition, names, types, null, reservedSize);
    }

    public boolean addMember(AstDefinition definition, String memberName, Type memberType) {
        if (definitions.containsKey(memberName)) {
            return false;
        }

        definitions.put(memberName, definition);

        memberNames.add(memberName);
        memberTypes.add(memberType);
        classDefinition.memberDefinitions.definitions.add(definition);

        return true;
    }

    public boolean addConformance(AstType conformance) {
        for (AstType c : classDefinition.conformances) {
            if (c.getName().equals(conformance.getName())) return false;
        }

        classDefinition.conformances.add(conformance);
        return true;
    }

    public Type getTypeOfMemberWithName(String name) {
        // first check in base class
        if (baseClass != null) {
            Type type = base.getTypeOfMemberWithName(name);

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

    public int getOffsetOfMemberWithName(String name) {
        int offset = reservedSize;

        // first check in base class
        if (base != null) {
            offset = base.getOffsetOfMemberWithName(name);

            if (offset < base.sizeInBytes())
                return offset;
        }

        Iterator<String> namesIterator = memberNames.iterator();
        Iterator<Type> typesIterator = memberTypes.iterator();
        Iterator<AstDefinition> defsIterator = classDefinition.memberDefinitions.definitions.iterator();

        while (defsIterator.hasNext()) {
            AstDefinition next = defsIterator.next();

            if (!(next instanceof AstVariableDefinition)) {
                namesIterator.next();
                typesIterator.next();
                continue;
            }

            if (name.equals(namesIterator.next())) break;
            offset += typesIterator.next().sizeInBytes();
        }

        return offset;
    }

    @Override
    public int sizeInBytes() {
        int size = this.size;

        if (base != null) {
            size += base.sizeInBytes() - reservedSize;
        }

        return size;
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
        // nominal types
        return type.friendlyName().equals(friendlyName());
    }

    @Override
    public boolean canBeCastedToType(Type type) {
        if (type.isInterfaceType()) {
            return conformsTo((InterfaceType) type);
        }

        if (!type.isObjectType()) {
            return false;
        }

        return isDescendantOf((ObjectType) type);
    }

    @Override
    public AstDefinition findMemberDefinitionWithName(String name) {
        return findMemberDefinitionWithName(name, true);
    }

    public AstDefinition findMemberDefinitionWithName(String name, boolean fromBase) {
        if (fromBase && base != null) {
            AstDefinition member = base.findMemberDefinitionWithName(name);

            if (member != null) return member;
        }

        return definitions.get(name);
    }

    public boolean isDescendantOf(ObjectType baseType) {
        if (base == null) {
            return false;
        }

        if (base.friendlyName().equals(baseType.friendlyName())) {
            return true;
        }

        return base.isDescendantOf(baseType);
    }

    public boolean conformsTo(InterfaceType interfaceType) {
        for (AstDefinition def: interfaceType.definition.definitions.definitions) {
            AstDefinition member = findMemberDefinitionWithName(def.getName());
            if (member == null) return false;
        }

        return true;
    }

    public boolean isConformingTo(InterfaceType type) {
        if (base != null && base.isConformingTo(type)) {
            return true;
        }

        for (AstType conformance : classDefinition.conformances) {
            if (conformance.getName().equals(type.friendlyName())) {
                return true;
            }
        }

        return false;
    }

    @Override
    public String friendlyName() {
        return classDefinition.name;
    }

    public void debugPrint() {
        if (base != null) {
            base.debugPrint();
        }

        System.out.println(friendlyName() + " - sizeInBytes: " + sizeInBytes());

        Iterator<String> namesIterator = memberNames.iterator();
        Iterator<Type> typesIterator = memberTypes.iterator();

        while (namesIterator.hasNext()) {
            String name = namesIterator.next();
            System.out.println("  " + name + ": " + typesIterator.next().friendlyName());
            System.out.println("    Offset: " + getOffsetOfMemberWithName(name));
        }
    }

    public Iterator<Type> getTypes() {
        return memberTypes.iterator();
    }

    public Iterator<String> getNames() {
        return memberNames.iterator();
    }
}
