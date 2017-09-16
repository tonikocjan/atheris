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

import compiler.ast.tree.enums.AtomTypeKind;
import compiler.ast.tree.def.AstDefinition;
import compiler.logger.LoggerFactory;
import compiler.logger.LoggerInterface;

/**
 * Data memberType description.
 */
public abstract class Type {

    protected static LoggerInterface logger = LoggerFactory.logger();

    private static int TYPE_DESCRIPTOR = 0;

    private int getTypeDescriptor() {
        return TYPE_DESCRIPTOR++;
    }

    /** Descriptor for this memberType is automatically assigned when new memberType is instantiated  */
    public final int descriptor = getTypeDescriptor();

	public abstract boolean sameStructureAs(Type type);

	public abstract boolean canBeCastedToType(Type type);

	public abstract int sizeInBytes();

	public abstract boolean containsMember(String name);

	public abstract AstDefinition findMemberDefinitionWithName(String name);

	public abstract String toString();

    public abstract String friendlyName();

	public boolean isArrayType() {
		return this instanceof ArrayType;
	}

	public boolean isAtomType() {
		return this instanceof AtomType;
	}

	public boolean isCanType() {
		return this instanceof CanType;
	}

    public boolean isObjectType() {
        return this instanceof ObjectType;
    }

	public boolean isClassType() {
		return this instanceof ClassType;
	}

    public boolean isInterfaceType() {
        return this instanceof InterfaceType;
    }

    public boolean isStructType() {
        return this instanceof StructType;
    }

	public boolean isEnumType() {
		return this instanceof EnumType;
	}

	public boolean isFunctionType() {
		return this instanceof FunctionType;
	}

	public boolean isReferenceType() {
		return this instanceof ReferenceType;
	}

    public boolean isValueType() {
        return this instanceof ValueType;
    }

	public boolean isOptionalType() {
		return this instanceof OptionalType;
	}

	public boolean isTupleType() {
		return this instanceof TupleType;
	}

	public boolean isBuiltinIntType() {
		return isAtomType() && ((AtomType) this).type == AtomTypeKind.INT;
	}

	public boolean isBuiltinCharType() {
		return isAtomType() && ((AtomType) this).type == AtomTypeKind.CHR;
	}

	public boolean isBuiltinDoubleType() {
		return isAtomType() && ((AtomType) this).type == AtomTypeKind.DOB;
	}

	public boolean isBuiltinStringType() {
		return isAtomType() && ((AtomType) this).type == AtomTypeKind.STR;
	}

	public boolean isBuiltinBoolType() {
		return isAtomType() && ((AtomType) this).type == AtomTypeKind.LOG;
	}

	public boolean isBuiltinNilType() {
		return isAtomType() && ((AtomType) this).type == AtomTypeKind.NIL;
	}

    public boolean isVoidType() {
        return isAtomType() && ((AtomType) this).type == AtomTypeKind.VOID;
    }

    // TODO: Bad design!!
	public static void clean() {
        intType = new AtomType(AtomTypeKind.INT);
        charType = new AtomType(AtomTypeKind.CHR);
        doubleType = new AtomType(AtomTypeKind.DOB);
        stringType = new AtomType(AtomTypeKind.STR);
        boolType = new AtomType(AtomTypeKind.LOG);
        voidType = new AtomType(AtomTypeKind.VOID);
        nilType = new AtomType(AtomTypeKind.NIL);
        TYPE_DESCRIPTOR = 0;
    }

	/// Static members
    // TODO: Bad design; this variables should be final!
	public static AtomType intType = new AtomType(AtomTypeKind.INT);
	public static AtomType charType = new AtomType(AtomTypeKind.CHR);
	public static AtomType doubleType = new AtomType(AtomTypeKind.DOB);
	public static AtomType stringType = new AtomType(AtomTypeKind.STR);
	public static AtomType boolType = new AtomType(AtomTypeKind.LOG);
    public static AtomType voidType = new AtomType(AtomTypeKind.VOID);
    public static AtomType nilType = new AtomType(AtomTypeKind.NIL);
    public static InterfaceType anyType;

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
