package compiler.abstr.tree;

import compiler.Position;
import compiler.abstr.Visitor;

public class AbsStructDef extends AbsTypeDef {
	
	private final AbsDefs definitions;
	
	public AbsDefs getDefinitions() { return definitions; }
	
	public AbsStructDef(String name, Position pos, AbsDefs definitions) {
		super(pos, name);
		
		this.definitions = definitions;
	}
	
	public String getName() {
		return name;
	}

	@Override public void accept(Visitor visitor) { visitor.visit(this); }

}
