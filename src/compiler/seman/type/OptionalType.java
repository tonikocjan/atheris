package compiler.seman.type;

import compiler.ast.tree.def.AbsDef;

public class OptionalType extends Type implements ReferenceType {

        /** Child type */
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
	public boolean canCastTo(Type t) {
		return childType.sameStructureAs(t);
	}

	@Override
	public int size() {
		return childType.size();
	}

	@Override
	public boolean containsMember(String name) {
		return childType.containsMember(name);
	}

	@Override
	public AbsDef findMemberForName(String name) {
		return childType.findMemberForName(name);
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
