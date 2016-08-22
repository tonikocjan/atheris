package compiler.seman.type;

/**
 * Opis tabelaricnega tipa.
 * 
 * @author sliva
 */
public class ArrayType extends PointerType {

	/** Tip elementa. */
	public final Type type;

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
	public ArrayType(Type type, int count) {
		this.type = type;
		this.count = count;
	}

	@Override
	public boolean sameStructureAs(Type type) {
		if (type.actualType() instanceof ArrayType) {
			ArrayType listType = (ArrayType) (type.actualType());
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
	public boolean canCastTo(Type t) {
		return false;
	}
}
