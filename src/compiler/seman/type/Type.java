/**
 * Copyright 2016 Toni Kocjan
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package compiler.seman.type;

import compiler.abstr.tree.AtomTypeKind;
import compiler.abstr.tree.def.AbsDef;

/**
 * Data type description.
 */
public abstract class Type {

    // MARK: - Type descriptor
    // Current descriptor available.
    private static int TYPE_DESCRIPTOR = 0;

    /**
     * Get type descriptor.
     * @return Current descriptor
     */
    private int getTypeDescriptor() {
        return TYPE_DESCRIPTOR++;
    }

    /** Descriptor for this type (automatically assigned when new type is instantiated)  */
    public final int descriptor = getTypeDescriptor();


    // MARK: - Methods
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
	public abstract boolean canCastTo(Type type);
	
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
	 * Find and return member for given name.
	 * @param name Member name
	 * @return Member definition or Null
	 */
	public abstract AbsDef findMemberForName(String name);
	
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
	public boolean isAtomType() {
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
    public boolean isObjectType() {
        return this instanceof ObjectType;
    }

	/**
	 * Check if this type is class type.
	 * @return True if it is, otherwise false.
	 */
	public boolean isClassType() {
		return this instanceof ClassType;
	}

	/**
     * Check if this type is interface type.
     * @return True if it is, otherwise false.
     */
    public boolean isInterfaceType() {
        return this instanceof InterfaceType;
    }

    /**
     * Check if this type is class type.
     * @return True if it is, otherwise false.
     */
    public boolean isStructType() {
        return this instanceof StructType;
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
	public boolean isReferenceType() {
		return this instanceof ReferenceType;
	}

    /**
     * Check if this type is pointer type.
     * @return True if it is, otherwise false.
     */
    public boolean isValueType() {
        return this instanceof ValueType;
    }

	/**
	 * Check if this type is optional type.
	 * @return True if it is, otherwise false.
	 */
	public boolean isOptionalType() {
		return this instanceof OptionalType;
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
		return isAtomType() && ((AtomType) this).type == AtomTypeKind.INT;
	}

	/**
	 * Check if this type is builtin Char type.
	 * @return True if it is, otherwise false.
	 */
	public boolean isBuiltinCharType() {
		return isAtomType() && ((AtomType) this).type == AtomTypeKind.CHR;
	}

	/**
	 * Check if this type is builtin Double type.
	 * @return True if it is, otherwise false.
	 */
	public boolean isBuiltinDoubleType() {
		return isAtomType() && ((AtomType) this).type == AtomTypeKind.DOB;
	}

	/**
	 * Check if this type is builtin String type.
	 * @return True if it is, otherwise false.
	 */
	public boolean isBuiltinStringType() {
		return isAtomType() && ((AtomType) this).type == AtomTypeKind.STR;
	}

	/**
	 * Check if this type is builtin Bool type.
	 * @return True if it is, otherwise false.
	 */
	public boolean isBuiltinBoolType() {
		return isAtomType() && ((AtomType) this).type == AtomTypeKind.LOG;
	}
	
	/**
	 * Check if this type is builtin Nil type.
	 * @return True if it is, otherwise false.
	 */
	public boolean isBuiltinNilType() {
		return isAtomType() && ((AtomType) this).type == AtomTypeKind.NIL;
	}

    /**
     * Check if this type is builtin Nil type.
     * @return True if it is, otherwise false.
     */
    public boolean isVoidType() {
        return isAtomType() && ((AtomType) this).type == AtomTypeKind.VOID;
    }

    /**
	 * String representation.
	 */
	public abstract String toString();
	
	/**
	 * Friendly name.
	 */
	public abstract String friendlyName();

	/// Static members
	public final static AtomType intType = new AtomType(AtomTypeKind.INT);
	public final static AtomType charType = new AtomType(AtomTypeKind.CHR);
	public final static AtomType doubleType = new AtomType(AtomTypeKind.DOB);
	public final static AtomType stringType = new AtomType(AtomTypeKind.STR);
	public final static AtomType boolType = new AtomType(AtomTypeKind.LOG);
    public final static AtomType voidType = new AtomType(AtomTypeKind.VOID);
    public final static AtomType nilType = new AtomType(AtomTypeKind.NIL);

    public final static AtomType[] atomTypes = new AtomType[] { Type.intType, Type.charType, Type.doubleType, Type.stringType, Type.boolType, Type.voidType };

    public static AtomType atomType(AtomTypeKind kind) {
        switch (kind) {
            case CHR: return Type.charType;
            case INT: return Type.intType;
            case DOB: return Type.doubleType;
            case LOG: return Type.boolType;
            case STR: return Type.stringType;
            case VOID: return Type.voidType;
            case NIL: return Type.nilType;
            default: return null;
        }
    }
}
