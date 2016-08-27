package compiler.abstr.tree.def;

import java.util.LinkedList;

import compiler.Position;
import compiler.abstr.ASTVisitor;
import compiler.abstr.tree.AbsDefs;

public class AbsTupleDef extends AbsTypeDef {
	
	/** Definitions inside tuple */
	public final AbsDefs definitions;

	/**
	 * Create new tuple type definition
	 * @param pos position
	 * @param expressions member expressions
	 * @param names name of each member
	 */
	public AbsTupleDef(Position pos, LinkedList<AbsDef> defs) {
		super(pos, "");

		// TODO: position
		this.definitions = new AbsDefs(pos, defs);
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public void accept(ASTVisitor aSTVisitor) { aSTVisitor.visit(this); }

}
