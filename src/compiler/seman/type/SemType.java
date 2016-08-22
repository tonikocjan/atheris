package compiler.seman.type;

/**
 * Opis podatkovnega tipa.
 */
public abstract class SemType {

	/** Vrne konkretno predstavitev tega tipa. */
	public SemType actualType() {
		return this;
	}

	/**
	 * Ugotovi, ali je podani tip strukturno enak temu tipu.
	 * 
	 * @param type
	 *            Podani tip.
	 * @return Ali je podani tip strukturno enak temu tipu.
	 */
	public abstract boolean sameStructureAs(SemType type);
	
	/**
	 * @param type
	 * @return true, if this type can be castet to type t
	 */
	public abstract boolean canCastTo(SemType t);
	
	/**
	 * Vrne velikost podatkovnega tipa v bytih.
	 * 
	 * @return Velikost podatkovnega tipa v bytih.
	 */
	public abstract int size();
	
	/**
	 * Convert type to string.
	 */
	public abstract String toString();

}
