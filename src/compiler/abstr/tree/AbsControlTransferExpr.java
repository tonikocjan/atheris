package compiler.abstr.tree;

import compiler.Position;
import compiler.abstr.ASTVisitor;

public class AbsControlTransferExpr extends AbsExpr {
	
	public final ControlTransfer control;

	public AbsControlTransferExpr(Position pos, ControlTransfer control) {
		super(pos);
		this.control = control;
	}

	@Override
	public void accept(ASTVisitor aSTVisitor) {
		aSTVisitor.visit(this);
	}

}
