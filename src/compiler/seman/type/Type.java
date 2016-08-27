package compiler.seman.type;

import compiler.abstr.tree.AtomTypeKind;

/**
 * Data type description.
 */
public abstract class Type {

	/** Vrne konkretno predstavitev tega tipa. */
	public Type actualType() {
		return this;
	}

	/**
	 * Check if types structurarily match.
	 * 
	 * @param type Given type.
	 * @return True if types match, otherwise false
	 */
	public abstract boolean sameStructureAs(Type type);
	
	/**
	 * Check if this type can be casted to given type.
	 * @param type Given type
	 * @return True if this type can be casted to type t
	 */
	public abstract boolean canCastTo(Type t);
	
	/**
	 * Get size in bytes.
	 * 
	 * @return Size of this type in bytes.
	 */
	public abstract int size();
	
	/**
	 * Check if type contains member with given name.
	 * @param name Member name
	 * @return True if type contains name, otherwise false
	 */
	public abstract boolean containsMember(String name);
	
	/**
	 * Check if this type is array type.
	 * @return True if it is, otherwise false.
	 */
	public boolean isArrayType() {
		return this instanceof ArrayType;
	}

	/**
	 * Check if this type is atom (builtin) type.
	 * @return True if it is, otherwise false.
	 */
	public boolean isBuiltinType() {
		return this instanceof AtomType;
	}

	/**
	 * Check if this type is can type.
	 * @return True if it is, otherwise false.
	 */
	public boolean isCanType() {
		return this instanceof CanType;
	}

	/**
	 * Check if this type is class type.
	 * @return True if it is, otherwise false.
	 */
	public boolean isClassType() {
		return this instanceof ClassType;
	}

	/**
	 * Check if this type is enum type.
	 * @return True if it is, otherwise false.
	 */
	public boolean isEnumType() {
		return this instanceof EnumType;
	}

	/**
	 * Check if this type is function type.
	 * @return True if it is, otherwise false.
	 */
	public boolean isFunctionType() {
		return this instanceof FunctionType;
	}

	/**
	 * Check if this type is pointer type.
	 * @return True if it is, otherwise false.
	 */
	public boolean isPointerType() {
		return this instanceof PointerType;
	}

	/**
	 * Check if this type is tuple type.
	 * @return True if it is, otherwise false.
	 */
	public boolean isTupleType() {
		return this instanceof TupleType;
	}

	/**
	 * Check if this type is builtin Int type.
	 * @return True if it is, otherwise false.
	 */
	public boolean isBuiltinIntType() {
		return isBuiltinType() && ((AtomType) this).type == AtomTypeKind.INT;
	}

	/**
	 * Check if this type is builtin Char type.
	 * @return True if it is, otherwise false.
	 */
	public boolean isBuiltinCharType() {
		return isBuiltinType() && ((AtomType) this).type == AtomTypeKind.CHR;
	}

	/**
	 * Check if this type is builtin Double type.
	 * @return True if it is, otherwise false.
	 */
	public boolean isBuiltinDoubleType() {
		return isBuiltinType() && ((AtomType) this).type == AtomTypeKind.DOB;
	}

	/**
	 * Check if this type is builtin String type.
	 * @return True if it is, otherwise false.
	 */
	public boolean isBuiltinStringType() {
		return isBuiltinType() && ((AtomType) this).type == AtomTypeKind.STR;
	}

	/**
	 * Check if this type is builtin Bool type.
	 * @return True if it is, otherwise false.
	 */
	public boolean isBuiltinBoolType() {
		return isBuiltinType() && ((AtomType) this).type == AtomTypeKind.LOG;
	}
	
	/**
	 * Check if this type is builtin Nil type.
	 * @return True if it is, otherwise false.
	 */
	public boolean isBuiltinNilType() {
		return isBuiltinType() && ((AtomType) this).type == AtomTypeKind.NIL;
	}
	
	/**
	 * String representation.
	 */
	public abstract String toString();

}
