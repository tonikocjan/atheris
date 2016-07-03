package compiler.seman.type;

import compiler.*;

/**
 * Opis atomarnih podatkovnih tipov.
 * 
 * @author sliva
 */
public class SemAtomType extends SemType {

	public static final int LOG = 0;
	public static final int INT = 1;
	public static final int STR = 2;
	public static final int DOB = 3;
	public static final int CHR = 4;
	public static final int VOID = 5;

	/* Tip. */
	public final int type;

	/**
	 * Ustvari nov opis atomarnega tipa.
	 * 
	 * @param type
	 *            Atomarni tip.
	 */
	public SemAtomType(int type) {
		this.type = type;
	}

	@Override
	public boolean sameStructureAs(SemType type) {
		if (type.actualType() instanceof SemAtomType) {
			SemAtomType atomType = (SemAtomType) (type.actualType());
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
	public boolean canCastTo(SemType t) {
		if (!(t instanceof SemAtomType)) return false;
		// int can be castet to double
		return ((SemAtomType) t).type == DOB && type == INT;
	}
}
