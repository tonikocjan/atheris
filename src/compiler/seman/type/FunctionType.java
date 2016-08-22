package compiler.seman.type;

import java.util.*;

/**
 * Opis funkcijskega tipa.
 * 
 * @author sliva
 */
public class FunctionType extends PointerType {

	/** Tipi parametrov. */
	public final Vector<Type> parameterTypes;

	/** Tipa rezultata. */
	public final Type resultType;

	/**
	 * Ustvari nov opis funkcijskega tipa.
	 * 
	 * @param parTypes
	 *            Tipi parametrov.
	 * @param resultType
	 *            Tip rezultata.
	 */
	public FunctionType(Vector<Type> parTypes, Type resultType) {
		this.parameterTypes = parTypes;
		this.resultType = resultType;
	}

	/**
	 * Vrne stevilo parametrov.
	 * 
	 * @return Stevilo parametrov.
	 */
	public int getNumPars() {
		return parameterTypes.size();
	}

	/**
	 * Vrne tip zahtevanega parametra.
	 * 
	 * @param index
	 *            Indeks zahtevanega parametra.
	 * @return Tip zahtevanega parametra.
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
		int input = 4;
		for (Type t : parameterTypes)
			input += t.size();
		
		return Math.max(resultType.size(), input);
	}

	@Override
	public boolean canCastTo(Type t) {
		return false;
	}

}
