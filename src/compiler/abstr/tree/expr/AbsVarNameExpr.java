package compiler.abstr.tree.expr;

import compiler.*;
import compiler.abstr.*;

/**
 * Ime spremenljivke v izrazu.
 * 
 * @author sliva
 */
public class AbsVarNameExpr extends AbsExpr {
	
	/** Ime spremenljivke. */
	public final String name;

	/**
	 * Ustvari nov opis imena spremenljivke v izrazu.
	 * 
	 * @param pos
	 *            Polozaj stavcne oblike tega drevesa.
	 * @param name
	 *            Ime spremenljivke.
	 */
	public AbsVarNameExpr(Position pos, String name) {
		super(pos);
		this.name = name;
	}

	@Override public void accept(ASTVisitor aSTVisitor) { aSTVisitor.visit(this); }

}