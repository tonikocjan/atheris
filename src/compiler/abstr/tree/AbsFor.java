package compiler.abstr.tree;

import compiler.*;
import compiler.abstr.*;

/**
 * Zanka z eksplicitnim stevcem.
 * 
 * @author sliva
 */
public class AbsFor extends AbsExpr {
	
	/** Stevec. */
	public final AbsVarName count;
	
	/** Zbirka preko katire iteriramo. */
	public final AbsExpr collection;
	
	/** Jedro stavka. */
	public final AbsStmts body;
	
	/**
	 * Ustvari zanko z eksplicitnim stevcem.
	 * 
	 * @param pos
	 *            Polozaj stavcne oblike tega drevesa.
	 * @param count
	 *            Stevec.
	 * @param collection
	 *            Zbirka.
	 * @param body
	 *            Jedro zanke.
	 */
	public AbsFor(Position pos, AbsVarName count, AbsExpr collection, AbsStmts body) {
		super(pos);
		this.count = count;
		this.collection = collection;
		this.body = body;
	}

	@Override public void accept(Visitor visitor) { visitor.visit(this); }

}
