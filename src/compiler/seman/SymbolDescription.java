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
import compiler.ast.tree.def.AstDefinition;
import compiler.seman.type.*;

public class SymbolDescription implements SymbolDescriptionMap {

	private Map<AstNode, Integer> scopeMap = new HashMap<>();
    private Map<AstNode, AstDefinition> definitionMap = new HashMap<>();
    private Map<AstNode, Type> typeMap = new HashMap<>();

	public void setScope(AstNode node, int nodeScope) {
		scopeMap.put(node, nodeScope);
	}

	public Integer getScope(AstNode node) {
		Integer nodeScope = scopeMap.get(node);
		return nodeScope;
	}

	public void setDefinitionForAstNode(AstNode node, AstDefinition def) {
		definitionMap.put(node, def);
	}

	public AstDefinition getDefinitionForAstNode(AstNode node) {
		AstDefinition def = definitionMap.get(node);
		return def;
	}

	public void setTypeForAstNode(AstNode node, Type type) {
        typeMap.put(node, type);
	}

	public Type getTypeForAstNode(AstNode node) {
		Type typ = typeMap.get(node);
		return typ;
	}
}
