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

package compiler.abstr.tree.def;

import java.util.HashSet;

import compiler.Position;
import compiler.abstr.ASTVisitor;
import compiler.abstr.tree.AbsDefs;

/**
 * Import definition.
 * @author toni kocjan
 *
 */
public class AbsImportDef extends AbsDef {
	
	/** Imported definitions.  */
	public AbsDefs imports;
	
	/** Set of definition names which to be imported. */
	public final HashSet<String> definitions = new HashSet<>();

	/**
	 * Create new import definition.
	 * @param pos Position.
	 * @param fileName Name of the file to import.
	 */
	public AbsImportDef(Position pos, String fileName) {
		super(pos, fileName);
		imports = null;
	}

	@Override
	public void accept(ASTVisitor aSTVisitor) { aSTVisitor.visit(this); }

}
