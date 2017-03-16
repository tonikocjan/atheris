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

package compiler.abstr.tree.expr;

import java.util.*;

import compiler.*;
import compiler.abstr.*;
import compiler.abstr.tree.def.AbsParDef;

/**
 * Klic funkcije.
 * 
 * @author toni kocjan
 */
public class AbsFunCall extends AbsExpr {
	
	/** Ime funkcije. */
	public final String name;
	
	/** Argumenti funkcije. */
	public final Vector<AbsExpr> args;

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
	public AbsFunCall(Position pos, String name, Vector<AbsExpr> args) {
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
	public void addArgument(AbsExpr arg) {
		args.insertElementAt(arg, 0);;
	}

	@Override public void accept(ASTVisitor aSTVisitor) { aSTVisitor.visit(this); }

}