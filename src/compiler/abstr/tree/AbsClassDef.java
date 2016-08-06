package compiler.abstr.tree;

import java.util.Vector;

import compiler.Position;
import compiler.abstr.Visitor;

public class AbsClassDef extends AbsTypeDef {
	
	/** Definicije znotraj razreda */
//	public final AbsDefs definitions;
	public final AbsStmts statements;
	
	/** Konstruktorji */
	public final Vector<AbsFunDef> contrustors = new Vector<>();
	
	public AbsClassDef(String name, Position pos, Vector<AbsStmt> statements) {
		super(pos, name);
		
//		this.definitions = definitions;
		Position start = statements.firstElement().position;
		Position end = statements.lastElement().position;
		this.statements = new AbsStmts(new Position(start, end), statements);

		// add default constructor
		AbsFunDef contructor = new AbsFunDef(pos, 
				name, 
				new Vector<>(), 
				new AbsAtomType(pos, AtomType.VOID), 
				new AbsStmts(pos, statements));
		contrustors.add(contructor);
	}

//	public AbsDefs getDefinitions() {
//		return definitions;
//	}
	
	
	public String getName() {
		return name;
	}

	@Override public void accept(Visitor visitor) { visitor.visit(this); }

}
