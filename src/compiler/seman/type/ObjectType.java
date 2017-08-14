package compiler.seman.type;

import compiler.Logger;
import compiler.ast.tree.def.AstClassDefinition;
import compiler.ast.tree.def.AstDefinition;
import compiler.ast.tree.def.AstVariableDefinition;
import compiler.ast.tree.type.AstType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

/**
 * Object memberType.
 */
public abstract class ObjectType extends Type {

    /**
     * Class definition.
     */
    public final AstClassDefinition classDefinition;

    /**
     * Class member names and types.
     */
    protected final ArrayList<String> memberNames;
    protected final ArrayList<Type> memberTypes;

    /**
     * Mapping for memberDefinitions.
     */
    protected final HashMap<String, AstDefinition> definitions = new HashMap<>();

    /**
     * Sum of sizes of all members.
     */
    protected final int size;

    /**
     * Inital framePointerOffset of all members (space used for memberType descriptor).
     */
    protected final int reservedSize;

    /**
     * Base class (null if no base class).
     */
    public final CanType baseClass;
    protected final ObjectType base;

    /**
     * Create new class memberType.
     * @param definition Class definition.
     * @param names Name for each member.
     * @param types Type for each member.
     * @param baseClass Base class for this class memberType.
     */
    public ObjectType(AstClassDefinition definition, ArrayList<String> names, ArrayList<Type> types, CanType baseClass, int reservedSize) {
        if (names.size() != types.size()) {
            Logger.error("Internal error :: compiler.seman.memberType.ObjectType: "
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

    /**
     * Create new class memberType.
     * @param definition Class definition.
     * @param names Name for each member.
     * @param types Type for each member.
     */
    public ObjectType(AstClassDefinition definition, ArrayList<String> names, ArrayList<Type> types, int reservedSize) {
        this(definition, names, types, null, reservedSize);
    }

    /**
     *
     * @param definition
     * @param memberName
     * @param memberType
     * @return
     */
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

    /**
     *
     * @param conformance
     * @return
     */
    public boolean addConformance(AstType conformance) {
        for (AstType c : classDefinition.conformances) {
            if (c.getName().equals(conformance.getName())) return false;
        }

        classDefinition.conformances.add(conformance);
        return true;
    }

    /////////

    /**
     * Get memberType for member.
     * @param name Name of the member.
     * @return Type of the member (or null if member with such getName doesn't exist).
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
     * Calculate framePointerOffset for member.
     * @param name member getName
     * @return framePointerOffset of that member
     */
    public int offsetForMember(String name) {
        int offset = reservedSize;

        // first check in base class
        if (base != null) {
            offset = base.offsetForMember(name);

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

    /**
     *
     * @return
     */
    @Override
    public int sizeInBytes() {
        int size = this.size;

        if (base != null) {
            size += base.sizeInBytes() - reservedSize;
        }

        return size;
    }

    /**
     *
     * @param name Member getName
     * @return
     */
    @Override
    public boolean containsMember(String name) {
        // first check in base class
        if (base != null) {
            boolean contains = base.containsMember(name);

            if (contains) return true;
        }

        return memberNames.contains(name);
    }

    /**
     *
     * @param type Given memberType.
     * @return
     */
    @Override
    public boolean sameStructureAs(Type type) {
        // nominal types
        return type.friendlyName().equals(friendlyName());
    }

    /**
     *
     * @param type Given memberType
     * @return
     */
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
    public AstDefinition findMemberDefinitionForName(String name) {
        return findMemberForName(name, true);
    }

    /**
     *
     * @param name
     * @param fromBase
     * @return
     */
    public AstDefinition findMemberForName(String name, boolean fromBase) {
        if (fromBase && base != null) {
            AstDefinition member = base.findMemberDefinitionForName(name);

            if (member != null) return member;
        }

        return definitions.get(name);
    }

    /**
     * Check whether this memberType is descendant of baseType.
     * @param baseType
     * @return True or false
     */
    public boolean isDescendantOf(ObjectType baseType) {
        if (base == null) {
            return false;
        }

        if (base.friendlyName().equals(baseType.friendlyName())) {
            return true;
        }

        return base.isDescendantOf(baseType);
    }

    /**
     * Check whether this memberType conforms to interface, i.e. implements all of it's methods.
     * @param interfaceType
     * @return
     */
    public boolean conformsTo(InterfaceType interfaceType) {
        for (AstDefinition def: interfaceType.definition.definitions.definitions) {
            AstDefinition member = findMemberDefinitionForName(def.getName());
            if (member == null) return false;
        }

        return true;
    }

    @Override
    public String friendlyName() {
        return classDefinition.name;
    }

    /**
     *
     */
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
            System.out.println("    Offset: " + offsetForMember(name));
        }
    }

    /**
     *
     * @param type
     * @return
     */
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

    /**
     *
     * @return
     */
    public Iterator<Type> getTypes() {
        return memberTypes.iterator();
    }

    /**
     *
     * @return
     */
    public Iterator<String> getNames() {
        return memberNames.iterator();
    }
}
