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

import java.util.ArrayList;

import compiler.seman.SymbolDescriptionMap;
import compiler.seman.SymbolTableMap;
import utils.Constants;
import compiler.ast.ASTVisitor;
import compiler.ast.tree.*;
import compiler.ast.tree.def.*;
import compiler.ast.tree.expr.AbsAtomConstExpr;
import compiler.ast.tree.expr.AbsBinExpr;
import compiler.ast.tree.expr.AbsExpr;
import compiler.ast.tree.expr.AbsForceValueExpr;
import compiler.ast.tree.expr.AbsFunCall;
import compiler.ast.tree.expr.AbsLabeledExpr;
import compiler.ast.tree.expr.AbsListExpr;
import compiler.ast.tree.expr.AbsOptionalEvaluationExpr;
import compiler.ast.tree.expr.AbsReturnExpr;
import compiler.ast.tree.expr.AbsTupleExpr;
import compiler.ast.tree.expr.AbsUnExpr;
import compiler.ast.tree.expr.AbsVarNameExpr;
import compiler.ast.tree.stmt.AbsCaseStmt;
import compiler.ast.tree.stmt.AbsControlTransferStmt;
import compiler.ast.tree.stmt.AbsForStmt;
import compiler.ast.tree.stmt.AbsIfStmt;
import compiler.ast.tree.stmt.AbsSwitchStmt;
import compiler.ast.tree.stmt.AbsWhileStmt;
import compiler.ast.tree.type.AbsAtomType;
import compiler.ast.tree.type.AbsFunType;
import compiler.ast.tree.type.AbsListType;
import compiler.ast.tree.type.AbsOptionalType;
import compiler.ast.tree.type.AbsTypeName;
import compiler.seman.type.CanType;
import compiler.seman.type.ClassType;
import compiler.seman.type.Type;

public class FrmEvaluator implements ASTVisitor {
    
    public FrmFrame entryPoint = null;
    private Type parentType = null;
    private FrmFrame currentFrame = null;
	private int currentLevel = 1;
	
	private FrameDescriptionMap frameDescription;
    private SymbolTableMap symbolTable;
    private SymbolDescriptionMap symbolDescription;

    public FrmEvaluator(SymbolTableMap symbolTable, SymbolDescriptionMap symbolDescription, FrameDescriptionMap frameDescription) {
        this.symbolTable = symbolTable;
        this.symbolDescription = symbolDescription;
        this.frameDescription = frameDescription;

        AbsFunDef _main = new AbsFunDef(null, Constants.ENTRY_POINT, new ArrayList<>(),
                new AbsAtomType(null, AtomTypeKind.VOID), new AbsStmts(null, new ArrayList<>()));

        entryPoint = new FrmFrame(_main, 0);
        entryPoint.entryLabel = FrmLabel.newNamedLabel(Constants.ENTRY_POINT);
        entryPoint.parametersSize = 0;
        entryPoint.parameterCount = 0;

        currentFrame = entryPoint;
    }

	@Override
	public void visit(AbsListType acceptor) {
        ///
	}

	@Override
	public void visit(AbsClassDef acceptor) {
        Type currentParentType = parentType;
        parentType = symbolDescription.getTypeForAstNode(acceptor);

        if (((CanType) parentType).childType.isClassType()) {
            ClassType classType = (ClassType) ((CanType) parentType).childType;

            FrmVirtualTableAccess virtualTableAccess = new FrmVirtualTableAccess(
                    acceptor,
                    classType.virtualTableSize() + Constants.Byte*2);
            frameDescription.setAccess(acceptor, virtualTableAccess);
            frameDescription.setVirtualTable(classType, virtualTableAccess);
        }

		acceptor.definitions.accept(this);

		for (AbsFunDef c : acceptor.construstors) {
			c.accept(this);
		}
		
		parentType = currentParentType;
	}

	@Override
	public void visit(AbsAtomConstExpr acceptor) {
        ///
	}

	@Override
	public void visit(AbsAtomType acceptor) {
        ///
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
		symbolDescription.getDefinitionForAstNode(acceptor.iterator).accept(this);

		acceptor.iterator.accept(this);
		acceptor.collection.accept(this);
		acceptor.body.accept(this);
	}

	@Override
	public void visit(AbsFunCall acceptor) {
		int parSize = 4;

		for (AbsExpr arg: acceptor.args) {
            Type argType = symbolDescription.getTypeForAstNode(arg);
            int size = argType.isReferenceType() ? 4 : argType.sizeInBytes(); // FIXME: -

            parSize += size;
        }

		currentFrame.sizeArgs = Math.max(currentFrame.sizeArgs, parSize);
	}

	@Override
	public void visit(AbsFunDef acceptor) {
		FrmFrame frame = new FrmFrame(acceptor, currentLevel);

		frameDescription.setFrame(acceptor, frame);
		frameDescription.setAccess(acceptor, new FrmFunAccess(acceptor));

		FrmFrame tmp = currentFrame;
		currentFrame = frame;

		frame.parameterCount = acceptor.pars.size();

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
		frameDescription.setAccess(acceptor, new FrmParAccess(acceptor, currentFrame, currentFrame.parametersSize));
		
		Type type = symbolDescription.getTypeForAstNode(acceptor);
		int size = type.isObjectType() ? 4 : type.sizeInBytes(); // FIXME: -
		
		currentFrame.parametersSize += size;
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
        AbsDef parentDefinition = acceptor.getParentDefinition();
        Type parentDefinitionType = symbolDescription.getTypeForAstNode(parentDefinition);
        boolean isGlobal = currentFrame.entryLabel.getName().equals("_" + Constants.ENTRY_POINT);

        frameDescription.setAccess(acceptor, FrmAccess.createAccess(
                acceptor,
                symbolDescription.getTypeForAstNode(acceptor),
                parentDefinition,
                parentDefinitionType,
                isGlobal,
                currentFrame));
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

    @Override
    public void visit(AbsInterfaceDef absInterfaceDef) {
        absInterfaceDef.definitions.accept(this);
    }
}
