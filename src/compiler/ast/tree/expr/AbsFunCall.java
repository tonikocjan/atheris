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

/**
 * Klic funkcije.
 * 
 * @author toni kocjan
 */
public class AbsFunCall extends AbsExpr {
	
	/** Ime funkcije. */
	public final String name;
	
	/** Argumenti funkcije. */
	public final ArrayList<AbsLabeledExpr> args;

	/**
	 * Ustvari nov opis klica funkcije.
	 * 
	 * @param pos
	 *            Polozaj stavcne oblike tega drevesa.
	 * @param name
	 *            Ime funkcije.
	 * @param args
	 *            Argumenti funkcije.
	 */
	public AbsFunCall(Position pos, String name, ArrayList<AbsLabeledExpr> args) {
		super(pos);
		
		this.name = name;
		this.args = args;
	}

	/**
	 * Vrne izbrani argument.
	 * 
	 * @param index
	 *            Indeks argumenta.
	 * @return Argument na izbranem mestu.
	 */
	public AbsExpr arg(int index) {
		return args.get(index);
	}

	/**
	 * Vrne stevilo argumentov.
	 * 
	 * @return Stevilo argumentov.
	 */
	public int numArgs() {
		return args.size();
	}

	/**
	 * Add new argument to this function call.
	 * @param arg Argument to be added.
	 */
	public void addArgument(AbsLabeledExpr arg) {
		args.add(0, arg);
	}
	
	/**
	 * Get string representation of this function call
	 * @return String representation, i.e.: func (x: Int, y: Double) Int = (x:y:)
	 */
	public String getStringRepresentation() {
		StringBuilder sb = new StringBuilder(name);
		sb.append('(');
		for (AbsLabeledExpr arg: args) {
			sb.append(arg.name);
			sb.append(':');
		}
		sb.append(')');
		return sb.toString();
	}
	
	@Override public void accept(ASTVisitor aSTVisitor) { aSTVisitor.visit(this); }

}