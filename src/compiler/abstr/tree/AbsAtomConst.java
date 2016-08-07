package compiler.abstr.tree;

import compiler.*;
import compiler.abstr.*;

/**
 * Opis konstante atomarnega tipa.
 * 
 * @author sliva
 */
public class AbsAtomConst extends AbsExpr {
	
	/** Tip. */
	public final AtomType type;
	
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
	public AbsAtomConst(Position pos, AtomType type, String value) {
		super(pos);
		this.type = type;
		this.value = value;
	}
	
	@Override public void accept(ASTVisitor aSTVisitor) { aSTVisitor.visit(this); }

}
