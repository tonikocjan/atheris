package compiler.abstr.tree;

import compiler.*;
import compiler.abstr.*;

/**
 * Opis atomarnega tipa.
 * 
 * @author sliva
 */
public class AbsAtomType extends AbsType {

	public static final int LOG = 0;
	public static final int INT = 1;
	public static final int STR = 2;
	public static final int DOB = 3;
	public static final int CHR = 4;
	public static final int VOID = 5;
	
	/** Tip. */
	public final int type;

	/**
	 * Ustvari opis konkretnega tipa.
	 * 
	 * @param pos
	 *            Polozaj stavcne oblike tega drevesa.
	 * @param type
	 *            Konkretni tip.
	 */
	public AbsAtomType(Position pos, int type) {
		super(pos);
		this.type = type;
	}
	
	@Override public void accept(Visitor visitor) { visitor.visit(this); }

}
