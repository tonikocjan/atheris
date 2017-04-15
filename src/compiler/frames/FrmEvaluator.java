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

import java.util.LinkedList;

import compiler.abstr.ASTVisitor;
import compiler.abstr.tree.*;
import compiler.abstr.tree.def.*;
import compiler.abstr.tree.expr.AbsAtomConstExpr;
import compiler.abstr.tree.expr.AbsBinExpr;
import compiler.abstr.tree.expr.AbsExpr;
import compiler.abstr.tree.expr.AbsForceValueExpr;
import compiler.abstr.tree.expr.AbsFunCall;
import compiler.abstr.tree.expr.AbsLabeledExpr;
import compiler.abstr.tree.expr.AbsListExpr;
import compiler.abstr.tree.expr.AbsOptionalEvaluationExpr;
import compiler.abstr.tree.expr.AbsReturnExpr;
import compiler.abstr.tree.expr.AbsTupleExpr;
import compiler.abstr.tree.expr.AbsUnExpr;
import compiler.abstr.tree.expr.AbsVarNameExpr;
import compiler.abstr.tree.stmt.AbsCaseStmt;
import compiler.abstr.tree.stmt.AbsControlTransferStmt;
import compiler.abstr.tree.stmt.AbsForStmt;
import compiler.abstr.tree.stmt.AbsIfStmt;
import compiler.abstr.tree.stmt.AbsSwitchStmt;
import compiler.abstr.tree.stmt.AbsWhileStmt;
import compiler.abstr.tree.type.AbsAtomType;
import compiler.abstr.tree.type.AbsFunType;
import compiler.abstr.tree.type.AbsListType;
import compiler.abstr.tree.type.AbsOptionalType;
import compiler.abstr.tree.type.AbsTypeName;
import compiler.seman.SymbDesc;
import compiler.seman.type.CanType;
import compiler.seman.type.ClassType;
import compiler.seman.type.ObjectType;
import compiler.seman.type.Type;

public class FrmEvaluator implements ASTVisitor {

    private ObjectType parentType = null;
    private FrmFrame currentFrame = null;
    public FrmFrame entryPoint = null;
	private int currentLevel = 1;
	
	public static final String ENTRY_POINT = "_main";

    /**
     *
     */
	public FrmEvaluator() {
		AbsFunDef _main = new AbsFunDef(null, ENTRY_POINT, new LinkedList<>(), 
				new AbsAtomType(null, AtomTypeKind.VOID), new AbsStmts(null, 
						new LinkedList<>()));

		entryPoint = new FrmFrame(_main, 0);
		entryPoint.label = FrmLabel.newLabel(ENTRY_POINT);
		entryPoint.sizePars = 0;
		entryPoint.numPars = 0;
		
		currentFrame = entryPoint;
	}

	@Override
	public void visit(AbsListType acceptor) {
        ///
	}

	@Override
	public void visit(AbsClassDef acceptor) {
        ObjectType tmp = parentType;
        parentType = (ObjectType) ((CanType) SymbDesc.getType(acceptor)).childType;

        if (parentType.isClassType()) {
            FrmVirtualTableAccess virtualTableAccess = new FrmVirtualTableAccess(acceptor,
                    ((ClassType) parentType).virtualTableSize() + 8);
            FrmDesc.setAccess(acceptor, virtualTableAccess);
            FrmDesc.setVirtualTable((ClassType) parentType, virtualTableAccess);
        }

		acceptor.definitions.accept(this);

		for (AbsFunDef c : acceptor.contrustors) {
			c.accept(this);
		}
		
		parentType = tmp;
	}

	@Override
	public void visit(AbsAtomConstExpr acceptor) {
        ///
	}

	@Override
	public void visit(AbsAtomType acceptor) {

	}

	@Override
	public void visit(AbsBinExpr acceptor) {
		acceptor.expr1.accept(this);
		acceptor.expr2.accept(this);
	}

	@Override
	public void visit(AbsDefs acceptor) {
		for (AbsDef def : acceptor.definitions)
			def.accept(this);
	}

	@Override
	public void visit(AbsExprs acceptor) {
		for (AbsExpr e : acceptor.expressions)
			e.accept(this);
	}

	@Override
	public void visit(AbsForStmt acceptor) {
		SymbDesc.getNameDef(acceptor.iterator).accept(this);
		acceptor.iterator.accept(this);
		acceptor.collection.accept(this);
		acceptor.body.accept(this);
	}

	@Override
	public void visit(AbsFunCall acceptor) {
		int parSize = 4;

		for (AbsExpr arg: acceptor.args) {
            Type argType = SymbDesc.getType(arg);
            int size = argType.isReferenceType() ? 4 : argType.size(); // FIXME: -

            parSize += size;
        }

		currentFrame.sizeArgs = Math.max(currentFrame.sizeArgs, parSize);
	}

	@Override
	public void visit(AbsFunDef acceptor) {
		FrmFrame frame = new FrmFrame(acceptor, currentLevel);

		FrmDesc.setFrame(acceptor, frame);
		FrmDesc.setAccess(acceptor, new FrmFunAccess(acceptor));

		FrmFrame tmp = currentFrame;
		currentFrame = frame;

		for (AbsParDef par : acceptor.pars) {
            par.accept(this);
        }

		currentLevel++;

		acceptor.func.accept(this);

		currentFrame = tmp;
		currentLevel--;
	}

	@Override
	public void visit(AbsIfStmt acceptor) {
		for (Condition c : acceptor.conditions) {
			c.cond.accept(this);
			c.body.accept(this);
		}

		if (acceptor.elseBody != null) {
            acceptor.elseBody.accept(this);
        }
	}

	@Override
	public void visit(AbsParDef acceptor) {
		FrmDesc.setAccess(acceptor, new FrmParAccess(acceptor, currentFrame, currentFrame.sizePars));
		
		Type type = SymbDesc.getType(acceptor);
		int size = type.isReferenceType() ? 4 : type.size(); // FIXME: -
		
		currentFrame.sizePars += size;
		currentFrame.numPars++;
	}

	@Override
	public void visit(AbsTypeName acceptor) {
        ///
	}

	@Override
	public void visit(AbsUnExpr acceptor) {
		acceptor.expr.accept(this);
	}

	@Override
	public void visit(AbsVarDef acceptor) {
		if (parentType != null) {
            // member access
            FrmDesc.setAccess(acceptor, new FrmMemberAccess(acceptor, parentType));
        }
		else if (currentFrame.label.name().equals("_" + ENTRY_POINT)) {
            // var access
            FrmDesc.setAccess(acceptor, new FrmVarAccess(acceptor));
        }
		else {
            // local function access
            FrmDesc.setAccess(acceptor, new FrmLocAccess(acceptor, currentFrame));
        }
	}

	@Override
	public void visit(AbsVarNameExpr acceptor) {
        ///
	}

	@Override
	public void visit(AbsWhileStmt acceptor) {
		acceptor.cond.accept(this);
		acceptor.body.accept(this);
	}

	@Override
	public void visit(AbsImportDef importDef) {
		importDef.imports.accept(this);
	}

	@Override
	public void visit(AbsStmts stmts) {
		for (AbsStmt s : stmts.statements) {
			s.accept(this);
		}
	}

	@Override
	public void visit(AbsReturnExpr returnExpr) {
		if (returnExpr.expr != null) 
			returnExpr.expr.accept(this);
	}

	@Override
	public void visit(AbsListExpr absListExpr) {
		for (AbsExpr e : absListExpr.expressions)
			e.accept(this);
	}

	@Override
	public void visit(AbsFunType acceptor) {
		
	}

	@Override
	public void visit(AbsControlTransferStmt acceptor) {
		///
	}

	@Override
	public void visit(AbsSwitchStmt switchStmt) {
		switchStmt.subjectExpr.accept(this);
		
		for (AbsCaseStmt singleCase : switchStmt.cases)
			singleCase.accept(this);
		
		if (switchStmt.defaultBody != null)
			switchStmt.defaultBody.accept(this);
	}

	@Override
	public void visit(AbsCaseStmt acceptor) {
		for (AbsExpr e : acceptor.exprs)
			e.accept(this);
		acceptor.body.accept(this);
	}

	@Override
	public void visit(AbsEnumDef acceptor) {
		for (AbsDef def : acceptor.definitions)
			def.accept(this);
	}

	@Override
	public void visit(AbsEnumMemberDef acceptor) {
		///
	}

	@Override
	public void visit(AbsTupleDef acceptor) {
		acceptor.definitions.accept(this);
	}

	@Override
	public void visit(AbsLabeledExpr acceptor) {
		acceptor.expr.accept(this);
	}

	@Override
	public void visit(AbsTupleExpr acceptor) {
		acceptor.expressions.accept(this);
	}
	
	@Override
	public void visit(AbsOptionalType acceptor) {
		acceptor.childType.accept(this);
	}

	@Override
	public void visit(AbsOptionalEvaluationExpr acceptor) {
		acceptor.subExpr.accept(this);
	}

	@Override
	public void visit(AbsForceValueExpr acceptor) {
		acceptor.subExpr.accept(this);
	}

    @Override
    public void visit(AbsExtensionDef acceptor) {
        acceptor.extendingType.accept(this);
        acceptor.definitions.accept(this);
    }

}
