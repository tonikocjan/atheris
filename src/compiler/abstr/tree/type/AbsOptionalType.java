package compiler.abstr.tree.type;

import compiler.Position;
import compiler.abstr.ASTVisitor;

public class AbsOptionalType extends AbsType {
	
	/**
	 * Child type for this optional.
	 */
	public final AbsType childType;

	/**
	 * 
	 * @param pos
	 * @param childType
	 */
	public AbsOptionalType(Position pos, AbsType childType) {
		super(pos);
		
		this.childType = childType;
	}

	/**
	 * 
	 */
	@Override
	public void accept(ASTVisitor aSTVisitor) {
		aSTVisitor.visit(this);
	}

}
