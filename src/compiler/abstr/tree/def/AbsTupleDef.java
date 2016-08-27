package compiler.abstr.tree.def;

import java.util.LinkedHashMap;
import java.util.LinkedList;

import compiler.Position;
import compiler.abstr.ASTVisitor;
import compiler.abstr.tree.AbsExprs;
import compiler.abstr.tree.expr.AbsExpr;

public class AbsTupleDef extends AbsDef {
	
	/**
	 * Members of this tuple definition.
	 */
	public final LinkedHashMap<String, AbsExpr> members;
	

	/**
	 * Create new tuple definition
	 * @param pos position
	 * @param expressions member expressions
	 * @param names name of each member
	 */
	public AbsTupleDef(Position pos, 
			LinkedList<AbsExpr> expressions, LinkedList<String> names) {
		super(pos);

		members = new LinkedHashMap<>(names.size());
		for (int i = 0; i < names.size(); i++)
			members.put(names.get(i), expressions.get(i));
	}

	/**
	 * Check if this tuple contains given name.
	 * @param name name of member
	 * @return true if it does
	 */
	public boolean containsName(String name) {
		return members.containsKey(name);
	}
	
	@Override
	public String getName() {
		return null;
	}

	@Override
	public void accept(ASTVisitor aSTVisitor) { aSTVisitor.visit(this); }

}
