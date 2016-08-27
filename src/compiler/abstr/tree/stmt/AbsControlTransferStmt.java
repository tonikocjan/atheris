package compiler.abstr.tree.stmt;

import compiler.Position;
import compiler.abstr.ASTVisitor;
import compiler.abstr.tree.ControlTransferKind;

public class AbsControlTransferStmt extends AbsConditionalStmt {
	
	public final ControlTransferKind control;

	public AbsControlTransferStmt(Position pos, ControlTransferKind control) {
		super(pos);
		this.control = control;
	}

	@Override
	public void accept(ASTVisitor aSTVisitor) {
		aSTVisitor.visit(this);
	}

}
