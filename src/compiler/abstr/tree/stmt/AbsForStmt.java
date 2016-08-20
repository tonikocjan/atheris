package compiler.abstr.tree.stmt;

import compiler.*;
import compiler.abstr.*;
import compiler.abstr.tree.AbsStmts;
import compiler.abstr.tree.expr.AbsExpr;
import compiler.abstr.tree.expr.AbsVarNameExpr;

/**
 * Zanka z eksplicitnim stevcem.
 * 
 * @author sliva
 */
public class AbsForStmt extends AbsConditionalStmt {
	
//	/** Stevec. */
//	public final AbsVarName count;
	
	/** Vrednost v tabeli collection[count] */
	public final AbsVarNameExpr iterator;
	
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
	public AbsForStmt(Position pos, AbsVarNameExpr count, AbsExpr collection, AbsStmts body) {
		super(pos);
		this.iterator = count;
		this.collection = collection;
		this.body = body;
	}

	@Override public void accept(ASTVisitor aSTVisitor) { aSTVisitor.visit(this); }

}
