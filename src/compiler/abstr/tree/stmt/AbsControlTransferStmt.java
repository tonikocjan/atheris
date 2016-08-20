package compiler.abstr.tree.stmt;

import compiler.Position;
import compiler.abstr.ASTVisitor;
import compiler.abstr.tree.ControlTransfer;

public class AbsControlTransferStmt extends AbsConditionalStmt {
	
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
