package compiler.abstr.tree;

import java.util.Vector;

import compiler.*;
import compiler.abstr.*;

/**
 * Conditional expression.
 * 
 * @author sliva
 */
// TODO: this should be AbsStmt
public class AbsIfExpr extends AbsExpr {
	
	/**
	 * Vector holding all of the conditions for this if expression.
	 */
	public final Vector<Condition> conditions;
	
	/**
	 * Code to be executed when all of the conditions fail.
	 */
	public final AbsStmts elseBody;
		
	/**
	 * Create new conditional statement.
	 * 
	 * @param pos
	 *            Position of this node.
	 * @param conditions
	 *            If and else-if code.
	 * @param elseBody
	 *            Code to be executed when conditions fail.
	 */
	public AbsIfExpr(Position pos, Vector<Condition> conditions, AbsStmts elseBody) {
		super(pos);
		this.conditions = conditions;
		this.elseBody = elseBody;
	}

	@Override public void accept(ASTVisitor aSTVisitor) { aSTVisitor.visit(this); }

}
