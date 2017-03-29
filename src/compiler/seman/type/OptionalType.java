package compiler.seman.type;

import compiler.abstr.tree.def.AbsDef;

public class OptionalType extends ReferenceType {
	
	/**
	 * 
	 */
	public final Type childType;
	
	/**
	 * 
	 * @param childType
	 */
	public OptionalType(Type childType) {
		this.childType = childType;
	}

	
	@Override
	public boolean sameStructureAs(Type type) {
		return false;
	}

	@Override
	public boolean canCastTo(Type t) {
		return childType.sameStructureAs(t);
	}

	@Override
	public int size() {
		return 4;
	}

	@Override
	public boolean containsMember(String name) {
		return false;
	}

	@Override
	public AbsDef findMemberForName(String name) {
		return null;
	}

	@Override
	public String toString() {
		return "Optional (" + childType.toString() + ")";
	}

	@Override
	public String friendlyName() {
		return toString();
	}

}
