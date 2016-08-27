package compiler.abstr.tree.def;

import compiler.*;
import compiler.abstr.*;
import compiler.abstr.tree.VisibilityKind;
import compiler.abstr.tree.type.AbsType;

/**
 * Variable definition.
 * 
 * @author sliva
 */
public class AbsVarDef extends AbsDef {

	/** Variable type. */
	public final AbsType type;
	
	/** Is this variable a constant. */
	public final boolean isConstant;
	
	/** Is variable public / private (used for class members) */
	public final VisibilityKind visibilityKind;

	/**
	 * Create new variable definition.
	 * 
	 * @param pos
	 *            Position.
	 * @param name
	 *            Name.
	 * @param type
	 *            Type.
	 */
	public AbsVarDef(Position pos, String name, AbsType type) {
		this(pos, name, type, false);
	}
	
	/**
	 * Create new variable definition.
	 * 
	 * @param pos
	 *            Position.
	 * @param name
	 *            Name.
	 * @param type
	 *            Type.
	 * @param constant
	 * 			  Is this variable a constant.
	 */
	public AbsVarDef(Position pos, String name, AbsType type, boolean constant) {
		this(pos, name, type, constant, VisibilityKind.Public);
	}
	
	/**
	 * Create new variable definition.
	 * 
	 * @param pos
	 *            Position.
	 * @param name
	 *            Name.
	 * @param type
	 *            Type.
	 * @param constant
	 * 			  Is this variable a constant.
	 * @param visibilityKind
	 * 			  Visibility.
	 */
	public AbsVarDef(Position pos, String name, 
			AbsType type, boolean constant, VisibilityKind visibilityKind) {
		super(pos, name);
		
		this.type = type;
		this.isConstant = constant;
		this.visibilityKind = visibilityKind;
	}


	@Override public void accept(ASTVisitor aSTVisitor) { aSTVisitor.visit(this); }
}
