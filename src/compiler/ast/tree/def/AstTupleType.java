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

package compiler.ast.tree.def;

import java.util.List;

import compiler.Position;
import compiler.ast.ASTVisitor;
import compiler.ast.tree.AstDefinitions;
import compiler.ast.tree.type.AstType;

public class AstTupleType extends AstType {

	public final List<AstType> types;

	public AstTupleType(Position pos, List<AstType> types) {
		super(pos);
		this.types = types;
	}

	@Override
	public void accept(ASTVisitor aSTVisitor) { aSTVisitor.visit(this); }

    @Override
    public String getName() {
        return "";
    }
}
