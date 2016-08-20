package compiler.abstr.tree.type;

import java.util.Vector;

import compiler.Position;
import compiler.abstr.ASTVisitor;

public class AbsFunType extends AbsType {
	
	/** Types of paramaters */
	public final Vector<AbsType> parameterTypes;
	
	/** Return type */
	public final AbsType returnType;
	
	public AbsFunType(Position pos, Vector<AbsType> parameters, AbsType returnType) {
		super(pos);
		this.parameterTypes = parameters;
		this.returnType = returnType;
	}
	
	public int numPars() {
		return parameterTypes.size();
	}
	
	public AbsType type(int t) {
		return parameterTypes.elementAt(t);
	}
	
	public String toString() {
		String str = "(";
		for (AbsType p : parameterTypes) {
			str += p.toString();
			if (p != parameterTypes.lastElement()) str += ", ";
		}
		str += ") -> " + returnType.toString();
		return str;
	}

	@Override
	public void accept(ASTVisitor aSTVisitor) {
		aSTVisitor.visit(this);
	}

}
