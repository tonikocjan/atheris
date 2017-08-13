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
import compiler.ast.tree.type.AbsType;
import compiler.ast.tree.type.AbsTypeName;
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
	public void visit(AbsListType acceptor) {
		acceptor.type.accept(this);
	}

	@Override
	public void visit(AbsClassDef acceptor) {
        try {
            symbolTable.insertDefinitionOnCurrentScope(acceptor.getName(), acceptor);
        } catch (SemIllegalInsertException e) {
            Logger.error(acceptor.position, "Invalid redeclaration of \'" + acceptor.getName() + "\'");
        }

        if (acceptor.baseClass != null) {
            acceptor.baseClass.accept(this);
        }

        for (AbsType conformance : acceptor.conformances) {
            conformance.accept(this);
        }

        for (AbsFunDef constructor: acceptor.construstors) {
            // add implicit self: classType parameter to constructors
            AbsParDef parDef = new AbsParDef(constructor.position, Constants.selfParameterIdentifier,
                    new AbsAtomType(constructor.position, AtomTypeKind.NIL));

            constructor.addParamater(parDef);
            constructor.accept(this);
        }

        symbolTable.newScope();

        for (AbsDef def : acceptor.definitions.definitions) {
            if (def instanceof AbsFunDef && !def.isStatic()) {
                // add implicit self: classType parameter to instance methods
                AbsFunDef funDef = (AbsFunDef) def;

                AbsParDef parDef = new AbsParDef(funDef.position, Constants.selfParameterIdentifier,
                        new AbsAtomType(funDef.position, AtomTypeKind.NIL));
                funDef.addParamater(parDef);
            }

            // nested class and enum definitions are always static
            if (/*def instanceof AbsClassDef || */def instanceof AbsEnumDef) {
                def.setModifier(Modifier.isStatic);
            }

            def.accept(this);
        }

        symbolTable.oldScope();
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

		if (acceptor.oper != AbsBinExpr.DOT) {
            acceptor.expr2.accept(this);
		}
	}

	@Override
	public void visit(AbsDefs acceptor) {
		for (AbsDef def : acceptor.definitions) {
            def.accept(this);
        }
	}

	@Override
	public void visit(AbsExprs acceptor) {
		for (AbsExpr e : acceptor.expressions)
			e.accept(this);
	}

	@Override
	public void visit(AbsForStmt acceptor) {
		symbolTable.newScope();

		AbsVarDef var = new AbsVarDef(
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
	public void visit(AbsFunCall acceptor) {
        AbsFunDef definition = (AbsFunDef) symbolDescription.getDefinitionForAstNode(acceptor);

        if (definition == null) {
            String funCallIdentifier = acceptor.getStringRepresentation();
            definition = (AbsFunDef) symbolTable.findDefinitionForName(funCallIdentifier);

            if (definition == null) {
                // handle implicit "self" argument for constructors
                AbsVarNameExpr selfArgExpr = new AbsVarNameExpr(acceptor.position, Constants.selfParameterIdentifier);
                AbsLabeledExpr selfArg = new AbsLabeledExpr(acceptor.position, selfArgExpr, Constants.selfParameterIdentifier);

                acceptor.addArgument(selfArg);

                definition = (AbsFunDef) symbolTable.findDefinitionForName(acceptor.getStringRepresentation());
            }

            if (definition == null) {
                Logger.error(acceptor.position, "Method " + funCallIdentifier + " is undefined");
            }
        }

        boolean isConstructor = definition.isConstructor;
		symbolDescription.setDefinitionForAstNode(acceptor, definition);

		for (AbsExpr argExpr : acceptor.args) {
		    // skip first ("self") argument if function is constructor
            if (isConstructor && argExpr == acceptor.args.get(0))
                continue;

            argExpr.accept(this);
        }
	}

	@Override
	public void visit(AbsFunDef acceptor) {
		try {
			symbolTable.insertDefinitionOnCurrentScope(acceptor.getStringRepresentation(), acceptor);
		} catch (SemIllegalInsertException e) {
			Logger.error(acceptor.position, "Invalid redeclaration of \"" + acceptor.getStringRepresentation() + "\"");
		}
		
		symbolTable.newScope();

		for (AbsParDef par : acceptor.getParamaters())
			par.accept(this);
		
		acceptor.type.accept(this);
		acceptor.func.accept(this);

		symbolTable.oldScope();
	}

	@Override
	public void visit(AbsIfStmt acceptor) {
		for (Condition c : acceptor.conditions) {
			c.cond.accept(this);

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
	public void visit(AbsParDef acceptor) {
		try {
			symbolTable.insertDefinitionOnCurrentScope(acceptor.name, acceptor);
		} catch (SemIllegalInsertException e) {
			Logger.error(acceptor.position, "Duplicate parameter \"" + acceptor.name + "\"");
		}
		acceptor.type.accept(this);
	}

	@Override
	public void visit(AbsTypeName acceptor) {
		AbsDef definition = symbolTable.findDefinitionForName(acceptor.name);

		if (definition == null) {
            Logger.error(acceptor.position, "Type \"" + acceptor.name + "\" is undefined");
        }

		symbolDescription.setDefinitionForAstNode(acceptor, definition);
	}

	@Override
	public void visit(AbsUnExpr acceptor) {
		acceptor.expr.accept(this);
	}

	@Override
	public void visit(AbsVarDef acceptor) {
		try {
			symbolTable.insertDefinitionOnCurrentScope(acceptor.name, acceptor);
			if (acceptor.type != null)
				acceptor.type.accept(this);
		} catch (SemIllegalInsertException e) {
			Logger.error(acceptor.position, "Duplicate variable \"" + acceptor.name + "\"");
		}
	}

	@Override
	public void visit(AbsVarNameExpr acceptor) {
	    if (acceptor.name.equals("Int")) return;
        if (acceptor.name.equals("Double")) return;
        if (acceptor.name.equals("String")) return;
        if (acceptor.name.equals("Char")) return;
        if (acceptor.name.equals("Void")) return;

        if (symbolDescription.getDefinitionForAstNode(acceptor) != null) {
            return;
        }

		AbsDef definition = symbolTable.findDefinitionForName(acceptor.name);
		
		if (definition == null)
			Logger.error(acceptor.position, "Use of unresolved indentifier \"" + acceptor.name + "\"");

		symbolDescription.setDefinitionForAstNode(acceptor, definition);
	}

	@Override
	public void visit(AbsWhileStmt acceptor) {
		acceptor.cond.accept(this);

		symbolTable.newScope();
		acceptor.body.accept(this);
		symbolTable.oldScope();
	}

	@Override
	public void visit(AbsImportDef acceptor) {
		String tmp = Logger.fileName;
		Logger.fileName = acceptor.getName();

		// parse the file
		// FIXME: - Hardcoded location
		SynAn synAn = new SynAn(new LexAn("test/" + acceptor.getName() + ".ar", false), false);
		synAn.parseStandardLibrary = acceptor.getName().equals(Constants.standardLibraryIdentifier);

		AbsStmts source = (AbsStmts) synAn.parse();
        ArrayList<AbsDef> definitions = new ArrayList<>();
		
		for (AbsStmt s : source.statements) {
			// skip statements which are not definitions
			if (!(s instanceof AbsDef)) {
                continue;
            }

			AbsDef definition = (AbsDef) s;

			if (acceptor.definitions.size() > 0) {
				String name = definition.getName();

				if (!acceptor.definitions.contains(name)) {
                    continue;
                }
			}
			
			definitions.add(definition);
		}

		acceptor.imports = new AbsDefs(source.position, definitions);
		acceptor.imports.accept(this);

		Logger.fileName = tmp;
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
	public void visit(AbsFunType funType) {
		for (AbsType t : funType.parameterTypes)
			t.accept(this);
		funType.returnType.accept(this);
	}

	@Override
	public void visit(AbsControlTransferStmt transferStmt) {
		///
	}

	@Override
	public void visit(AbsSwitchStmt switchStmt) {
		switchStmt.subjectExpr.accept(this);
		
		for (AbsCaseStmt singleCase : switchStmt.cases)
			singleCase.accept(this);
		
		if (switchStmt.defaultBody != null) {
			symbolTable.newScope();
			switchStmt.defaultBody.accept(this);
			symbolTable.oldScope();
		}
	}

	@Override
	public void visit(AbsCaseStmt acceptor) {
		for (AbsExpr e : acceptor.exprs)
			e.accept(this);
		symbolTable.newScope();
		acceptor.body.accept(this);
		symbolTable.oldScope();
	}

	@Override
	public void visit(AbsEnumDef acceptor) {
		try {
			symbolTable.insertDefinitionOnCurrentScope(acceptor.name, acceptor);
		} catch (SemIllegalInsertException e) {
			Logger.error(acceptor.position, "Invalid redeclaration of \'" +
					acceptor.name + "\'");
		}

		if (acceptor.type != null)
			acceptor.type.accept(this);

		symbolTable.newScope();
		for (AbsDef def : acceptor.definitions)
			def.accept(this);
		symbolTable.oldScope();
	}

	@Override
	public void visit(AbsEnumMemberDef acceptor) {
		try {
			symbolTable.insertDefinitionOnCurrentScope(acceptor.name.name, acceptor);
		} catch (SemIllegalInsertException e) {
			Logger.error(acceptor.position, "Invalid redeclaration of \'" +
					acceptor.name.name + "\'");
		}

		acceptor.name.accept(this);
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

        symbolDescription.setDefinitionForAstNode(acceptor, symbolDescription.getDefinitionForAstNode(acceptor.subExpr));
	}

	@Override
	public void visit(AbsForceValueExpr acceptor) {
		acceptor.subExpr.accept(this);

		symbolDescription.setDefinitionForAstNode(acceptor, symbolDescription.getDefinitionForAstNode(acceptor.subExpr));
	}

    @Override
    public void visit(AbsExtensionDef acceptor) {
	    acceptor.extendingType.accept(this);

        for (AbsType conformance: acceptor.conformances) {
            conformance.accept(this);
        }

        symbolTable.newScope();
	    for (AbsDef def : acceptor.definitions.definitions) {
            if (def instanceof AbsFunDef) {
                if (!def.isStatic()) {
                    // add implicit self: classType parameter to instance methods
                    AbsFunDef funDef = (AbsFunDef) def;

                    AbsParDef parDef = new AbsParDef(
                            funDef.position,
                            Constants.selfParameterIdentifier,
                            new AbsAtomType(funDef.position, AtomTypeKind.NIL));
                    funDef.addParamater(parDef);
                }
            }
            else {
                Logger.error(def.position, "Only function definitions are allowed in extensions");
            }

            def.accept(this);
        }
        symbolTable.oldScope();
    }

    @Override
    public void visit(AbsInterfaceDef acceptor) {
        try {
            symbolTable.insertDefinitionOnCurrentScope(acceptor.getName(), acceptor);
        } catch (SemIllegalInsertException e) {
            Logger.error(acceptor.position, "Invalid redeclaration of \'" + acceptor.getName() + "\'");
        }

        for (AbsDef def : acceptor.definitions.definitions) {
            // add implicit self: classType parameter to instance methods
            AbsFunDef funDef = (AbsFunDef) def;
            AbsParDef parDef = new AbsParDef(funDef.position, Constants.selfParameterIdentifier,
                    new AbsAtomType(funDef.position, AtomTypeKind.NIL));
            funDef.addParamater(parDef);
        }
    }
}
