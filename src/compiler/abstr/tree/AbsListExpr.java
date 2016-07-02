package compiler.abstr.tree;

import java.util.Vector;

import compiler.Position;
import compiler.abstr.Visitor;

public class AbsListExpr extends AbsExpr {
	
	public final Vector<AbsExpr> expressions;

	public AbsListExpr(Position pos, Vector<AbsExpr> expressions) {
		super(pos);
		this.expressions = expressions;
	}

	@Override
	public void accept(Visitor visitor) {
		visitor.visit(this);
	}

}
