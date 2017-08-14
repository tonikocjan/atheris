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

import compiler.ast.tree.def.AstVariableDefinition;
import compiler.seman.type.ObjectType;

public class FrmMemberAccess extends FrmAccess {

	private final AstVariableDefinition memberDefinition;
    private final ObjectType parentType;

	public FrmMemberAccess(AstVariableDefinition memberDefinition, ObjectType parentType) {
		this.memberDefinition = memberDefinition;
		this.parentType = parentType;
	}
	
	public int offsetForMember() {
		return parentType.offsetForMember(memberDefinition.getName());
	}

	@Override
	public String toString() {
		return "Member (" + memberDefinition.name + ", framePointerOffset: " + offsetForMember() + ")";
	}
}
