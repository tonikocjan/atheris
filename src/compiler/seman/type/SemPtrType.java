package compiler.seman.type;

public class SemPtrType extends SemType {
	
	/** Tip na katerega kazalec kaže. */
	public final SemType type;
	
	/** Velikost seznama (0, če ne kaže na seznam) */
	public final int count;
	
	public SemPtrType(SemType type) {
		this.type = type;
		this.count = 0;
	}
	
	public SemPtrType(SemType type, int count) {
		this.type = type;
		this.count = count;
	}

	@Override
	public boolean sameStructureAs(SemType type) {
		if (!(type instanceof SemPtrType))
			return false;
		if (((SemPtrType)type).type.sameStructureAs(this.type))
			return true;
		return false;
	}
	
	@Override
	public String toString() {
		return "PTR(" + type.toString() + ")";
	}

	@Override
	public int size() {
		return 4;
	}

	@Override
	public boolean canCastTo(SemType t) {
		return false;
	}

}
