package compiler.abstr.tree.def;

import java.util.*;

import compiler.*;
import compiler.abstr.*;
import compiler.abstr.tree.AbsStmts;
import compiler.abstr.tree.type.AbsType;

/**
 * Definicija funckije.
 * 
 * @author sliva
 */
public class AbsFunDef extends AbsDef {

	/** Ime funkcije. */
	public final String name;
	
	/** Seznam parametrov. */
	private final AbsParDef pars[];

	/** Opis tipa rezultata funkcije. */
	public final AbsType type;
	
	/** Jedro funkcije. */
	public final AbsStmts func;

	/**
	 * Ustvari novo definicijo funckije.
	 * 
	 * @param pos
	 *            Polozaj stavcne oblike tega drevesa.
	 * @param name
	 *            Ime funkcije.
	 * @param pars
	 *            Seznam parametrov.
	 * @param type
	 *            Opis tipa rezultata funkcije.
	 * @param stmts
	 *            Jedro funkcije.
	 */
	public AbsFunDef(Position pos, String name, Vector<AbsParDef> pars, AbsType type, AbsStmts stmts) {
		super(pos);
		this.name = name;
		this.pars = new AbsParDef[pars.size()];
		for (int par = 0; par < pars.size(); par++)
			this.pars[par] = pars.elementAt(par);
		this.type = type;
		this.func = stmts;
	}

	/**
	 * Vrne izbrani parameter.
	 * 
	 * @param index
	 *            Indeks parametra.
	 * @return Parameter na izbranem mestu.
	 */
	public AbsParDef par(int index) {
		return pars[index];
	}

	/**
	 * Vrne stevilo parametrov funkcije.
	 * 
	 * @return Stevilo parametrov funkcije.
	 */
	public int numPars() {
		return pars.length;
	}

	@Override public void accept(ASTVisitor aSTVisitor) { aSTVisitor.visit(this); }

	@Override
	public String getName() {
		return name;
	}
}
