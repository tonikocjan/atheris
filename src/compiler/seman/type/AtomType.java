package compiler.seman.type;

import compiler.*;
import compiler.abstr.tree.AtomTypeEnum;

/**
 * Opis atomarnih podatkovnih tipov.
 * 
 * @author sliva
 */
public class AtomType extends Type {

	/* Tip. */
	public final AtomTypeEnum type;

	/**
	 * Ustvari nov opis atomarnega tipa.
	 * 
	 * @param type
	 *            Atomarni tip.
	 */
	public AtomType(AtomTypeEnum type) {
		this.type = type;
	}

	@Override
	public boolean sameStructureAs(Type type) {
		if (type.actualType() instanceof AtomType) {
			AtomType atomType = (AtomType) (type.actualType());
			return this.type == atomType.type;
		} else
			return false;
	}

	@Override
	public String toString() {
		switch (type) {
		case LOG: return "Bool";
		case INT: return "Int";
		case STR: return "String";
		case VOID: return "Void";
		case DOB: return "Double";
		case CHR: return "Char";
		case NIL: return "nil";
		}
		Report.error("Internal error :: compiler.seman.type.SemAtomType.toString()");
		return "";
	}

	@Override
	public int size() {
		switch (type) {
		case LOG:
		case INT:
		case STR:
			return 4;
		case VOID:
		case NIL:
			return 0;
		case CHR:
			return 1;
		case DOB:
			return 8;
		}
		Report.error("Internal error :: compiler.seman.type.SemAtomType.size()");
		return 0;
	}

	@Override
	public boolean canCastTo(Type t) {
		if (!(t instanceof AtomType)) return false;
		// int can be castet to double
		return ((AtomType) t).type == AtomTypeEnum.DOB && type == AtomTypeEnum.INT;
	}
}
