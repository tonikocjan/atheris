package compiler.imcode;

import compiler.*;

/**
 * Return.
 * 
 * @author toni
 */
public class ImcRETURN extends ImcExpr {

	/** Izraz.  */
	public ImcExpr expr = null;

	/** Ustvari nov prenos.
	 * 
	 * @param dst Ponor.
	 * @param src Izvor.
	 */
	public ImcRETURN(ImcExpr expr) {
		this.expr = expr;
	}

	@Override
	public void dump(int indent) {
		Report.dump(indent, "RETURN");
		if (expr != null) expr.dump(indent + 2);
	}

	@Override
	public ImcESEQ linear() {
		ImcESEQ lin = expr.linear();
		lin.expr = new ImcMEM(lin.expr);
		return lin;
	}

}
