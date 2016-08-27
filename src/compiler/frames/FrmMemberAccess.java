package compiler.frames;

import compiler.abstr.tree.def.AbsVarDef;
import compiler.seman.type.ClassType;

public class FrmMemberAccess extends FrmAccess {

	public final AbsVarDef memberDef;

	// TODO: is this attribure necessary?
	public final ClassType parentType;
	
	public FrmMemberAccess(AbsVarDef memberDef, ClassType parentType) {
		this.memberDef = memberDef;
		this.parentType = parentType;
	}
	
	public int offsetForMember() {
		return parentType.offsetOf(memberDef.name);
	}

	@Override
	public String toString() {
		return "Member (" + memberDef.name + ", offset: " + parentType.offsetOf(memberDef.name) + ")";
	}
}
