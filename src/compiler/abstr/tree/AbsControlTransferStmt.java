package compiler.abstr.tree;

import compiler.Position;
import compiler.abstr.ASTVisitor;

public class AbsControlTransferStmt extends AbsStmt {
	
	public final ControlTransferEnum control;

	public AbsControlTransferStmt(Position pos, ControlTransferEnum control) {
		super(pos);
		this.control = control;
	}

	@Override
	public void accept(ASTVisitor aSTVisitor) {
		aSTVisitor.visit(this);
	}

}
