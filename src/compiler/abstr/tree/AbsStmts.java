package compiler.abstr.tree;

import java.util.Vector;

import compiler.Position;
import compiler.abstr.ASTVisitor;

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
	 * Vrne izbran stavek.
	 * 
	 * @param index
	 *            Indeks stavka.
	 * @return Definicija na izbranem mestu v seznamu.
	 */
	public AbsStmt stmt(int index) {
		return stmts[index];
	}

	/**
	 * Vrne število stavkov v seznamu.
	 * 
	 * @return Število stavkov v seznamu.
	 */
	public int numStmts() {
		return stmts.length;
	}
	
	public AbsDef findDefinition(String name) {
		for (AbsStmt s : stmts) {
			if (s instanceof AbsVarDef)
				if (((AbsVarDef) s).name.equals(name))
					return (AbsDef) s;
			if (s instanceof AbsFunDef)
				if (((AbsFunDef) s).name.equals(name))
					return (AbsDef) s;
		}
		return null;
	}
	
	
	@Override public void accept(ASTVisitor aSTVisitor) { aSTVisitor.visit(this); }

}
