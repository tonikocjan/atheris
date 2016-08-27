package compiler.seman.type;

/**
 * This type is used for representing class definition, enum definition, ... nodes.
 * @author toni
 *
 */
// TODO: What should this type be called?
public class CanType extends Type {
	
	public final Type childType;
	
	public CanType(ClassType child) {
		this.childType = child;
	}
	
	public CanType(EnumType child) {
		this.childType = child;
	}

	@Override
	public boolean sameStructureAs(Type type) {
		if (type instanceof CanType)
			return ((CanType) type).childType.sameStructureAs(childType);
		return false;
	}

	@Override
	public boolean canCastTo(Type t) {
		return false;
	}

	@Override
	public int size() {
		return 0;
	}

	@Override
	public String toString() {
		if (childType instanceof ClassType)
			return ((ClassType) childType).getName() + ".Type";
		else
			return ((EnumType) childType).enumDefinition.name + ".Type";
	}

	@Override
	public boolean containsMember(String name) {
		return childType.containsMember(name);
	}

}
