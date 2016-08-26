package compiler.abstr.tree.def;

import java.util.LinkedList;

import compiler.Position;
import compiler.abstr.ASTVisitor;
import compiler.abstr.tree.AbsDefs;
import compiler.abstr.tree.AbsStmt;
import compiler.abstr.tree.AbsStmts;
import compiler.abstr.tree.AtomTypeEnum;
import compiler.abstr.tree.type.AbsAtomType;

public class AbsClassDef extends AbsTypeDef {
	
	/** Definitions inside class */
	public final AbsDefs definitions;
	
	/** Constructors (initializers) */
	public final LinkedList<AbsFunDef> contrustors = new LinkedList<>();
	
	/**
	 * Create new class definition.
	 * @param name
	 * @param pos
	 * @param statements
	 */
	public AbsClassDef(String name, Position pos, LinkedList<AbsDef> definitions, 
			LinkedList<AbsStmt> initExpressions) {
		super(pos, name);
		
		if (definitions.size() > 0) {
			Position start = definitions.getFirst().position;
			Position end = definitions.getLast().position;
			this.definitions = new AbsDefs(new Position(start, end), definitions);
		}
		else
			this.definitions = new AbsDefs(pos, definitions);
		
		// set this definition as parent for all member definitions
		for (AbsDef def : this.definitions.definitions)
			def.setParentDefinition(this);
		
		// add default constructor
		AbsFunDef contructor = new AbsFunDef(pos, 
				name,
				new LinkedList<>(), 
				new AbsAtomType(pos, AtomTypeEnum.VOID), 
				new AbsStmts(pos, initExpressions));
		contrustors.add(contructor);
	}
	
	public String getName() {
		return name;
	}

	@Override public void accept(ASTVisitor aSTVisitor) { aSTVisitor.visit(this); }

}
