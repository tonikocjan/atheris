package compiler.frames;

import compiler.abstr.tree.def.AbsFunDef;

/**
 * Dostop do funkcije.
 * 
 * @author toni
 */
public class FrmFunAccess extends FrmAccess {

	/** Opis funkcije.  */
	public final AbsFunDef fun;

	/** Labela funkcije.  */
	public final FrmLabel label;

	/**
	 * Ustvari nov dostop do funkcije.
	 * 
	 * @param var Funkcija.
	 */
	public FrmFunAccess(AbsFunDef fun) {
		this.fun = fun;
		label = FrmLabel.newLabel(fun.name);
	}

	@Override
	public String toString() {
		return "FUN(" + fun.name + ": label=" + label.name() + ")";
	}
}
