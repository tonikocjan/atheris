package compiler.abstr.tree.stmt;

import java.util.Vector;

import compiler.Position;
import compiler.abstr.ASTVisitor;
import compiler.abstr.tree.expr.AbsExpr;

public class AbsSwitchStmt extends AbsConditionalStmt {
	
	/**
	 * Expression to be compared.
	 * Must be typed as Int, String (or Enum)
	 */
	public final AbsExpr subjectExpr;
	
	/**
	 * Cases.
	 */
	public final Vector<AbsCaseStmt> cases;

	/**
	 * 
	 * @param pos
	 */
	public AbsSwitchStmt(Position pos, AbsExpr subjectExpr, Vector<AbsCaseStmt> cases) {
		super(pos);

		this.subjectExpr = subjectExpr;
		this.cases = cases;
	}

	@Override public void accept(ASTVisitor aSTVisitor) { aSTVisitor.visit(this); }

}
