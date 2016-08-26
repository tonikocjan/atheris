package compiler.seman;

import java.util.*;

import compiler.*;
import compiler.abstr.tree.def.AbsDef;
import compiler.seman.type.Type;

public class SymbTable {

	/** Simbolna tabela. */
	private static HashMap<String, LinkedList<AbsDef>> mapping = new HashMap<>();
	private static HashMap<String, HashMap<String, AbsDef>> functions = new HashMap<>();
	
	/** Trenutna globina nivoja gnezdenja. */
	private static int scope = 0;

	/**
	 * Preide na naslednji nivo gnezdenja.
	 */
	public static void newScope() {
		scope++;
	}

	/**
	 * Odstrani vse definicije na trenutnem nivoju gnezdenja in preide na
	 * predhodni nivo gnezdenja.
	 */
	public static void oldScope() {
		LinkedList<String> allNames = new LinkedList<String>();
		allNames.addAll(mapping.keySet());
		for (String name : allNames) {
			try {
				SymbTable.del(name);
			} catch (SemIllegalDeleteException __) {
			}
		}
		scope--;
	}

	/**
	 * Vstavi novo definicijo imena na trenutni nivo gnezdenja.
	 * 
	 * @param name
	 *            Ime.
	 * @param newDef
	 *            Nova definicija.
	 * @throws SemIllegalInsertException
	 *             Ce definicija imena na trenutnem nivoju gnezdenja ze obstaja.
	 */
	public static void ins(String name, AbsDef newDef)
			throws SemIllegalInsertException {
		LinkedList<AbsDef> allNameDefs = mapping.get(name);
		if (allNameDefs == null) {
			allNameDefs = new LinkedList<AbsDef>();
			allNameDefs.addFirst(newDef);
			SymbDesc.setScope(newDef, scope);
			mapping.put(name, allNameDefs);
			return;
		}
		if ((allNameDefs.size() == 0)
				|| (SymbDesc.getScope(allNameDefs.getFirst()) == null)) {
			Thread.dumpStack();
			Report.error("Internal error.");
			return;
		}
		if (SymbDesc.getScope(allNameDefs.getFirst()) == scope)
			throw new SemIllegalInsertException();
		allNameDefs.addFirst(newDef);
		SymbDesc.setScope(newDef, scope);
	}
	
	/**
	 * Vstavi novo definicijo funkcije na trenutni nivo gnezdenja.
	 * 
	 * @param name
	 *            Ime.
	 * @param type
	 * 			  Tip funkcije
	 * @param newDef
	 *            Nova definicija.
	 * @throws SemIllegalInsertException
	 *             Ce definicija imena na trenutnem nivoju gnezdenja ze obstaja.
	 */
	public static void insFunc(String name, Vector<Type> parameters, AbsDef newDef)
			throws SemIllegalInsertException {
		HashMap<String, AbsDef> hashmap = functions.get(name);
		
		// if method with such parameters already exists
		if (hashmap != null && hashmap.get(parameters) != null)
			throw new SemIllegalInsertException();
		
		if (hashmap == null) hashmap = new HashMap<>();
		hashmap.put(parameters.toString(), newDef);
		functions.put(name, hashmap);
		
		SymbDesc.setScope(newDef, scope);
	}

	/**
	 * Odstrani definicijo imena s trenutnega nivoja gnezdenja.
	 * 
	 * @param name
	 *            Ime.
	 * @throws SemIllegalDeleteException
	 *             Ce definicije imena na trenutnem nivoju gnezdenja ni.
	 */
	public static void del(String name) throws SemIllegalDeleteException {
		LinkedList<AbsDef> allNameDefs = mapping.get(name);
		if (allNameDefs == null)
			throw new SemIllegalDeleteException();
		if ((allNameDefs.size() == 0)
				|| (SymbDesc.getScope(allNameDefs.getFirst()) == null)) {
			Thread.dumpStack();
			Report.error("Internal error.");
			return;
		}
		if (SymbDesc.getScope(allNameDefs.getFirst()) < scope)
			throw new SemIllegalDeleteException();
		allNameDefs.removeFirst();
		if (allNameDefs.size() == 0)
			mapping.remove(name);
	}

	/**
	 * Vrne definicijo imena.
	 * 
	 * @param name
	 *            Ime.
	 * @return Definicija imena ali null, ce definicija imena ne obstaja.
	 */
	public static AbsDef fnd(String name) {
		LinkedList<AbsDef> allNameDefs = mapping.get(name);
		if (allNameDefs == null)
			return null;
		if (allNameDefs.size() == 0)
			return null;
		return allNameDefs.getFirst();
	}
	
	/**
	 * Vrne definicijo funkcije.
	 * @param name ime funkcije
	 * @param parameters tipi parametrov klica funkcije
	 * @return
	 */
	public static AbsDef fndFunc(String name, Vector<Type> parameters) {
		if (functions.get(name) != null)
			return functions.get(name).get(parameters.toString());
		return null;
	}

	public static void clean() {
		mapping.clear();
		mapping = null;
		functions.clear();
		functions = null;
	}
}
