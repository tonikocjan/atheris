package compiler.seman.type;

/**
 * This type is used for representing class definition, enum definition, ... nodes.
 * @author toni
 *
 */
// TODO: What should this type be called?
public class CanType extends SemType {
	
	public final SemType childType;
	
	public CanType(SemClassType child) {
		this.childType = child;
	}
	
	public CanType(SemEnumType child) {
		this.childType = child;
	}

	@Override
	public boolean sameStructureAs(SemType type) {
		return false;
	}

	@Override
	public boolean canCastTo(SemType t) {
		return false;
	}

	@Override
	public int size() {
		return 0;
	}

	@Override
	public String toString() {
		if (childType instanceof SemClassType)
			return ((SemClassType) childType).getName() + ".Type";
		else
			return ((SemEnumType) childType).definition.name + ".Type";
	}

}
