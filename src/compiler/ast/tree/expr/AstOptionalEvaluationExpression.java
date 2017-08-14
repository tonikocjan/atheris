package compiler.ast.tree.expr;

import compiler.Position;
import compiler.ast.ASTVisitor;

public class AstOptionalEvaluationExpression extends AstExpression {

	public final AstExpression subExpr;

	public AstOptionalEvaluationExpression(Position pos, AstExpression subExpr) {
		super(pos);
		
		this.subExpr = subExpr;
	}

	@Override
	public void accept(ASTVisitor aSTVisitor) {
		aSTVisitor.visit(this);
	}

}
