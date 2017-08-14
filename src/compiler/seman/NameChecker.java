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

import compiler.Logger;
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
        try {
            symbolTable.insertDefinitionOnCurrentScope(acceptor.getName(), acceptor);
        } catch (SemIllegalInsertException e) {
            Logger.error(acceptor.position, "Invalid redeclaration of \'" + acceptor.getName() + "\'");
        }

        if (acceptor.baseClass != null) {
            acceptor.baseClass.accept(this);
        }

        for (AstType conformance : acceptor.conformances) {
            conformance.accept(this);
        }

        for (AstFunctionDefinition constructor: acceptor.construstors) {
            // add implicit self: classType parameter to constructors
            AstParameterDefinition parDef = new AstParameterDefinition(constructor.position, Constants.selfParameterIdentifier,
                    new AstAtomType(constructor.position, AtomTypeKind.NIL));

            constructor.addParamater(parDef);
            constructor.accept(this);
        }

        symbolTable.newScope();

        for (AstDefinition def : acceptor.memberDefinitions.definitions) {
            if (def instanceof AstFunctionDefinition && !def.isStatic()) {
                // add implicit self: classType parameter to instance methods
                AstFunctionDefinition funDef = (AstFunctionDefinition) def;

                AstParameterDefinition parDef = new AstParameterDefinition(funDef.position, Constants.selfParameterIdentifier,
                        new AstAtomType(funDef.position, AtomTypeKind.NIL));
                funDef.addParamater(parDef);
            }

            // nested class and enum memberDefinitions are always static
            if (/*def instanceof AstClassDefinition || */def instanceof AstEnumDefinition) {
                def.setModifier(DefinitionModifier.isStatic);
            }

            def.accept(this);
        }

        symbolTable.oldScope();
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
	}

	@Override
	public void visit(AstDefinitions acceptor) {
		for (AstDefinition def : acceptor.definitions) {
            def.accept(this);
        }
	}

	@Override
	public void visit(AstExpressions acceptor) {
		for (AstExpression e : acceptor.expressions)
			e.accept(this);
	}

	@Override
	public void visit(AstForStatement acceptor) {
		symbolTable.newScope();

		AstVariableDefinition var = new AstVariableDefinition(
				acceptor.iterator.position, acceptor.iterator.name, null, false);
		try {
			symbolTable.insertDefinitionOnCurrentScope(acceptor.iterator.name, var);
			symbolDescription.setDefinitionForAstNode(acceptor.iterator, var);
		} catch (SemIllegalInsertException e) {
			Logger.error("Error @ NameChecker::AbsFor");
		}
		acceptor.iterator.accept(this);
		acceptor.collection.accept(this);

		acceptor.body.accept(this);
		symbolTable.oldScope();
	}

	@Override
	public void visit(AstFunctionCallExpression acceptor) {
        AstFunctionDefinition definition = (AstFunctionDefinition) symbolDescription.getDefinitionForAstNode(acceptor);

        if (definition == null) {
            String funCallIdentifier = acceptor.getStringRepresentation();
            definition = (AstFunctionDefinition) symbolTable.findDefinitionForName(funCallIdentifier);

            if (definition == null) {
                // handle implicit "self" argument for constructors
                AstVariableNameExpression selfArgExpr = new AstVariableNameExpression(acceptor.position, Constants.selfParameterIdentifier);
                AstLabeledExpr selfArg = new AstLabeledExpr(acceptor.position, selfArgExpr, Constants.selfParameterIdentifier);

                acceptor.addArgument(selfArg);

                definition = (AstFunctionDefinition) symbolTable.findDefinitionForName(acceptor.getStringRepresentation());
            }

            if (definition == null) {
                Logger.error(acceptor.position, "Method " + funCallIdentifier + " is undefined");
            }
        }

        boolean isConstructor = definition.isConstructor;
		symbolDescription.setDefinitionForAstNode(acceptor, definition);

		for (AstExpression argExpr : acceptor.arguments) {
		    // skip first ("self") argument if function is constructor
            if (isConstructor && argExpr == acceptor.arguments.get(0))
                continue;

            argExpr.accept(this);
        }
	}

	@Override
	public void visit(AstFunctionDefinition acceptor) {
		try {
			symbolTable.insertDefinitionOnCurrentScope(acceptor.getStringRepresentation(), acceptor);
		} catch (SemIllegalInsertException e) {
			Logger.error(acceptor.position, "Invalid redeclaration of \"" + acceptor.getStringRepresentation() + "\"");
		}
		
		symbolTable.newScope();

		for (AstParameterDefinition par : acceptor.getParamaters())
			par.accept(this);
		
		acceptor.returnType.accept(this);
		acceptor.functionCode.accept(this);

		symbolTable.oldScope();
	}

	@Override
	public void visit(AstIfStatement acceptor) {
		for (Condition c : acceptor.conditions) {
			c.condition.accept(this);

			symbolTable.newScope();
			c.body.accept(this);
			symbolTable.oldScope();
		}

		if (acceptor.elseBody != null) {
			symbolTable.newScope();
			acceptor.elseBody.accept(this);
			symbolTable.oldScope();
		}
	}

	@Override
	public void visit(AstParameterDefinition acceptor) {
		try {
			symbolTable.insertDefinitionOnCurrentScope(acceptor.name, acceptor);
		} catch (SemIllegalInsertException e) {
			Logger.error(acceptor.position, "Duplicate parameter \"" + acceptor.name + "\"");
		}
		acceptor.type.accept(this);
	}

	@Override
	public void visit(AstTypeName acceptor) {
		AstDefinition definition = symbolTable.findDefinitionForName(acceptor.name);

		if (definition == null) {
            Logger.error(acceptor.position, "Type \"" + acceptor.name + "\" is undefined");
        }

		symbolDescription.setDefinitionForAstNode(acceptor, definition);
	}

	@Override
	public void visit(AstUnaryExpression acceptor) {
		acceptor.expr.accept(this);
	}

	@Override
	public void visit(AstVariableDefinition acceptor) {
		try {
			symbolTable.insertDefinitionOnCurrentScope(acceptor.name, acceptor);
			if (acceptor.type != null)
				acceptor.type.accept(this);
		} catch (SemIllegalInsertException e) {
			Logger.error(acceptor.position, "Duplicate variable \"" + acceptor.name + "\"");
		}
	}

	@Override
	public void visit(AstVariableNameExpression acceptor) {
	    if (acceptor.name.equals("Int")) return;
        if (acceptor.name.equals("Double")) return;
        if (acceptor.name.equals("String")) return;
        if (acceptor.name.equals("Char")) return;
        if (acceptor.name.equals("Void")) return;

        if (symbolDescription.getDefinitionForAstNode(acceptor) != null) {
            return;
        }

		AstDefinition definition = symbolTable.findDefinitionForName(acceptor.name);
		
		if (definition == null)
			Logger.error(acceptor.position, "Use of unresolved indentifier \"" + acceptor.name + "\"");

		symbolDescription.setDefinitionForAstNode(acceptor, definition);
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
		String tmp = Logger.fileName;
		Logger.fileName = acceptor.getName();

		// parse the file
		// FIXME: - Hardcoded location
		SynAn synAn = new SynAn(LexAn.parseSourceFile("test/" + acceptor.getName() + ".ar", false), false);
		synAn.parseStandardLibrary = acceptor.getName().equals(Constants.standardLibraryIdentifier);

		AstStatements source = (AstStatements) synAn.parse();
        ArrayList<AstDefinition> definitions = new ArrayList<>();
		
		for (AstStatement s : source.statements) {
			// skip statements which are not memberDefinitions
			if (!(s instanceof AstDefinition)) {
                continue;
            }

			AstDefinition definition = (AstDefinition) s;

			if (acceptor.definitions.size() > 0) {
				String name = definition.getName();

				if (!acceptor.definitions.contains(name)) {
                    continue;
                }
			}
			
			definitions.add(definition);
		}

		acceptor.imports = new AstDefinitions(source.position, definitions);
		acceptor.imports.accept(this);

		Logger.fileName = tmp;
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
	public void visit(AstFunctionType funType) {
		for (AstType t : funType.parameterTypes)
			t.accept(this);
		funType.returnType.accept(this);
	}

	@Override
	public void visit(AstControlTransferStatement transferStmt) {
		///
	}

	@Override
	public void visit(AstSwitchStatement switchStmt) {
		switchStmt.subjectExpr.accept(this);
		
		for (AstCaseStatement singleCase : switchStmt.cases)
			singleCase.accept(this);
		
		if (switchStmt.defaultBody != null) {
			symbolTable.newScope();
			switchStmt.defaultBody.accept(this);
			symbolTable.oldScope();
		}
	}

	@Override
	public void visit(AstCaseStatement acceptor) {
		for (AstExpression e : acceptor.exprs)
			e.accept(this);
		symbolTable.newScope();
		acceptor.body.accept(this);
		symbolTable.oldScope();
	}

	@Override
	public void visit(AstEnumDefinition acceptor) {
		try {
			symbolTable.insertDefinitionOnCurrentScope(acceptor.name, acceptor);
		} catch (SemIllegalInsertException e) {
			Logger.error(acceptor.position, "Invalid redeclaration of \'" +
					acceptor.name + "\'");
		}

		if (acceptor.type != null)
			acceptor.type.accept(this);

		symbolTable.newScope();
		for (AstDefinition def : acceptor.definitions)
			def.accept(this);
		symbolTable.oldScope();
	}

	@Override
	public void visit(AstEnumMemberDefinition acceptor) {
		try {
			symbolTable.insertDefinitionOnCurrentScope(acceptor.name.name, acceptor);
		} catch (SemIllegalInsertException e) {
			Logger.error(acceptor.position, "Invalid redeclaration of \'" +
					acceptor.name.name + "\'");
		}

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

        symbolDescription.setDefinitionForAstNode(acceptor, symbolDescription.getDefinitionForAstNode(acceptor.subExpr));
	}

	@Override
	public void visit(AstForceValueExpression acceptor) {
		acceptor.subExpr.accept(this);

		symbolDescription.setDefinitionForAstNode(acceptor, symbolDescription.getDefinitionForAstNode(acceptor.subExpr));
	}

    @Override
    public void visit(AstExtensionDefinition acceptor) {
	    acceptor.extendingType.accept(this);

        for (AstType conformance: acceptor.conformances) {
            conformance.accept(this);
        }

        symbolTable.newScope();
	    for (AstDefinition def : acceptor.definitions.definitions) {
            if (def instanceof AstFunctionDefinition) {
                if (!def.isStatic()) {
                    // add implicit self: classType parameter to instance methods
                    AstFunctionDefinition funDef = (AstFunctionDefinition) def;

                    AstParameterDefinition parDef = new AstParameterDefinition(
                            funDef.position,
                            Constants.selfParameterIdentifier,
                            new AstAtomType(funDef.position, AtomTypeKind.NIL));
                    funDef.addParamater(parDef);
                }
            }
            else {
                Logger.error(def.position, "Only function memberDefinitions are allowed in extensions");
            }

            def.accept(this);
        }
        symbolTable.oldScope();
    }

    @Override
    public void visit(AstInterfaceDefinition acceptor) {
        try {
            symbolTable.insertDefinitionOnCurrentScope(acceptor.getName(), acceptor);
        } catch (SemIllegalInsertException e) {
            Logger.error(acceptor.position, "Invalid redeclaration of \'" + acceptor.getName() + "\'");
        }

        for (AstDefinition def : acceptor.definitions.definitions) {
            // add implicit self: classType parameter to instance methods
            AstFunctionDefinition funDef = (AstFunctionDefinition) def;
            AstParameterDefinition parDef = new AstParameterDefinition(funDef.position, Constants.selfParameterIdentifier,
                    new AstAtomType(funDef.position, AtomTypeKind.NIL));
            funDef.addParamater(parDef);
        }
    }
}
