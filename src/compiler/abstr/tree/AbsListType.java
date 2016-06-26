package compiler.abstr.tree;

import compiler.*;
import compiler.abstr.*;

/**
 * Opis tabela.
 * 
 * @author sliva
 */
public class AbsListType extends AbsType {

	/** Dolzina seznama. */
	public final int length;

	/** Tip elementa seznama. */
	public final AbsType type;

	/**
	 * Ustvari nov opis tabele.
	 * 
	 * @param pos
	 *            Polozaj stavcne oblike tega drevesa.
	 * @param length
	 *            Dolzina tabele.
	 * @param type
	 *            Tip elementa tabele.
	 */
	public AbsListType(Position pos, int length, AbsType type) {
		super(pos);
		this.length = length;
		this.type = type;
	}
	
	@Override public void accept(Visitor visitor) { visitor.visit(this); }

}
