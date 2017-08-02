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

import compiler.*;
import compiler.ast.*;
import compiler.ast.tree.type.AbsType;

/**
 * Parameter definition.
 * @author toni kocjan
 *
 */
public class AbsParDef extends AbsDef {

	/** Parameter type. */
	public AbsType type;

	/**
	 * Each function parameter has both an argument label and a parameter name.
	 * The argument label is used when calling the function; each argument is written in
	 * the function call with its argument label before it.
	 * The parameter name is used in the implementation of the function.
	 * By default, parameters use their parameter name as their argument label.
	 */
	public final String argumentLabel;

	/**
	 * Create new parameter definition..
	 * 
	 * @param pos
	 *            Position.
	 * @param name
	 *            Parameter Name.
	 * @param argumentLabel
	 * 			  Argument label.
	 * @param type
	 *            Type.
	 */
	public AbsParDef(Position pos, String name, String argumentLabel, AbsType type) {
		super(pos, name == null ? argumentLabel : name);
		this.type = type;
		this.argumentLabel = argumentLabel;
	}

	public AbsParDef(Position pos, String name, AbsType type) {
		super(pos, name);
		this.type = type;
		this.argumentLabel = name;
	}

	@Override public void accept(ASTVisitor aSTVisitor) { aSTVisitor.visit(this); }
}
