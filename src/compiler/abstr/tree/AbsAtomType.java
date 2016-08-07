package compiler.abstr.tree;

import compiler.*;
import compiler.abstr.*;

/**
 * Opis atomarnega tipa.
 * 
 * @author sliva
 */
public class AbsAtomType extends AbsType {
	
	/** Tip. */
	public final AtomType type;

	/**
	 * Ustvari opis konkretnega tipa.
	 * 
	 * @param pos
	 *            Polozaj stavcne oblike tega drevesa.
	 * @param type
	 *            Konkretni tip.
	 */
	public AbsAtomType(Position pos, AtomType type) {
		super(pos);
		this.type = type;
	}
	
	public String toString() {
		switch (type) {
		case INT:
			return "Int";
		case DOB:
			return "Double";
		case CHR:
			return "Char";
		case VOID:
			return "Void";
		case STR:
			return "String";
		case NIL:
			Report.error("Internal error @ AtomType toString");
		default:
			return null;
		}
	}
	
	@Override public void accept(ASTVisitor aSTVisitor) { aSTVisitor.visit(this); }

}
