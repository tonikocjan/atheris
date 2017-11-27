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

import java.util.ArrayList;
import java.util.List;

import compiler.logger.LoggerFactory;
import compiler.logger.LoggerInterface;
import compiler.ast.tree.enums.AtomTypeKind;
import compiler.ast.tree.enums.DefinitionModifier;
import compiler.ast.tree.expr.*;
import compiler.ast.tree.stmt.*;
import compiler.ast.tree.type.*;
import utils.Constants;
import compiler.ast.ASTVisitor;
import compiler.ast.tree.*;
import compiler.ast.tree.def.*;
import compiler.ast.tree.expr.AstAtomConstExpression;
import compiler.ast.tree.stmt.AstCaseStatement;
import compiler.ast.tree.stmt.AstControlTransferStatement;
import compiler.ast.tree.type.AstAtomType;
import compiler.lexan.LexAn;
import compiler.synan.SynAn;

public class NameChecker implements ASTVisitor {

    private static LoggerInterface logger = LoggerFactory.logger();

    private SymbolTableMap symbolTable;
    private SymbolDescriptionMap symbolDescription;
    
    public NameChecker(SymbolTableMap symbolTable, SymbolDescriptionMap symbolDescription) {
        this.symbolTable = symbolTable;
        this.symbolDescription = symbolDescription;
    }

	@Override
	public void visit(AstListType acceptor) {
		acceptor.type.accept(this);
	}

	@Override
	public void visit(AstClassDefinition acceptor) {
        insertDefinition(acceptor);
        acceptBaseClass(acceptor);
        acceptConformances(acceptor.conformances);
        acceptConstructors(acceptor.construstors);
        symbolTable.newScope();
        acceptMemberDefinitions(acceptor.memberDefinitions);
        symbolTable.oldScope();
    }

    private void insertDefinition(AstDefinition acceptor) {
        try {
            symbolTable.insertDefinitionOnCurrentScope(acceptor.getName(), acceptor);
        } catch (SemIllegalInsertException e) {
            logger.error(acceptor.position, "Invalid redeclaration of \'" + acceptor.getName() + "\'");
        }
    }

    private void acceptBaseClass(AstClassDefinition acceptor) {
        if (acceptor.baseClass != null) {
            acceptor.baseClass.accept(this);
        }
    }

    private void acceptConformances(List<AstType> conformances) {
        for (AstType conformance : conformances) {
            conformance.accept(this);
        }
    }

    private void acceptConstructors(List<AstFunctionDefinition> constructors) {
        for (AstFunctionDefinition constructor: constructors) {
            addImplicitSelfParameterToMethod(constructor);
            constructor.accept(this);
        }
    }

    private void addImplicitSelfParameterToMethod(AstFunctionDefinition method) {
        AstParameterDefinition parDef = new AstParameterDefinition(
                method.position,
                Constants.selfParameterIdentifier,
                new AstAtomType(
                        method.position,
                        AtomTypeKind.NIL));

        method.addParamater(parDef);
    }

    private void acceptMemberDefinitions(AstDefinitions definitions) {
        for (AstDefinition definition : definitions.definitions) {
            if (definition instanceof AstFunctionDefinition && !definition.isStatic()) {
                addImplicitSelfParameterToMethod((AstFunctionDefinition) definition);
            }

            // nested class and enums definitions are always static
            if (/*def instanceof AstClassDefinition || */definition instanceof AstEnumDefinition) {
                definition.setModifier(DefinitionModifier.isStatic);
            }

            definition.accept(this);
        }
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

		if (acceptor.oper != AstBinaryExpression.DOT) {
            acceptor.expr2.accept(this);
		}
		else if (acceptor.expr2 instanceof AstFunctionCallExpression) {
            for (AstExpression arg: ((AstFunctionCallExpression) acceptor.expr2).arguments) {
                arg.accept(this);
            }
        }
	}

	@Override
	public void visit(AstDefinitions acceptor) {
		for (AstDefinition def : acceptor.definitions) {
            def.accept(this);
        }
	}

	@Override
	public void visit(AstExpressions acceptor) {
		for (AstExpression e : acceptor.expressions) {
            e.accept(this);
        }
	}

	@Override
	public void visit(AstForStatement acceptor) {
		AstVariableDefinition iterator = new AstVariableDefinition(acceptor.iterator.position, acceptor.iterator.name, null, false);

        symbolTable.newScope();

		insertDefinition(iterator);
		acceptor.iterator.accept(this);
		acceptor.collection.accept(this);
		acceptor.body.accept(this);

		symbolTable.oldScope();
	}

	@Override
	public void visit(AstFunctionCallExpression acceptor) {
        AstFunctionDefinition definition = findFunctionDefinitionForFunctionCall(acceptor);
        symbolDescription.setDefinitionForAstNode(acceptor, definition);
        acceptFunctionCallArguments(acceptor);
	}

	private AstFunctionDefinition findFunctionDefinitionForFunctionCall(AstFunctionCallExpression acceptor) {
        AstFunctionDefinition definition = (AstFunctionDefinition) symbolDescription.getDefinitionForAstNode(acceptor);

        if (definition == null) {
            String funCallIdentifier = acceptor.getStringRepresentation();
            definition = (AstFunctionDefinition) symbolTable.findDefinitionForName(funCallIdentifier);

            if (definition == null) {
                addImplicitSelfParameterToFunctionCall(acceptor);
                funCallIdentifier = acceptor.getStringRepresentation();
                definition = (AstFunctionDefinition) symbolTable.findDefinitionForName(funCallIdentifier);
            }

            if (definition == null) {
                logger.error(acceptor.position, "Method " + funCallIdentifier + " is undefined");
            }
        }

        return definition;
    }

	private void addImplicitSelfParameterToFunctionCall(AstFunctionCallExpression acceptor) {
        AstVariableNameExpression selfArgExpr = new AstVariableNameExpression(acceptor.position, Constants.selfParameterIdentifier);
        AstLabeledExpr selfArg = new AstLabeledExpr(acceptor.position, selfArgExpr, Constants.selfParameterIdentifier);
        acceptor.addArgument(selfArg);
    }

	private void acceptFunctionCallArguments(AstFunctionCallExpression acceptor) {
        boolean isConstructor = ((AstFunctionDefinition) symbolDescription.getDefinitionForAstNode(acceptor)).isConstructor;

        for (AstExpression argExpr : acceptor.arguments) {
            // skip first ("self") argument if function is constructor
            if (isConstructor && argExpr == acceptor.arguments.get(0))
                continue;

            argExpr.accept(this);
        }
    }

	@Override
	public void visit(AstFunctionDefinition acceptor) {
		insertDefinition(acceptor);
		symbolTable.newScope();
		acceptFunctionDefinitionArguments(acceptor);
		acceptor.returnType.accept(this);
		acceptor.functionCode.accept(this);
		symbolTable.oldScope();
	}

	private void acceptFunctionDefinitionArguments(AstFunctionDefinition acceptor) {
        for (AstParameterDefinition par : acceptor.getParamaters()) {
            par.accept(this);
        }
    }

	@Override
	public void visit(AstIfStatement acceptor) {
		acceptIfConditions(acceptor);
        acceptElseBody(acceptor);
	}

	private void acceptIfConditions(AstIfStatement acceptor) {
        for (Condition c : acceptor.conditions) {
            c.condition.accept(this);
            symbolTable.newScope();
            c.body.accept(this);
            symbolTable.oldScope();
        }
    }

    private void acceptElseBody(AstIfStatement acceptor) {
        if (acceptor.elseBody == null) { return; }

        symbolTable.newScope();
        acceptor.elseBody.accept(this);
        symbolTable.oldScope();
    }

	@Override
	public void visit(AstParameterDefinition acceptor) {
		insertDefinition(acceptor);
		acceptor.type.accept(this);
	}

	@Override
	public void visit(AstTypeName acceptor) {
		AstDefinition definition = symbolTable.findDefinitionForName(acceptor.name);

		if (definition == null) {
            logger.error(acceptor.position, "Type \"" + acceptor.name + "\" is undefined");
        }

		symbolDescription.setDefinitionForAstNode(acceptor, definition);
	}

	@Override
	public void visit(AstUnaryExpression acceptor) {
		acceptor.expr.accept(this);
	}

	@Override
	public void visit(AstVariableDefinition acceptor) {
        insertDefinition(acceptor);
        acceptVariableDefinitionType(acceptor);
	}

	private void acceptVariableDefinitionType(AstVariableDefinition acceptor) {
        if (acceptor.type != null) {
            acceptor.type.accept(this);
        }
    }

	@Override
	public void visit(AstVariableNameExpression acceptor) {
	    if (isAtomType(acceptor.name)) return;
        if (symbolDescription.getDefinitionForAstNode(acceptor) != null) return;

		AstDefinition definition = symbolTable.findDefinitionForName(acceptor.name);
		
		if (definition == null) {
            logger.error(acceptor.position, "Use of unresolved indentifier \"" + acceptor.name + "\"");
        }

		symbolDescription.setDefinitionForAstNode(acceptor, definition);
	}

	private boolean isAtomType(String name) {
        if (name.equals("Int")) return true;
        if (name.equals("Double")) return true;
        if (name.equals("String")) return true;
        if (name.equals("Char")) return true;
        if (name.equals("Void")) return true;
        return false;
    }

	@Override
	public void visit(AstWhileStatement acceptor) {
		acceptor.condition.accept(this);
		symbolTable.newScope();
		acceptor.body.accept(this);
		symbolTable.oldScope();
	}

	@Override
	public void visit(AstImportDefinition acceptor) {
		String currentFile = logger.getFileName();
		logger.setFileName(acceptor.getName());

        ArrayList<AstDefinition> definitions = new ArrayList<>();

        AstStatements statements = parseFile(acceptor.getName());
		for (AstStatement statement : statements.statements) {
			if (!isDefinition(statement)) { continue; }

			AstDefinition definition = (AstDefinition) statement;

			if (!acceptor.selectedDefinitions.isEmpty()) {
				String name = definition.getName();

				if (!acceptor.selectedDefinitions.contains(name)) {
                    continue;
                }
			}
			
			definitions.add(definition);
		}

		acceptor.imports = new AstDefinitions(statements.position, definitions);
		acceptor.imports.accept(this);

		logger.setFileName(currentFile);
	}

	private AstStatements parseFile(String fileName) {
        // FIXME: - Hardcoded location
        SynAn synAn = new SynAn(LexAn.parseSourceFile("test/" + fileName + ".ar", false), false);
        return (AstStatements) synAn.parse();
    }

    private boolean isDefinition(AstStatement statement) {
        return statement instanceof AstDefinition;
    }

	@Override
	public void visit(AstStatements acceptor) {
		for (AstStatement s : acceptor.statements) {
			s.accept(this);
		}
	}

	@Override
	public void visit(AstReturnExpression acceptor) {
		if (acceptor.expr != null) {
            acceptor.expr.accept(this);
        }
	}

	@Override
	public void visit(AstListExpr acceptor) {
		for (AstExpression e : acceptor.expressions) {
            e.accept(this);
        }
	}

	@Override
	public void visit(AstFunctionType acceptor) {
		for (AstType t : acceptor.parameterTypes) {
            t.accept(this);
        }

		acceptor.returnType.accept(this);
	}

	@Override
	public void visit(AstControlTransferStatement acceptor) {
		///
	}

	@Override
	public void visit(AstSwitchStatement acceptor) {
		acceptor.subjectExpr.accept(this);
		acceptCaseStatements(acceptor);
		acceptDefaultBody(acceptor);
	}

	private void acceptCaseStatements(AstSwitchStatement acceptor) {
        for (AstCaseStatement singleCase : acceptor.cases) {
            singleCase.accept(this);
        }
    }

    private void acceptDefaultBody(AstSwitchStatement acceptor) {
        if (acceptor.defaultBody == null) { return; }

        symbolTable.newScope();
        acceptor.defaultBody.accept(this);
        symbolTable.oldScope();
    }

	@Override
	public void visit(AstCaseStatement acceptor) {
		for (AstExpression e : acceptor.exprs) {
            e.accept(this);
        }

		symbolTable.newScope();
		acceptor.body.accept(this);
		symbolTable.oldScope();
	}

	@Override
	public void visit(AstEnumDefinition acceptor) {
		insertDefinition(acceptor);
        acceptEnumDefinitionType(acceptor);
		symbolTable.newScope();
		acceptEnumDefinitionMemberDefinitions(acceptor);
		symbolTable.oldScope();
	}

	private void acceptEnumDefinitionType(AstEnumDefinition acceptor) {
        if (acceptor.type != null) {
            acceptor.type.accept(this);
        }
    }

    private void acceptEnumDefinitionMemberDefinitions(AstEnumDefinition acceptor) {
        for (AstDefinition def : acceptor.definitions) {
            def.accept(this);
        }
    }

	@Override
	public void visit(AstEnumMemberDefinition acceptor) {
		insertDefinition(acceptor);
		acceptor.name.accept(this);
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
		AstDefinition definition = symbolDescription.getDefinitionForAstNode(acceptor.subExpr);
        symbolDescription.setDefinitionForAstNode(acceptor, definition);
	}

	@Override
	public void visit(AstForceValueExpression acceptor) {
		acceptor.subExpr.accept(this);
		AstDefinition definition = symbolDescription.getDefinitionForAstNode(acceptor.subExpr);
		symbolDescription.setDefinitionForAstNode(acceptor, definition);
	}

    @Override
    public void visit(AstExtensionDefinition acceptor) {
	    acceptor.extendingType.accept(this);
	    acceptConformances(acceptor.conformances);
        symbolTable.newScope();
        acceptMemberDefinitions(acceptor.definitions);
        symbolTable.oldScope();
    }

    @Override
    public void visit(AstInterfaceDefinition acceptor) {
        insertDefinition(acceptor);

        for (AstDefinition functionDefinition : acceptor.definitions.definitions) {
            addImplicitSelfParameterToMethod((AstFunctionDefinition) functionDefinition);
        }
    }
}
