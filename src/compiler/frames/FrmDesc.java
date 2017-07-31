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

package compiler.frames;

import java.util.*;

import compiler.abstr.tree.*;
import compiler.abstr.tree.def.AbsDef;
import compiler.seman.type.ClassType;

/**
 * Shranjevanje klicnih zapisov.
 * 
 * @author sliva
 */
public class FrmDesc {

	/**
     * Klicni zapisi.
     */
	private static HashMap<AbsDef, FrmFrame> frames = new HashMap<>();

    /**
     * Opisi dostopa.
     */
    private static HashMap<AbsDef, FrmAccess> acceses = new HashMap<>();

    /**
     * Opisi dostopa.
     */
    private static HashMap<ClassType, FrmVirtualTableAccess> virtualTables = new HashMap<>();

	/**
	 * Poveze funkcijo s klicnim zapisom.
	 * 
	 * @param fun Funkcija.
	 * @param frame Klicni zapis.
	 */
	public static void setFrame(AbsDef fun, FrmFrame frame) {
		FrmDesc.frames.put(fun, frame);
	}
	
	/**
	 * Vrne klicni zapis funkcije.
	 * 
	 * @param fun Funkcija.
	 * @return Klicni zapis.
	 */
	public static FrmFrame getFrame(AbsTree fun) {
		return FrmDesc.frames.get(fun);
	}

	/**
	 * Poveze spremenljivko, parameter ali komponento z opisom dostopa.
	 * 
	 * @param var Spremenljivka, parameter ali komponenta.
	 * @param access Opis dostopa.
	 */
	public static void setAccess(AbsDef var, FrmAccess access) {
		FrmDesc.acceses.put(var, access);
	}
	
	/**
	 * Vrne opis dostopa do spremenljivke, parametra ali komponente.
	 * 
	 * @param var Spremenljivka, parameter ali komponenta.
	 * @return Opis dostopa.
	 */
	public static FrmAccess getAccess(AbsDef var) {
		return FrmDesc.acceses.get(var);
	}


    public static void setVirtualTable(ClassType classType, FrmVirtualTableAccess virtualTable) {
        virtualTables.put(classType, virtualTable);
    }

    public static FrmVirtualTableAccess getVirtualTable(ClassType classType) {
        return virtualTables.get(classType);
    }

    /**
     * Cleanup.
     */
	public static void clean() {
		frames.clear();
        acceses.clear();
	}

}
