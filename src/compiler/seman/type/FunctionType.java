package compiler.seman.type;

import java.util.*;

import compiler.abstr.tree.def.AbsFunDef;

/**
 * Function type.
 * 
 * @author toni kocjan
 */
public class FunctionType extends PointerType {

	/** 
	 * Parameter types. 
	 */
	public final Vector<Type> parameterTypes;

	/** 
	 * Result type. 
	 */
	public final Type resultType;

	/**
	 * Definition.
	 */
	public final AbsFunDef functionDefinition;

	/**
	 * Create new function type.
	 * 
	 * @param parTypes
	 *            Parameter types.
	 * @param resultType
	 *            Result type.
	 * @param definition
	 * 			  Function definition.
	 */
	public FunctionType(Vector<Type> parTypes, Type resultType, AbsFunDef definition) {
		this.parameterTypes = parTypes;
		this.resultType = resultType;
		this.functionDefinition = definition;
	}

	/**
	 * Get parameter count.
	 * 
	 * @return Parameter count.
	 */
	public int getNumPars() {
		return parameterTypes.size();
	}

	/**
	 * Get parameter type at given index.
	 * 
	 * @param index
	 *            Indeks of parameter.
	 * @return 
	 * 			  Parameter type.
	 */
	public Type getParType(int index) {
		return parameterTypes.elementAt(index);
	}

	@Override
	public boolean sameStructureAs(Type type) {
		if (type.actualType() instanceof FunctionType) {
			FunctionType funType = (FunctionType) (type.actualType());
			if (this.getNumPars() != funType.getNumPars())
				return false;
			for (int par = 0; par < getNumPars(); par++)
				if (!this.getParType(par).sameStructureAs(funType.getParType(par)))
					return false;
			if (!this.resultType.sameStructureAs(funType.resultType))
				return false;
			return true;
		}
		return false;
	}
	
	@Override
	public String toString() {
		String str = "";
		str += "(";
		for (Type t : parameterTypes)
			str += t.toString() + (t == parameterTypes.lastElement() ? "" : ",") ;
		String res = resultType == null ? "?" : resultType.toString();
		str += ") -> " + res;
		return str;
	}

	@Override
	public int size() {
		int size = 4;
		for (Type t : parameterTypes)
			size += t.size();
		
		return Math.max(resultType.size(), size);
	}

	@Override
	public boolean canCastTo(Type t) {
		return false;
	}

	@Override
	public boolean containsMember(String name) {
		return false;
	}

}
