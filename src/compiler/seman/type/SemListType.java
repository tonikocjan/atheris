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
	public final int size;

	/**
	 * Ustvari nov opis tabelaricnega tipa.
	 * 
	 * @param type
	 *            Tip elementa tabele.
	 * @param size
	 *            Velikost tabele.
	 */
	public SemListType(int size, SemType type) {
		this.type = type;
		this.size = size;
	}

	@Override
	public boolean sameStructureAs(SemType type) {
		if (type.actualType() instanceof SemListType) {
			SemListType arrayType = (SemListType) (type.actualType());
			return (arrayType.size == size)
					&& (arrayType.type.sameStructureAs(this.type));
		} else
			return false;
	}

	@Override
	public String toString() {
		return "LIST(" + size + "," + type.toString() + ")";
	}

	@Override
	public int size() {
		return size * type.size();
	}
}
