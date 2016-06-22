package compiler.abstr.tree;

import compiler.*;
import compiler.abstr.*;

/**
 * Dolgi pogojni stavek.
 * 
 * @author sliva
 */
public class AbsIfThenElse extends AbsExpr {
	
	/** Pogoj. */
	public final AbsExpr cond;
	
	/** Pozitivna veja. */
	public final AbsStmts thenBody;
	
	/** Negativna veja. */
	public final AbsStmts elseBody;
	
	/**
	 * Ustvari nov dolgi pogojni stavek.
	 * 
	 * @param pos
	 *            Polozaj stavcne oblike tega drevesa.
	 * @param cond
	 *            Pogoj.
	 * @param thenBody
	 *            Pozitivna veja.
	 * @param elseBody
	 *            Negativna veja.
	 */
	public AbsIfThenElse(Position pos, AbsExpr cond, AbsStmts thenBody, AbsStmts elseBody) {
		super(pos);
		this.cond = cond;
		this.thenBody = thenBody;
		this.elseBody = elseBody;
	}

	@Override public void accept(Visitor visitor) { visitor.visit(this); }

}
