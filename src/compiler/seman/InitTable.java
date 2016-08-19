package compiler.seman;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

import compiler.abstr.tree.AbsVarDef;

public class InitTable {
	
	/**
	 * Already initialized variables.
	 */
	private static HashSet<AbsVarDef> initTable = new HashSet<>();
	private static HashMap<AbsVarDef, HashSet<Integer>> scopes = new HashMap<>();
	
	/**
	 * Current initialization scope.
	 */
	private static int scope = 0;
	
	/**
	 * Initialize variable.
	 */
	public static void initialize(AbsVarDef var) {
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
	public static void uninitialize(AbsVarDef var) {
		initTable.remove(var);
		scopes.remove(var);
	}
	
	/**
	 * Check if variable is initialized
	 * @return true if variable is initialized, otherwise false
	 */
	public static boolean isInitialized(AbsVarDef var) {
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
		Iterator<Map.Entry<AbsVarDef, HashSet<Integer>>> iter = scopes.entrySet().iterator();
		while (iter.hasNext()) {
		    Map.Entry<AbsVarDef, HashSet<Integer>> entry = iter.next();
		    entry.getValue().remove(scope);
		    if (entry.getValue().size() == 0) {
		    	initTable.remove(entry.getKey());
		    	iter.remove();
		    }
		}

		scope--;
	}
}
