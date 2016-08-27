package compiler.frames;

import compiler.abstr.tree.def.AbsVarDef;
import compiler.seman.type.ClassType;
import compiler.seman.type.TupleType;
import compiler.seman.type.Type;

public class FrmMemberAccess extends FrmAccess {

	/**
	 * Member definition.
	 */
	public final AbsVarDef memberDef;

	/**
	 * Parent type.
	 */
	public final Type parentType;
	
	public FrmMemberAccess(AbsVarDef memberDef, Type parentType) {
		this.memberDef = memberDef;
		this.parentType = parentType;
	}
	
	public int offsetForMember() {
		if (parentType instanceof ClassType)
			return ((ClassType) parentType).offsetOf(memberDef.getName());
		return -1;
	}

	@Override
	public String toString() {
		return "Member (" + memberDef.name + ", offset: " + offsetForMember() + ")";
	}
}
