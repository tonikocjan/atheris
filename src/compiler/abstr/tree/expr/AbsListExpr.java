package compiler.abstr.tree.expr;

import java.util.Vector;

import compiler.Position;
import compiler.abstr.ASTVisitor;

public class AbsListExpr extends AbsExpr {
	
	public final Vector<AbsExpr> expressions;

	public AbsListExpr(Position pos, Vector<AbsExpr> expressions) {
		super(pos);
		this.expressions = expressions;
	}

	@Override
	public void accept(ASTVisitor aSTVisitor) {
		aSTVisitor.visit(this);
	}

}
