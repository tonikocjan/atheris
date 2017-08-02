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

import compiler.ast.tree.*;
import compiler.ast.tree.def.AbsDef;
import compiler.seman.type.*;

/**
 * Opisi posameznih definicij.
 * 
 * @author sliva
 */
public class SymbDesc {

	/** Nivo vidnosti. */
	private static HashMap<AbsTree, Integer> scope = new HashMap<>();

	/**
	 * Doloci globino nivoja vidnosti za dano definicijo imena.
	 * 
	 * @param node
	 *            Vozlisce drevesa.
	 * @param nodeScope
	 *            Globina nivoja vidnosti.
	 */
	public static void setScope(AbsTree node, int nodeScope) {
		scope.put(node, new Integer(nodeScope));
	}

	/**
	 * Vrne globino nivoja vidnosti za dano definicijo imena.
	 * 
	 * @param node
	 *            Vozlisce drevesa.
	 * @return Globina nivoja vidnosti.
	 */
	public static Integer getScope(AbsTree node) {
		Integer nodeScope = scope.get(node);
		return nodeScope;
	}

	/** Definicija imena. */
	private static HashMap<AbsTree, AbsDef> nameDef = new HashMap<AbsTree, AbsDef>();

	/**
	 * Poveze vozlisce drevesa z definicijo imena.
	 * 
	 * @param node
	 *            Vozlisce drevesa.
	 * @param def
	 *            Definicija imena.
	 */
	public static void setNameDef(AbsTree node, AbsDef def) {
		nameDef.put(node, def);
	}

	/**
	 * Vrne definicijo imena, ki je povezano z vozliscem.
	 * 
	 * @param node
	 *            Vozlisce drevesa.
	 * @return Definicija imena.
	 */
	public static AbsDef getNameDef(AbsTree node) {
		AbsDef def = nameDef.get(node);
		return def;
	}

	/** Tipizacija vozlisc drevesa. */
	private static HashMap<AbsTree, Type> type = new HashMap<>();

	/**
	 * Poveze vozlisce drevesa z opisom tipa.
	 * 
	 * @param node
	 *            Vozlisce drevesa.
	 * @param typ
	 *            Opis tipa.
	 */
	public static void setType(AbsTree node, Type typ) {
		type.put(node, typ);
	}

	/**
	 * Vrne opis tipa, ki je povezano z vozliscem.
	 * 
	 * @param node
	 *            Vozlisce drevesa.
	 * @return Opis tipa.
	 */
	public static Type getType(AbsTree node) {
		Type typ = type.get(node);
		return typ;
	}

	public static void clean() {
		scope.clear();
		type.clear();
        nameDef.clear();
	}

}
