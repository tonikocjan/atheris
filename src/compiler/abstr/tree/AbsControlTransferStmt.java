package compiler.abstr.tree;

import compiler.Position;
import compiler.abstr.ASTVisitor;

public class AbsControlTransferStmt extends AbsStmt {
	
	public final ControlTransfer control;

	public AbsControlTransferStmt(Position pos, ControlTransfer control) {
		super(pos);
		this.control = control;
	}

	@Override
	public void accept(ASTVisitor aSTVisitor) {
		aSTVisitor.visit(this);
	}

}
