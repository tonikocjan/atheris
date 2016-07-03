package compiler.abstr.tree;

import compiler.*;
import compiler.abstr.*;

/**
 * Opis seznama.
 * 
 * @author sliva
 */
public class AbsListType extends AbsType {

	/** Dolzina seznama. */
	public final int count;

	/** Tip elementa seznama. */
	public final AbsType type;

	/**
	 * Ustvari nov opis tabele.
	 * 
	 * @param pos
	 *            Polozaj stavcne oblike tega drevesa.
	 * @param length
	 *            Dolzina seznama.
	 * @param type
	 *            Tip elementa tabele.
	 */
	public AbsListType(Position pos, int count, AbsType type) {
		super(pos);
		this.count = count;
		this.type = type;
	}
	
	@Override public void accept(Visitor visitor) { visitor.visit(this); }

}
