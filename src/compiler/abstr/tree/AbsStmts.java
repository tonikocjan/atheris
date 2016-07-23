package compiler.abstr.tree;

import java.util.Vector;

import compiler.Position;
import compiler.abstr.Visitor;

public class AbsStmts extends AbsTree {

	/** Elementi seznama definicij. */
	public AbsStmt stmts[];
	
	public AbsStmts(Position position, Vector<AbsStmt> absStmts) {
		super(position);
		
		this.stmts = new AbsStmt[absStmts.size()];
		for (int def = 0; def < absStmts.size(); def++)
			this.stmts[def] = absStmts.elementAt(def);
	}

	/**
	 * Vrne izbrano stvek.
	 * 
	 * @param index
	 *            Indeks definicije.
	 * @return Definicija na izbranem mestu v seznamu.
	 */
	public AbsStmt stmt(int index) {
		return stmts[index];
	}

	/**
	 * Vrne stevilo definicij v seznamu.
	 * 
	 * @return Stevilo definicij v seznamu.
	 */
	public int numStmts() {
		return stmts.length;
	}
	
	
	@Override public void accept(Visitor visitor) { visitor.visit(this); }

}
