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

import compiler.ast.tree.enums.AtomTypeKind;
import compiler.ast.tree.expr.*;
import compiler.ast.tree.stmt.*;
import compiler.ast.tree.type.*;
import compiler.seman.SymbolDescriptionMap;
import compiler.seman.SymbolTableMap;
import utils.Constants;
import compiler.ast.ASTVisitor;
import compiler.ast.tree.*;
import compiler.ast.tree.def.*;
import compiler.ast.tree.expr.AstExpression;
import compiler.ast.tree.stmt.AstCaseStatement;
import compiler.ast.tree.stmt.AstSwitchStatement;
import compiler.ast.tree.type.AstAtomType;
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

        AstFunctionDefinition _main = new AstFunctionDefinition(null, Constants.ENTRY_POINT, new ArrayList<>(),
                new AstAtomType(null, AtomTypeKind.VOID), new AstStatements(null, new ArrayList<>()));

        entryPoint = new FrmFrame(_main, 0);
        entryPoint.entryLabel = FrmLabel.newNamedLabel(Constants.ENTRY_POINT);
        entryPoint.parametersSize = 0;
        entryPoint.parameterCount = 0;

        currentFrame = entryPoint;
    }

	@Override
	public void visit(AstListType acceptor) {
        ///
	}

	@Override
	public void visit(AstClassDefinition acceptor) {
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

		acceptor.memberDefinitions.accept(this);

		for (AstFunctionDefinition c : acceptor.construstors) {
			c.accept(this);
		}
		
		parentType = currentParentType;
	}

	@Override
	public void visit(AstAtomConstExpression acceptor) {
        ///
	}

	@Override
	public void visit(AstAtomType acceptor) {
        ///
	}

	@Override
	public void visit(AstBinaryExpression acceptor) {
		acceptor.expr1.accept(this);
		acceptor.expr2.accept(this);
	}

	@Override
	public void visit(AstDefinitions acceptor) {
		for (AstDefinition def : acceptor.definitions)
			def.accept(this);
	}

	@Override
	public void visit(AstExpressions acceptor) {
		for (AstExpression e : acceptor.expressions)
			e.accept(this);
	}

	@Override
	public void visit(AstForStatement acceptor) {
		symbolDescription.getDefinitionForAstNode(acceptor.iterator).accept(this);

		acceptor.iterator.accept(this);
		acceptor.collection.accept(this);
		acceptor.body.accept(this);
	}

	@Override
	public void visit(AstFunctionCallExpression acceptor) {
		int parSize = 4;

		for (AstExpression arg: acceptor.arguments) {
            Type argType = symbolDescription.getTypeForAstNode(arg);
            int size = argType.isReferenceType() ? 4 : argType.sizeInBytes(); // FIXME: -

            parSize += size;
        }

		currentFrame.sizeArgs = Math.max(currentFrame.sizeArgs, parSize);
	}

	@Override
	public void visit(AstFunctionDefinition acceptor) {
		FrmFrame frame = new FrmFrame(acceptor, currentLevel);

		frameDescription.setFrame(acceptor, frame);
		frameDescription.setAccess(acceptor, new FrmFunAccess(acceptor));

		FrmFrame tmp = currentFrame;
		currentFrame = frame;

		frame.parameterCount = acceptor.pars.size();

		for (AstParameterDefinition par : acceptor.pars) {
            par.accept(this);
        }

		currentLevel++;

		acceptor.functionCode.accept(this);

		currentFrame = tmp;
		currentLevel--;
	}

	@Override
	public void visit(AstIfStatement acceptor) {
		for (Condition c : acceptor.conditions) {
			c.condition.accept(this);
			c.body.accept(this);
		}

		if (acceptor.elseBody != null) {
            acceptor.elseBody.accept(this);
        }
	}

	@Override
	public void visit(AstParameterDefinition acceptor) {
		frameDescription.setAccess(acceptor, new FrmParAccess(acceptor, currentFrame, currentFrame.parametersSize));
		
		Type type = symbolDescription.getTypeForAstNode(acceptor);
		int size = type.isObjectType() ? 4 : type.sizeInBytes(); // FIXME: -
		
		currentFrame.parametersSize += size;
	}

	@Override
	public void visit(AstTypeName acceptor) {
        ///
	}

	@Override
	public void visit(AstUnaryExpression acceptor) {
		acceptor.expr.accept(this);
	}

	@Override
	public void visit(AstVariableDefinition acceptor) {
        AstDefinition parentDefinition = acceptor.getParentDefinition();
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
	public void visit(AstVariableNameExpression acceptor) {
        ///
	}

	@Override
	public void visit(AstWhileStatement acceptor) {
		acceptor.condition.accept(this);
		acceptor.body.accept(this);
	}

	@Override
	public void visit(AstImportDefinition importDef) {
		importDef.imports.accept(this);
	}

	@Override
	public void visit(AstStatements stmts) {
		for (AstStatement s : stmts.statements) {
			s.accept(this);
		}
	}

	@Override
	public void visit(AstReturnExpression returnExpr) {
		if (returnExpr.expr != null) 
			returnExpr.expr.accept(this);
	}

	@Override
	public void visit(AstListExpr absListExpr) {
		for (AstExpression e : absListExpr.expressions)
			e.accept(this);
	}

	@Override
	public void visit(AstFunctionType acceptor) {
		
	}

	@Override
	public void visit(AstControlTransferStatement acceptor) {
		///
	}

	@Override
	public void visit(AstSwitchStatement switchStmt) {
		switchStmt.subjectExpr.accept(this);
		
		for (AstCaseStatement singleCase : switchStmt.cases)
			singleCase.accept(this);
		
		if (switchStmt.defaultBody != null)
			switchStmt.defaultBody.accept(this);
	}

	@Override
	public void visit(AstCaseStatement acceptor) {
		for (AstExpression e : acceptor.exprs)
			e.accept(this);
		acceptor.body.accept(this);
	}

	@Override
	public void visit(AstEnumDefinition acceptor) {
		for (AstDefinition def : acceptor.definitions)
			def.accept(this);
	}

	@Override
	public void visit(AstEnumMemberDefinition acceptor) {
		///
	}

	@Override
	public void visit(AstTupleDefinition acceptor) {
		acceptor.definitions.accept(this);
	}

	@Override
	public void visit(AstLabeledExpr acceptor) {
		acceptor.expr.accept(this);
	}

	@Override
	public void visit(AstTupleExpression acceptor) {
		acceptor.expressions.accept(this);
	}
	
	@Override
	public void visit(AstOptionalType acceptor) {
		acceptor.childType.accept(this);
	}

	@Override
	public void visit(AstOptionalEvaluationExpression acceptor) {
		acceptor.subExpr.accept(this);
	}

	@Override
	public void visit(AstForceValueExpression acceptor) {
		acceptor.subExpr.accept(this);
	}

    @Override
    public void visit(AstExtensionDefinition acceptor) {
        acceptor.extendingType.accept(this);
        acceptor.definitions.accept(this);
    }

    @Override
    public void visit(AstInterfaceDefinition absInterfaceDef) {
        absInterfaceDef.definitions.accept(this);
    }
}
