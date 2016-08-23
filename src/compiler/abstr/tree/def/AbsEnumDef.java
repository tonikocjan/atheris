package compiler.abstr.tree.def;

import java.util.LinkedList;

import compiler.Position;
import compiler.abstr.ASTVisitor;
import compiler.abstr.tree.type.AbsAtomType;

public class AbsEnumDef extends AbsTypeDef {
	
	/**
	 * Enumeration definitions.
	 */
	public final LinkedList<AbsEnumMemberDef> definitions;
	
	/**
	 * Type of raw values.
	 * If null, definitions dont have raw values.
	 */
	public final AbsAtomType type;

	/**
	 * Construct enum definition.
	 * @param pos position
	 * @param name name
	 * @param definitions enum definitions
	 * @param type type for each definitions' raw value
	 */
	public AbsEnumDef(Position pos, String name, 
			LinkedList<AbsEnumMemberDef> definitions, AbsAtomType type) {
		super(pos, name);
		
		this.definitions = definitions;
		this.type = type;
	}

	@Override
	public void accept(ASTVisitor aSTVisitor) {
		aSTVisitor.visit(this);
	}

}
