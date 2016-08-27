package compiler.abstr.tree.def;

import compiler.*;
import compiler.abstr.*;
import compiler.abstr.tree.type.AbsType;

/**
 * Parameter definition..
 * @author toni kocjan
 *
 */
public class AbsParDef extends AbsDef {

	/** Parameter type. */
	public final AbsType type;

	/**
	 * Create new parameter definition..
	 * 
	 * @param pos
	 *            Position.
	 * @param name
	 *            Name.
	 * @param type
	 *            Type.
	 */
	public AbsParDef(Position pos, String name, AbsType type) {
		super(pos, name);
		this.type = type;
	}
	
	@Override public void accept(ASTVisitor aSTVisitor) { aSTVisitor.visit(this); }
}
