package compiler.abstr.tree.def;

import compiler.Position;
import compiler.abstr.ASTVisitor;
import compiler.abstr.tree.expr.AbsAtomConstExpr;
import compiler.abstr.tree.expr.AbsVarNameExpr;

public class AbsEnumMemberDef extends AbsDef {
	
	public final AbsVarNameExpr name;
	public AbsAtomConstExpr value;

	public AbsEnumMemberDef(Position pos, AbsVarNameExpr name, AbsAtomConstExpr value) {
		super(pos);
		
		this.name = name;
		this.value = value;
	}

	@Override
	public void accept(ASTVisitor aSTVisitor) {
		aSTVisitor.visit(this);
	}

}
