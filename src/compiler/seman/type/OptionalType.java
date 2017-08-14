package compiler.seman.type;

import compiler.ast.tree.def.AstDefinition;

public class OptionalType extends Type implements ReferenceType {

	public final Type childType;
    public final boolean isForced;

	public OptionalType(Type childType, boolean isForced) {
		this.childType = childType;
		this.isForced = isForced;
	}

	@Override
	public boolean sameStructureAs(Type type) {
		if (type.isOptionalType()) {
		    return ((OptionalType) type).childType.sameStructureAs(childType);
        }

        return false;
	}

	@Override
	public boolean canBeCastedToType(Type t) {
		return childType.sameStructureAs(t);
	}

	@Override
	public int sizeInBytes() {
		return childType.sizeInBytes();
	}

	@Override
	public boolean containsMember(String name) {
		return childType.containsMember(name);
	}

	@Override
	public AstDefinition findMemberDefinitionWithName(String name) {
		return childType.findMemberDefinitionWithName(name);
	}

	@Override
	public String toString() {
		return "Optional (" + childType.toString() + ")";
	}

	@Override
	public String friendlyName() {
		return childType.friendlyName() + "?";
	}

}
