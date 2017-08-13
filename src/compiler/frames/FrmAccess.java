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

import compiler.ast.tree.def.AbsDef;
import compiler.ast.tree.def.AbsVarDef;
import compiler.seman.type.CanType;
import compiler.seman.type.ObjectType;
import compiler.seman.type.Type;

public abstract class FrmAccess {

    public static FrmAccess createAccess(AbsVarDef acceptor,
                                         Type acceptorType,
                                         AbsDef parentDefinition,
                                         Type parentType,
                                         boolean isGlobal,
                                         FrmFrame currentFrame) {
        FrmAccess access;

        if (parentDefinition != null && parentType.isCanType()) {
            if (acceptor.isStatic()) {
                access = new FrmStaticAccess(acceptor, (CanType) parentType);
            }
            else {
                access = new FrmMemberAccess(acceptor, (ObjectType) ((CanType) parentType).childType);
            }
        }
        else if (isGlobal) {
            access = new FrmVarAccess(acceptor);
        }
        else {
            access = new FrmLocAccess(
                    acceptor,
                    currentFrame,
                    acceptorType);
        }

        return access;
    }

}
