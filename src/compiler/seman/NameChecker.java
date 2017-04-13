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

import java.util.LinkedList;
import Utils.Constants;
import compiler.Report;
import compiler.abstr.ASTVisitor;
import compiler.abstr.tree.AbsDefs;
import compiler.abstr.tree.AbsExprs;
import compiler.abstr.tree.AbsStmt;
import compiler.abstr.tree.AbsStmts;
import compiler.abstr.tree.AtomTypeKind;
import compiler.abstr.tree.Condition;
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
import compiler.abstr.tree.type.AbsType;
import compiler.abstr.tree.type.AbsTypeName;
import compiler.frames.FrmDesc;
import compiler.frames.FrmFrame;
import compiler.frames.FrmLabel;
import compiler.lexan.LexAn;
import compiler.seman.type.AtomType;
import compiler.seman.type.FunctionType;
import compiler.seman.type.Type;
import compiler.synan.SynAn;

/**
 * Preverjanje in razresevanje imen (razen imen komponent).
 * 
 * @author Toni Kocjan
 */
public class NameChecker implements ASTVisitor {

	@Override
	public void visit(AbsListType acceptor) {
		acceptor.type.accept(this);
	}

	@Override
	public void visit(AbsClassDef acceptor) {
        SymbTable.newScope();

		for (AbsDef def : acceptor.definitions.definitions) {
			if (def instanceof AbsFunDef /* && !def.isStatic */) {
                // add implicit self: classType parameter to instance methods
                AbsFunDef funDef = (AbsFunDef) def;

                AbsParDef parDef = new AbsParDef(funDef.position, "self",
                        new AbsAtomType(funDef.position, AtomTypeKind.NIL));
                funDef.addParamater(parDef);
            }

            def.accept(this);
		}

		SymbTable.oldScope();

        try {
            SymbTable.ins(acceptor.getName(), acceptor);
        } catch (SemIllegalInsertException e) {
            Report.error(acceptor.position, "Invalid redeclaration of \'" + acceptor.getName()
                    + "\'");
        }

        if (acceptor.baseClass != null) {
            acceptor.baseClass.accept(this);
        }

        for (AbsFunDef constructor: acceptor.contrustors) {
            // add implicit self: classType parameter to constructors
            AbsParDef parDef = new AbsParDef(constructor.position, Constants.selfParameterIdentifier,
                    new AbsAtomType(constructor.position, AtomTypeKind.NIL));

            constructor.addParamater(parDef);
            constructor.accept(this);
        }
    }

	@Override
	public void visit(AbsAtomConstExpr acceptor) {

	}

	@Override
	public void visit(AbsAtomType acceptor) {

	}

	@Override
	public void visit(AbsBinExpr acceptor) {
		acceptor.expr1.accept(this);

		if (acceptor.oper == AbsBinExpr.DOT) {
			// handle implicit "self" argument for function calls
			// TODO: - handle static methods (when they are supported)
			if (acceptor.expr2 instanceof AbsFunCall) {
				AbsFunCall funCall = (AbsFunCall) acceptor.expr2;

				AbsLabeledExpr selfArg = new AbsLabeledExpr(acceptor.expr2.position, acceptor.expr1, Constants.selfParameterIdentifier);
				funCall.addArgument(selfArg);
			}
		}
		else {
			acceptor.expr2.accept(this);
		}
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
		SymbTable.newScope();

		AbsVarDef var = new AbsVarDef(
				acceptor.iterator.position, acceptor.iterator.name, null, false);
		try {
			SymbTable.ins(acceptor.iterator.name, var);
			SymbDesc.setNameDef(acceptor.iterator, var);
		} catch (SemIllegalInsertException e) {
			Report.error("Error @ NameChecker::AbsFor");
		}
		acceptor.iterator.accept(this);
		acceptor.collection.accept(this);

		acceptor.body.accept(this);
		SymbTable.oldScope();
	}

	@Override
	public void visit(AbsFunCall acceptor) {
	    String funCallIdentifier = acceptor.getStringRepresentation();
        AbsFunDef definition = (AbsFunDef) SymbTable.fnd(funCallIdentifier);

        // handle implicit "self" argument for constructors
        if (definition == null) {
            AbsVarNameExpr selfArgExpr = new AbsVarNameExpr(acceptor.position, Constants.selfParameterIdentifier);
            AbsLabeledExpr selfArg = new AbsLabeledExpr(acceptor.position, selfArgExpr, Constants.selfParameterIdentifier);

            acceptor.addArgument(selfArg);

            definition = (AbsFunDef) SymbTable.fnd(acceptor.getStringRepresentation());
        }

        if (definition == null) {
            Report.error(acceptor.position, "Method " + funCallIdentifier + " is undefined");
        }

        boolean isConstructor = definition.isConstructor;
		SymbDesc.setNameDef(acceptor, definition);

		for (AbsExpr argExpr : acceptor.args) {
		    // skip first ("self") argument if function is constructor
            if (isConstructor && argExpr == acceptor.args.firstElement())
                continue;

            argExpr.accept(this);
        }
	}

	@Override
	public void visit(AbsFunDef acceptor) {
		try {
			SymbTable.ins(acceptor.getStringRepresentation(), acceptor);
		} catch (SemIllegalInsertException e) {
			Report.error(acceptor.position, "Invalid redeclaration of \"" + acceptor.getStringRepresentation() + "\"");
		}
		
		SymbTable.newScope();

		for (AbsParDef par : acceptor.getParamaters())
			par.accept(this);
		
		acceptor.type.accept(this);
		acceptor.func.accept(this);

		SymbTable.oldScope();
	}

	@Override
	public void visit(AbsIfStmt acceptor) {
		for (Condition c : acceptor.conditions) {
			c.cond.accept(this);

			SymbTable.newScope();
			c.body.accept(this);
			SymbTable.oldScope();
		}

		if (acceptor.elseBody != null) {
			SymbTable.newScope();
			acceptor.elseBody.accept(this);
			SymbTable.oldScope();
		}
	}

	@Override
	public void visit(AbsParDef acceptor) {
		try {
			SymbTable.ins(acceptor.name, acceptor);
		} catch (SemIllegalInsertException e) {
			Report.error(acceptor.position, "Duplicate parameter \""
					+ acceptor.name + "\"");
		}
		acceptor.type.accept(this);
	}

	@Override
	public void visit(AbsTypeName acceptor) {
		AbsDef definition = SymbTable.fnd(acceptor.name);

		if (definition == null)
			Report.error(acceptor.position, "Type \"" + acceptor.name
					+ "\" is undefined");

		SymbDesc.setNameDef(acceptor, definition);
	}

	@Override
	public void visit(AbsUnExpr acceptor) {
		acceptor.expr.accept(this);
	}

	@Override
	public void visit(AbsVarDef acceptor) {
		try {
			SymbTable.ins(acceptor.name, acceptor);
			if (acceptor.type != null)
				acceptor.type.accept(this);
		} catch (SemIllegalInsertException e) {
			Report.error(acceptor.position, "Duplicate variable \""
					+ acceptor.name + "\"");
		}
	}

	@Override
	public void visit(AbsVarNameExpr acceptor) {
		AbsDef definition = SymbTable.fnd(acceptor.name);
		
		if (definition == null)
			Report.error(acceptor.position, "Use of unresolved indentifier \""
					+ acceptor.name + "\"");

		SymbDesc.setNameDef(acceptor, definition);
	}

	@Override
	public void visit(AbsWhileStmt acceptor) {
		acceptor.cond.accept(this);

		SymbTable.newScope();
		acceptor.body.accept(this);
		SymbTable.oldScope();
	}

	@Override
	public void visit(AbsImportDef acceptor) {
		String tmp = Report.fileName;
		Report.fileName = acceptor.getName();

		// parse the file
		// FIXME: - Hardcoded location
		SynAn synAn = new SynAn(new LexAn("test/" + acceptor.getName() + ".ar", false), false);
		synAn.parseStandardLibrary = acceptor.getName().equals(Constants.standardLibraryIdentifier);

		AbsStmts source = (AbsStmts) synAn.parse();
		LinkedList<AbsDef> definitions = new LinkedList<>();
		
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

		Report.fileName = tmp;
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
			SymbTable.newScope();
			switchStmt.defaultBody.accept(this);
			SymbTable.oldScope();
		}
	}

	@Override
	public void visit(AbsCaseStmt acceptor) {
		for (AbsExpr e : acceptor.exprs)
			e.accept(this);
		SymbTable.newScope();
		acceptor.body.accept(this);
		SymbTable.oldScope();
	}

	@Override
	public void visit(AbsEnumDef acceptor) {
		try {
			SymbTable.ins(acceptor.name, acceptor);
		} catch (SemIllegalInsertException e) {
			Report.error(acceptor.position, "Invalid redeclaration of \'" + 
					acceptor.name + "\'");
		}
		if (acceptor.type != null)
			acceptor.type.accept(this);
		SymbTable.newScope();
		for (AbsDef def : acceptor.definitions)
			def.accept(this);
		SymbTable.oldScope();
	}

	@Override
	public void visit(AbsEnumMemberDef acceptor) {
		try {
			SymbTable.ins(acceptor.name.name, acceptor);
		} catch (SemIllegalInsertException e) {
			Report.error(acceptor.position, "Invalid redeclaration of \'" + 
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

        SymbDesc.setNameDef(acceptor, SymbDesc.getNameDef(acceptor.subExpr));
	}

	@Override
	public void visit(AbsForceValueExpr acceptor) {
		acceptor.subExpr.accept(this);

		SymbDesc.setNameDef(acceptor, SymbDesc.getNameDef(acceptor.subExpr));
	}

    @Override
    public void visit(AbsExtensionDef acceptor) {

    }
}
