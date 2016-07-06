package compiler.seman.type;

import java.util.*;

/**
 * Opis funkcijskega tipa.
 * 
 * @author sliva
 */
public class SemFunType extends SemType {

	/** Tipi parametrov. */
	public final SemType parTypes[];

	/** Tipa rezultata. */
	public final SemType resultType;

	/**
	 * Ustvari nov opis funkcijskega tipa.
	 * 
	 * @param parTypes
	 *            Tipi parametrov.
	 * @param resultType
	 *            Tip rezultata.
	 */
	public SemFunType(Vector<SemType> parTypes, SemType resultType) {
		this.parTypes = new SemType[parTypes.size()];
		for (int par = 0; par < parTypes.size(); par++)
			this.parTypes[par] = parTypes.elementAt(par);
		this.resultType = resultType;
	}

	/**
	 * Vrne stevilo parametrov.
	 * 
	 * @return Stevilo parametrov.
	 */
	public int getNumPars() {
		return parTypes.length;
	}

	/**
	 * Vrne tip zahtevanega parametra.
	 * 
	 * @param index
	 *            Indeks zahtevanega parametra.
	 * @return Tip zahtevanega parametra.
	 */
	public SemType getParType(int index) {
		return parTypes[index];
	}

	@Override
	public boolean sameStructureAs(SemType type) {
		if (type.actualType() instanceof SemFunType) {
			SemFunType funType = (SemFunType) (type.actualType());
			if (this.getNumPars() != funType.getNumPars())
				return false;
			for (int par = 0; par < getNumPars(); par++)
				if (!this.getParType(par).sameStructureAs(
						funType.getParType(par)))
					return false;
			if (!this.resultType.sameStructureAs(funType.resultType))
				return false;
			return true;
		} else
			return false;
	}
	
	@Override
	public String toString() {
		String str = "";
		str += "(";
		for (int par = 0; par < parTypes.length; par++)
			str += (par > 0 ? "," : "") + parTypes[par].toString();
		str += ") -> " + resultType.toString();
		return str;
	}

	@Override
	public int size() {
		int input = 4;
		for (SemType t : parTypes)
			input += t.size();
		
		return Math.max(resultType.size(), input);
	}

	@Override
	public boolean canCastTo(SemType t) {
		return false;
	}

}
