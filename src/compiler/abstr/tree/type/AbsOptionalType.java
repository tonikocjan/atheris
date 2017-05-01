package compiler.abstr.tree.type;

import compiler.Position;
import compiler.abstr.ASTVisitor;

public class AbsOptionalType extends AbsType {
	
	/** Child type */
	public final AbsType childType;

	/** Is this optional force, i.e. let x: Int! */
	public final boolean isForced;

	/**
	 * 
	 * @param pos
	 * @param childType
	 */
	public AbsOptionalType(Position pos, AbsType childType, boolean isForced) {
		super(pos);
		
		this.childType = childType;
		this.isForced = isForced;
	}

	/**
	 * 
	 */
	@Override
	public void accept(ASTVisitor aSTVisitor) {
		aSTVisitor.visit(this);
	}

    @Override
    public String getName() {
        return childType.getName();
    }
}
