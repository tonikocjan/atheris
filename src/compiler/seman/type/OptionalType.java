package compiler.seman.type;

import compiler.ast.tree.def.AbsDef;

public class OptionalType extends Type implements ReferenceType {

        /** Child memberType */
	public final Type childType;

    /** Is implicitly forced */
    public final boolean isForced;

    /**
	 * 
	 * @param childType
	 */
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
	public AbsDef findMemberDefinitionForName(String name) {
		return childType.findMemberDefinitionForName(name);
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
