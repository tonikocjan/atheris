package compiler.abstr.tree;

import java.util.LinkedList;

import compiler.Position;
import compiler.abstr.ASTVisitor;
import compiler.abstr.tree.def.AbsDef;

public class AbsStmts extends AbsTree {

	/** Statements. */
	public final LinkedList<AbsStmt> statements;
	
	public AbsStmts(Position position, LinkedList<AbsStmt> absStmts) {
		super(position);
		
		this.statements = absStmts;
	}

	public AbsDef findDefinitionForName(String name) {
		for (AbsStmt s : statements) {
			if (s instanceof AbsDef)
				if (((AbsDef) s).getName().equals(name))
					return (AbsDef) s;
		}
		return null;
	}
	
	@Override public void accept(ASTVisitor aSTVisitor) { aSTVisitor.visit(this); }

}
