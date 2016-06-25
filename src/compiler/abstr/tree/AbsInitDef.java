package compiler.abstr.tree;

import compiler.Position;
import compiler.Report;
import compiler.abstr.Visitor;

public class AbsInitDef extends AbsDef {
	
	/** AbsVarDef or AbsConstDef */
	public final AbsDef definition;
	
	/** Initialization expression */
	public final AbsExpr initialization;

	public AbsInitDef(Position pos, AbsDef definition, AbsExpr initialization) {
		super(pos);
		
		if (!(definition instanceof AbsVarDef || definition instanceof AbsConstDef))
			Report.error(pos, "Internal error @ AbsInitExpr; definition must be"
					+ "AbsVarDef or AbsConstDef");
		
		this.definition = definition;
		this.initialization = initialization;
	}

	@Override
	public void accept(Visitor visitor) {
		visitor.visit(this);
	}

}
