package compiler.abstr.tree.def;

import compiler.Position;
import compiler.abstr.ASTVisitor;
import compiler.abstr.tree.expr.AbsAtomConstExpr;
import compiler.abstr.tree.expr.AbsVarNameExpr;

/**
 * Enum member definition.
 * @author toni kocjan
 *
 */
public class AbsEnumMemberDef extends AbsDef {
	
	/**
	 * Name for this member.
	 */
	public final AbsVarNameExpr name;
	
	/**
	 * Raw value for this member.
	 */
	public AbsAtomConstExpr value;

	/**
	 * Create new enum definition.
	 * @param pos Position
	 * @param name Variable name
	 * @param value Raw value
	 */
	public AbsEnumMemberDef(Position pos, AbsVarNameExpr name, AbsAtomConstExpr value) {
		super(pos, name.name);
		
		this.name = name;
		this.value = value;
	}

	@Override
	public void accept(ASTVisitor aSTVisitor) {
		aSTVisitor.visit(this);
	}

}
