package compiler.seman.type;

/**
 * Opis tabelaricnega tipa.
 * 
 * @author sliva
 */
public class SemListType extends SemPtrType {

	/** Tip elementa. */
	public final SemType type;

	/** Velikost tabele. */
	public final int count;

	/**
	 * Ustvari nov opis tabelaricnega tipa.
	 * 
	 * @param type
	 *            Tip elementa tabele.
	 * @param count
	 *            Velikost tabele.
	 */
	public SemListType(SemType type, int count) {
		this.type = type;
		this.count = count;
	}

	@Override
	public boolean sameStructureAs(SemType type) {
		if (type.actualType() instanceof SemListType) {
			SemListType listType = (SemListType) (type.actualType());
			return (listType.type.sameStructureAs(this.type));
		}
		return false;
	}

	@Override
	public String toString() {
		return "LIST(" + type.toString() + ")";
	}

	@Override
	public int size() {
		return count * type.size();
	}

	@Override
	public boolean canCastTo(SemType t) {
		return false;
	}
}
