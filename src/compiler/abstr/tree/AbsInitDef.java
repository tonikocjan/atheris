package compiler.abstr.tree;

import compiler.Position;
import compiler.Report;
import compiler.abstr.Visitor;

public class AbsInitDef extends AbsDef {
	
	/** AbsVarDef or AbsConstDef */
	public final AbsDef definition;
	
	/**  */
	public final AbsVarName name;
	
	/** Initialization expression */
	public final AbsExpr initialization;

	/**
	 * Contruct AbsInit defition
	 * @param pos position of definition
	 * @param definition variable which is being initialized
	 * @param initialization expression to initialize variable
	 * @param name name of the variable
	 */
	public AbsInitDef(Position pos, AbsDef definition, AbsExpr initialization, String name) {
		super(pos);
		
		if (!(definition instanceof AbsVarDef || definition instanceof AbsConstDef))
			Report.error(pos, "Internal error @ AbsInitExpr; definition must be"
					+ "AbsVarDef or AbsConstDef");
		
		this.definition = definition;
		this.initialization = initialization;
		this.name = new AbsVarName(pos, name);
	}

	@Override
	public void accept(Visitor visitor) {
		visitor.visit(this);
	}

}
