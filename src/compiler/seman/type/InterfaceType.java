package compiler.seman.type;

import compiler.ast.tree.def.AstDefinition;
import compiler.ast.tree.def.AstInterfaceDefinition;
import utils.Constants;
import compiler.ast.tree.def.AstFunctionDefinition;

public class InterfaceType extends Type {

    public final AstInterfaceDefinition definition;

    public InterfaceType(AstInterfaceDefinition definition) {
        this.definition = definition;
    }

    @Override
    public boolean sameStructureAs(Type type) {
        if (type.friendlyName().equals(friendlyName())) {
            return true;
        }

        // any memberType can be assigned to Any
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
    public boolean canBeCastedToType(Type type) {
        return false;
    }

    @Override
    public int sizeInBytes() {
        return 0;
    }

    @Override
    public boolean containsMember(String name) {
        for (AstDefinition def : definition.definitions.definitions) {
            if (((AstFunctionDefinition) def).getStringRepresentation().equals(name))
                return true;
        }

        return false;
    }

    @Override
    public AstDefinition findMemberDefinitionWithName(String name) {
        for (AstDefinition def : definition.definitions.definitions) {
            if (((AstFunctionDefinition) def).getStringRepresentation().equals(name))
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
