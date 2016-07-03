package compiler.seman.type;

/**
 * Opis tabelaricnega tipa.
 * 
 * @author sliva
 */
public class SemListType extends SemType {

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
	public SemListType(int count, SemType type) {
		this.type = type;
		this.count = count;
	}

	@Override
	public boolean sameStructureAs(SemType type) {
		if (type.actualType() instanceof SemListType) {
			SemListType arrayType = (SemListType) (type.actualType());
			return (arrayType.count == count)
					&& (arrayType.type.sameStructureAs(this.type));
		} else
			return false;
	}

	@Override
	public String toString() {
		return "LIST(" + count + "," + type.toString() + ")";
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
