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

import java.util.*;

import compiler.*;
import compiler.abstr.tree.def.AbsDef;

public class SymbTable {
	
	/** Simbolna tabela. */
	private static HashMap<String, LinkedList<AbsDef>> mapping = new HashMap<String, LinkedList<AbsDef>>();
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
	
	public static void clean() {
		mapping.clear();
		mapping = null;
	}
}
