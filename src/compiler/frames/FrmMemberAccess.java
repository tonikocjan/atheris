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

import compiler.abstr.tree.def.AbsVarDef;
import compiler.seman.type.ClassType;
import compiler.seman.type.ObjectType;
import compiler.seman.type.Type;

public class FrmMemberAccess implements FrmAccess {

	/**
	 * Member definition.
	 */
	public final AbsVarDef memberDef;

	/**
	 * Parent type.
	 */
	public final ObjectType parentType;

    /**
     *
     * @param memberDef
     * @param parentType
     */
	public FrmMemberAccess(AbsVarDef memberDef, ObjectType parentType) {
		this.memberDef = memberDef;
		this.parentType = parentType;
	}
	
	public int offsetForMember() {
		return parentType.offsetForMember(memberDef.getName());
	}

	@Override
	public String toString() {
		return "Member (" + memberDef.name + ", offset: " + offsetForMember() + ")";
	}
}
