package compiler.imcode;

import compiler.abstr.tree.ControlTransfer;

public class ImcCONTROL extends ImcExpr {
	
	public final ControlTransfer control;
	
	public ImcCONTROL(ControlTransfer control) {
		this.control = control;
	}

	@Override
	public ImcESEQ linear() {
		return new ImcESEQ(new ImcSEQ(), this);
	}

	@Override
	public void dump(int indent) {
		// TODO
	}

}
