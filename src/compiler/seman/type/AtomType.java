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
import compiler.ast.tree.def.AstClassDefinition;
import compiler.logger.LoggerFactory;
import compiler.logger.LoggerInterface;

import java.util.ArrayList;

public class AtomType extends ObjectType {

    private static LoggerInterface logger = LoggerFactory.logger();

	public final AtomTypeKind type;
    public final CanType staticType;

	public AtomType(AtomTypeKind type) {
	    super(new AstClassDefinition(type.toString()), new ArrayList<>(), new ArrayList<>(), 0);
	    this.staticType = new CanType(this);
		this.type = type;
	}

	@Override
	public boolean sameStructureAs(Type type) {
		if (type.isAtomType()) {
		    return this.type == ((AtomType) type).type;
		}

		return false;
	}

	@Override
	public String toString() {
		switch (type) {
		case LOG: return "Bool";
		case INT: return "Int";
		case STR: return "String";
		case VOID: return "Void";
		case DOB: return "Double";
		case CHR: return "Char";
		case NIL: return "Nil";
		}
        logger.error("Internal error :: compiler.seman.memberType.SemAtomType.toString()");
		return "";
	}

	@Override
	public int sizeInBytes() {
		switch (type) {
		case LOG:
		case INT:
		case STR:
			return 4;
		case VOID:
		case NIL:
			return 0;
		case CHR:
			return 1;
		case DOB:
			return 8;
		}
        logger.error("Internal error :: compiler.seman.memberType.SemAtomType.sizeInBytes()");
		return 0;
	}

	@Override
	public boolean canBeCastedToType(Type t) {
	    if (super.canBeCastedToType(t)) {
	        return true;
        }

		if (!t.isAtomType()) return false;
		
		// int can be casted to double
		return t.isBuiltinDoubleType() && isBuiltinIntType();
	}

	@Override
	public String friendlyName() {
		return toString();
	}
}
