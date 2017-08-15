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

package compiler.ast.tree.expr;

import java.util.*;

import compiler.*;
import compiler.ast.*;

public class AstFunctionCallExpression extends AstExpression {

	public final String name;
	public final List<AstLabeledExpr> arguments;

	public AstFunctionCallExpression(Position pos, String name, List<AstLabeledExpr> args) {
		super(pos);
		
		this.name = name;
		this.arguments = args;
	}

	public AstExpression getArgumentAtIndex(int index) {
		return arguments.get(index);
	}

	public int getArgumentCount() {
		return arguments.size();
	}

	public void addArgument(AstLabeledExpr arg) {
		arguments.add(0, arg);
	}

	public String getStringRepresentation() {
		StringBuilder sb = new StringBuilder(name);
		sb.append('(');
		for (AstLabeledExpr arg: arguments) {
			sb.append(arg.label);
			sb.append(':');
		}
		sb.append(')');
		return sb.toString();
	}
	
	@Override public void accept(ASTVisitor aSTVisitor) { aSTVisitor.visit(this); }

}