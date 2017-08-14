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

package compiler.ast.tree.type;

import compiler.*;
import compiler.ast.*;
import compiler.ast.tree.AtomTypeKind;

public class AstAtomType extends AstType {

	public final AtomTypeKind type;

	public AstAtomType(Position pos, AtomTypeKind type) {
		super(pos);
		this.type = type;
	}
	
	public String toString() {
        return getName();
	}
	
	@Override public void accept(ASTVisitor aSTVisitor) { aSTVisitor.visit(this); }

    @Override
    public String getName() {
        switch (type) {
            case INT:
                return "Int";
            case DOB:
                return "Double";
            case CHR:
                return "Char";
            case VOID:
                return "Void";
            case STR:
                return "String";
            case LOG:
                return "Bool";
            case NIL:
                return "Null";
            default:
                return null;
        }
    }
}
