package compiler.seman;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import compiler.abstr.tree.AbsVarDef;

public class InitTable {
	
	/**
	 * Already initialized variables.
	 */
	private static HashMap<AbsVarDef, Integer> initTable = new HashMap<>();
	
	/**
	 * Current initialization scope.
	 */
	private static int scope = 0;
	
	/**
	 * Initialize variable.
	 * @return true if variable was not initialized before, otherwise false
	 */
	public static boolean initialize(AbsVarDef var) {
		return initTable.put(var, scope) == null ? true : false;
	}
	
	/**
	 * Unitialize variable.
	 */
	public static void uninitialize(AbsVarDef var) {
		initTable.remove(var);
	}
	
	/**
	 * Check if variable is initialized
	 * @return true if variable is initialized, otherwise false
	 */
	public static boolean isInitialized(AbsVarDef var) {
		return initTable.containsKey(var);
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
		Iterator<Map.Entry<AbsVarDef, Integer>> iter = initTable.entrySet().iterator();
		while (iter.hasNext()) {
		    Map.Entry<AbsVarDef, Integer> entry = iter.next();
		    
		    if (entry.getValue() == scope)
		    	iter.remove();
		}
		scope--;
	}
}
