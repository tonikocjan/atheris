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
import compiler.ast.tree.def.AbsDef;

public class SymbolTable implements SymbolTableMap {

	private HashMap<String, LinkedList<AbsDef>> mapping = new HashMap<>();
	private int currentScopeDepth = 0;
	private SymbolDescriptionMap symbolDescription;

	public SymbolTable(SymbolDescriptionMap symbolDescription) {
	    this.symbolDescription = symbolDescription;
    }

	public void newScope() {
		currentScopeDepth++;
	}

	public void oldScope() {
        removeAllDefinitionsOnCurrentScope();
		currentScopeDepth--;
	}

	private void removeAllDefinitionsOnCurrentScope() {
        LinkedList<String> allNames = new LinkedList<>();
        allNames.addAll(mapping.keySet());

        for (String name : allNames) {
            try {
                removeDefinitionFromCurrentScope(name);
            } catch (SemIllegalDeleteException __) {
            }
        }
    }

	public void insertDefinitionOnCurrentScope(String name, AbsDef definition) throws SemIllegalInsertException {
		LinkedList<AbsDef> definitionsForName = mapping.get(name);

		if (definitionsForName == null) {
			createNewEntry(name, definition);
            setScopeForDefinition(definition);
			return;
		}

		if (isIllegal(definitionsForName)) {
			Thread.dumpStack();
			Logger.error("Internal error.");
			return;
		}

		if (isNameAlreadyUsedInCurrentScope(definitionsForName)) {
            throw new SemIllegalInsertException();
        }

		definitionsForName.addFirst(definition);
        setScopeForDefinition(definition);
	}

    public void removeDefinitionFromCurrentScope(String name) throws SemIllegalDeleteException {
        LinkedList<AbsDef> definitionsForName = mapping.get(name);

        if (definitionsForName == null) {
            throw new SemIllegalDeleteException();
        }

        if (isIllegal(definitionsForName)) {
            Thread.dumpStack();
            Logger.error("Internal error.");
            return;
        }

        if (!isNameDefinedInCurrentOrGreaterScope(definitionsForName)) {
            throw new SemIllegalDeleteException();
        }

        definitionsForName.removeFirst();
        if (definitionsForName.isEmpty())
            mapping.remove(name);
    }

    public AbsDef findDefinitionForName(String name) {
        LinkedList<AbsDef> definitionsForName = mapping.get(name);

        if (definitionsForName == null || definitionsForName.isEmpty()) {
            return null;
        }

        return definitionsForName.getFirst();
    }

	private void createNewEntry(String name, AbsDef definition) {
        LinkedList<AbsDef> definitions = new LinkedList<>();
        definitions.addFirst(definition);

        mapping.put(name, definitions);
    }

    private void setScopeForDefinition(AbsDef definition) {
        symbolDescription.setScope(definition, currentScopeDepth);
    }

    private boolean isNameAlreadyUsedInCurrentScope(List<AbsDef> definitions) {
        return symbolDescription.getScope(definitions.get(0)) == currentScopeDepth;
    }

    private boolean isNameDefinedInCurrentOrGreaterScope(List<AbsDef> definitions) {
	    return symbolDescription.getScope(definitions.get(0)) >= currentScopeDepth;
    }

    private boolean isIllegal(List<AbsDef> definitionList) {
	    return definitionList.isEmpty() || (symbolDescription.getScope(definitionList.get(0)) == null);
    }
}
