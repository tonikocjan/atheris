package compiler.imcode;

import compiler.Report;
import compiler.frames.FrmLabel;

public class ImcMALLOC extends ImcStmt {

	/** Velikost pomnilnika */
	public final int size;
	
	/** Labela data chunka  */
	public final FrmLabel chunkLabel;
	
	public ImcMALLOC(int size, FrmLabel label) {
		this.size = size;
		this.chunkLabel = label;
	}
	
	@Override
	public ImcSEQ linear() {
		ImcSEQ seq = new ImcSEQ();
		seq.stmts.add(this);
		return seq;
	}

	@Override
	public void dump(int indent) {
		Report.dump(indent, "ImcMALLOC");
		indent += 2;
		Report.dump(indent, "Size: " + size);
		Report.dump(indent, "Label: " + chunkLabel.toString());
		indent -= 2;
	}

}
