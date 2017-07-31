package compiler.seman.type;

import utils.Constants;
import compiler.abstr.tree.def.AbsDef;
import compiler.abstr.tree.def.AbsFunDef;
import compiler.abstr.tree.def.AbsInterfaceDef;


/**
 *
 */
public class InterfaceType extends Type {

    /** Definition. */
    public final AbsInterfaceDef definition;

    /**
     * Create new interface type.
     * @param definition
     */
    public InterfaceType(AbsInterfaceDef definition) {
        this.definition = definition;
    }

    @Override
    public boolean sameStructureAs(Type type) {
        if (type.friendlyName().equals(friendlyName())) {
            return true;
        }

        // any type can be assigned to Any
        if (friendlyName().equals(Constants.any)) {
            return true;
        }

        if (!type.isObjectType()) {
            return false;
        }

        ObjectType classType = (ObjectType) type;
        return classType.isConformingTo(this);
    }

    @Override
    public boolean canCastTo(Type type) {
        return false;
    }

    @Override
    public int size() {
        return 0;
    }

    @Override
    public boolean containsMember(String name) {
        for (AbsDef def : definition.definitions.definitions) {
            if (((AbsFunDef) def).getStringRepresentation().equals(name))
                return true;
        }

        return false;
    }

    @Override
    public AbsDef findMemberForName(String name) {
        for (AbsDef def : definition.definitions.definitions) {
            if (((AbsFunDef) def).getStringRepresentation().equals(name))
                return def;
        }

        return null;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append("Interface: ");
        sb.append(definition.name);
        return sb.toString();
    }

    @Override
    public String friendlyName() {
        return definition.getName();
    }

    public int indexForMember(String name) {
        return 0;
    }
}
