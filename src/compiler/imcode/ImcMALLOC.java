package compiler.imcode;

import compiler.Report;
import compiler.frames.FrmLabel;

public class ImcMALLOC extends ImcExpr {

	/** Velikost pomnilnika */
	public final int size;
	
	public ImcMALLOC(int size) {
		this.size = size;
	}

	@Override
	public void dump(int indent) {
		Report.dump(indent, "ImcMALLOC");
		Report.dump(indent + 2, "Size: " + size);
	}

	@Override
	public ImcESEQ linear() {
		ImcESEQ eseq = new ImcESEQ(new ImcSEQ(), this);
		return eseq;
	}

}
