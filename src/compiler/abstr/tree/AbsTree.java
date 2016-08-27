package compiler.abstr.tree;

import compiler.*;
import compiler.abstr.*;

/**
 * Abstract syntax tree node.
 * This is base class for all types of nodes.
 * 
 * @author toni kocjan
 */
public abstract class AbsTree {

	/**
	 * Position of this node.
	 */
	public final Position position;

	/**
	 * Create new abstract syntax tree.
	 * 
	 * @param pos
	 *            Position.
	 */
	public AbsTree(Position pos) {
		this.position = pos;
	}

	public abstract void accept(ASTVisitor aSTVisitor);

}
