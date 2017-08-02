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

package compiler.imcode;

import java.util.HashMap;

import compiler.ast.tree.AbsTree;

public class ImcDesc {
	/**
	 * 
	 */
	public static HashMap<AbsTree, ImcCode> imcMap = new HashMap<>();
	
	/**
	 * 
	 * @param node
	 * @param imc
	 */
	public static void setImcCode(AbsTree node, ImcCode imc) {
		imcMap.put(node, imc);
	}
	
	/**
	 * 
	 * @param node
	 * @return
	 */
	public static ImcCode getImcCode(AbsTree node) {
		return imcMap.get(node);
	}

	public static void clean() {
		imcMap.clear();
	}
}
