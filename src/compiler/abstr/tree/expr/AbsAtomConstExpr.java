package compiler.abstr.tree.expr;

import compiler.*;
import compiler.abstr.*;
import compiler.abstr.tree.AtomTypeKind;

/**
 * Opis konstante atomarnega tipa.
 * 
 * @author sliva
 */
public class AbsAtomConstExpr extends AbsExpr {
	
	/** Tip. */
	public final AtomTypeKind type;
	
	/** Vrednost. */
	public final String value;

	/**
	 * Ustvari konstanto.
	 * 
	 * @param pos
	 *            Polozaj stavcne oblike tega drevesa.
	 * @param type
	 *            Konkretni tip.
	 * @param value
	 *            Vrednost.
	 */
	public AbsAtomConstExpr(Position pos, AtomTypeKind type, String value) {
		super(pos);
		this.type = type;
		this.value = value;
	}
	
	@Override public void accept(ASTVisitor aSTVisitor) { aSTVisitor.visit(this); }

}
