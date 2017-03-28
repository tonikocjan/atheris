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

package compiler.seman;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

import compiler.abstr.tree.def.AbsDef;
import compiler.abstr.tree.def.AbsVarDef;

public class InitTable {
	
	/**
	 * Already initialized variables.
	 */
	private static HashSet<AbsDef> initTable = new HashSet<>();
	private static HashMap<AbsDef, HashSet<Integer>> scopes = new HashMap<>();
	
	/**
	 * Current initialization scope.
	 */
	private static int scope = 0;
	
	/**
	 * Initialize variable.
	 */
	public static void initialize(AbsDef var) {
		initTable.add(var);
		
		HashSet<Integer> currentScope = scopes.get(var);
		if (currentScope == null)
			currentScope = new HashSet<>();
		currentScope.add(scope);
		scopes.put(var, currentScope);
	}
	
	/**
	 * Unitialize variable.
	 */
	public static void uninitialize(AbsDef var) {
		initTable.remove(var);
		scopes.remove(var);
	}
	
	/**
	 * Check if variable is initialized
	 * @return true if variable is initialized, otherwise false
	 */
	public static boolean isInitialized(AbsDef var) {
		return initTable.contains(var);
	}
	
	/**
	 * New initialization scope.
	 */
	public static void newScope() {
		scope++;
	}
	
	/**
	 * Old initialization scope.
	 * Remove all initializations in current scope and go to previous scope
	 */
	public static void oldScope() {
		// TODO: - O(n), should increase speed? 
		Iterator<Map.Entry<AbsDef, HashSet<Integer>>> iter = scopes.entrySet().iterator();

		while (iter.hasNext()) {
		    Map.Entry<AbsDef, HashSet<Integer>> entry = iter.next();
		    entry.getValue().remove(scope);

		    if (entry.getValue().size() == 0) {
		    	initTable.remove(entry.getKey());
		    	iter.remove();
		    }
		}

		scope--;
	}
}
