package compiler.abstr.tree;

import java.util.*;

import compiler.*;
import compiler.abstr.*;
import compiler.abstr.tree.expr.AbsExpr;

/**
 * Opis izrazov.
 * 
 * @author toni kocjan
 */
public class AbsExprs extends AbsExpr {

	/** Seznam izrazov. */
	public final LinkedList<AbsExpr> expressions;

	/**
	 * Ustvari nov opis izrazov.
	 * 
	 * @param pos
	 *            Polozaj stavcne oblike tega drevesa.
	 * @param exprs
	 *            Seznam izrazov.
	 */
	public AbsExprs(Position pos, LinkedList<AbsExpr> exprs) {
		super(pos);
		
		this.expressions = exprs;
	}

	@Override public void accept(ASTVisitor aSTVisitor) { aSTVisitor.visit(this); }

}
