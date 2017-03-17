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
import java.util.Vector;

import compiler.Report;
import compiler.abstr.ASTVisitor;
import compiler.abstr.tree.AbsDefs;
import compiler.abstr.tree.AbsExprs;
import compiler.abstr.tree.AbsStmt;
import compiler.abstr.tree.AbsStmts;
import compiler.abstr.tree.AtomTypeKind;
import compiler.abstr.tree.Condition;
import compiler.abstr.tree.def.AbsClassDef;
import compiler.abstr.tree.def.AbsDef;
import compiler.abstr.tree.def.AbsEnumDef;
import compiler.abstr.tree.def.AbsEnumMemberDef;
import compiler.abstr.tree.def.AbsFunDef;
import compiler.abstr.tree.def.AbsImportDef;
import compiler.abstr.tree.def.AbsParDef;
import compiler.abstr.tree.def.AbsTupleDef;
import compiler.abstr.tree.def.AbsVarDef;
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
	
	public NameChecker() {
		try {
			{
				String name = "print";
				LinkedList<AbsParDef> pars = new LinkedList<>();
				Vector<Type> parTypes = new Vector<>();
				
				parTypes.add(new AtomType(AtomTypeKind.INT));
				pars.add(new AbsParDef(null, "0", new AbsAtomType(null,
						AtomTypeKind.INT)));

				AbsFunDef print = new AbsFunDef(null, name, pars,
						new AbsAtomType(null, AtomTypeKind.VOID), new AbsStmts(
								null, new LinkedList<>()));
				SymbTable.ins(print.getStringRepresentation(), print);
				SymbDesc.setType(print, new FunctionType(parTypes,
						new AtomType(AtomTypeKind.VOID), print));

				FrmFrame frame = new FrmFrame(print, 1);
				frame.numPars = 1;
				frame.sizePars = 4;
				frame.label = FrmLabel.newLabel(name);
				FrmDesc.setFrame(print, frame);
			}
			{
				String name = "print";
				LinkedList<AbsParDef> pars = new LinkedList<>();
				Vector<Type> parTypes = new Vector<>();
				parTypes.add(new AtomType(AtomTypeKind.DOB));

				pars.add(new AbsParDef(null, "x", new AbsAtomType(null,
						AtomTypeKind.DOB)));

				AbsFunDef print = new AbsFunDef(null, name, pars,
						new AbsAtomType(null, AtomTypeKind.VOID), new AbsStmts(
								null, new LinkedList<>()));
				SymbTable.ins(print.getStringRepresentation(), print);
				SymbDesc.setType(print, new FunctionType(parTypes,
						new AtomType(AtomTypeKind.VOID), print));

				FrmFrame frame = new FrmFrame(print, 1);
				frame.numPars = 1;
				frame.sizePars = 4;
				frame.label = FrmLabel.newLabel(name);
				FrmDesc.setFrame(print, frame);
			}
			{
				String name = "print";
				LinkedList<AbsParDef> pars = new LinkedList<>();
				Vector<Type> parTypes = new Vector<>();
				parTypes.add(new AtomType(AtomTypeKind.STR));

				pars.add(new AbsParDef(null, "x", new AbsAtomType(null,
						AtomTypeKind.INT)));

				AbsFunDef print = new AbsFunDef(null, name, pars,
						new AbsAtomType(null, AtomTypeKind.VOID), new AbsStmts(
								null, new LinkedList<>()));
				SymbTable.ins(print.getStringRepresentation(), print);
				SymbDesc.setType(print, new FunctionType(parTypes,
						new AtomType(AtomTypeKind.VOID), print));

				FrmFrame frame = new FrmFrame(print, 1);
				frame.numPars = 1;
				frame.sizePars = 4;
				frame.label = FrmLabel.newLabel(name);
				FrmDesc.setFrame(print, frame);
			}
			{
				String name = "print";
				LinkedList<AbsParDef> pars = new LinkedList<>();
				Vector<Type> parTypes = new Vector<>();
				parTypes.add(new AtomType(AtomTypeKind.CHR));

				pars.add(new AbsParDef(null, "x", new AbsAtomType(null,
						AtomTypeKind.CHR)));

				AbsFunDef print = new AbsFunDef(null, name, pars,
						new AbsAtomType(null, AtomTypeKind.VOID), new AbsStmts(
								null, new LinkedList<>()));
				SymbTable.ins(print.getStringRepresentation(), print);
				SymbDesc.setType(print, new FunctionType(parTypes,
						new AtomType(AtomTypeKind.VOID), print));

				FrmFrame frame = new FrmFrame(print, 1);
				frame.numPars = 1;
				frame.sizePars = 4;
				frame.label = FrmLabel.newLabel(name);
				FrmDesc.setFrame(print, frame);
			}
			{
				String name = "print";
				LinkedList<AbsParDef> pars = new LinkedList<>();
				Vector<Type> parTypes = new Vector<>();
				parTypes.add(new AtomType(AtomTypeKind.LOG));

				pars.add(new AbsParDef(null, "x", new AbsAtomType(null,
						AtomTypeKind.LOG)));

				AbsFunDef print = new AbsFunDef(null, name, pars,
						new AbsAtomType(null, AtomTypeKind.VOID), new AbsStmts(
								null, new LinkedList<>()));
				SymbTable.ins(print.getStringRepresentation(), print);
				SymbDesc.setType(print, new FunctionType(parTypes,
						new AtomType(AtomTypeKind.VOID), print));

				FrmFrame frame = new FrmFrame(print, 1);
				frame.numPars = 1;
				frame.sizePars = 4;
				frame.label = FrmLabel.newLabel(name);
				FrmDesc.setFrame(print, frame);
			}
			{
				String name = "time";
				LinkedList<AbsParDef> pars = new LinkedList<>();
				Vector<Type> parTypes = new Vector<>();
				AbsFunDef time = new AbsFunDef(null, name, pars,
						new AbsAtomType(null, AtomTypeKind.INT), new AbsStmts(
								null, new LinkedList<>()));
				SymbTable.ins(time.getStringRepresentation(), time);
				SymbTable.ins("time", time);
				SymbDesc.setType(time, new FunctionType(parTypes,
						new AtomType(AtomTypeKind.INT), time));

				FrmFrame frame = new FrmFrame(time, 1);
				frame.numPars = 0;
				frame.sizePars = 0;
				frame.label = FrmLabel.newLabel(name);
				FrmDesc.setFrame(time, frame);
			}
			{
				String name = "rand";
				LinkedList<AbsParDef> pars = new LinkedList<>();
				Vector<Type> parTypes = new Vector<>();
				AbsFunDef rand = new AbsFunDef(null, name, pars,
						new AbsAtomType(null, AtomTypeKind.INT), new AbsStmts(
								null, new LinkedList<>()));
				SymbTable.ins(rand.getStringRepresentation(), rand);
				SymbTable.ins("rand", rand);
				SymbDesc.setType(rand, new FunctionType(parTypes,
						new AtomType(AtomTypeKind.INT), rand));

				FrmFrame frame = new FrmFrame(rand, 1);
				frame.numPars = 0;
				frame.sizePars = 0;
				frame.label = FrmLabel.newLabel(name);
				FrmDesc.setFrame(rand, frame);
			}
			{
				String name = "rand";
				LinkedList<AbsParDef> pars = new LinkedList<>();
				Vector<Type> parTypes = new Vector<>();
				parTypes.add(new AtomType(AtomTypeKind.INT));
				pars.add(new AbsParDef(null, "bound", new AbsAtomType(null,
						AtomTypeKind.INT)));

				AbsFunDef rand = new AbsFunDef(null, name, pars,
						new AbsAtomType(null, AtomTypeKind.INT), new AbsStmts(
								null, new LinkedList<>()));
				SymbTable.ins(rand.getStringRepresentation(), rand);
				SymbDesc.setType(rand, new FunctionType(parTypes,
						new AtomType(AtomTypeKind.INT), rand));

				FrmFrame frame = new FrmFrame(rand, 1);
				frame.numPars = 0;
				frame.sizePars = 0;
				frame.label = FrmLabel.newLabel(name);
				FrmDesc.setFrame(rand, frame);
			}
		} catch (Exception e) {
		}
	}

	@Override
	public void visit(AbsListType acceptor) {
		acceptor.type.accept(this);
	}

	@Override
	public void visit(AbsClassDef acceptor) {
		try {
			SymbTable.ins(acceptor.getName(), acceptor);
		} catch (SemIllegalInsertException e) {
			Report.error(acceptor.position, "Invalid redeclaration of \'" + acceptor.getName()
					+ "\'");
		}
		
		for (AbsFunDef constructor: acceptor.contrustors) {
			// insert constructor into symbol table
			try {
				SymbTable.ins(constructor.getStringRepresentation(), constructor);
			} catch (SemIllegalInsertException e) {
				Report.error(acceptor.position, "Duplicate method \""
						+ acceptor.name + "\"");
			}
			
			for (AbsStmt s : constructor.func.statements) {
				AbsBinExpr assignExpr = (AbsBinExpr) s;
				String name = ((AbsVarNameExpr) assignExpr.expr1).name;
				
				SymbDesc.setNameDef(assignExpr.expr1, 
						acceptor.definitions.findDefinitionForName(name));
			}
		}
		
		SymbTable.newScope();		
		for (AbsDef def : acceptor.definitions.definitions) {
			if (def instanceof AbsFunDef) {
				// add implicit self: classType parameter to instance methods
				AbsFunDef funDef = (AbsFunDef) def;
				
				// FIXME: - Position
				AbsParDef parDef = new AbsParDef(funDef.position, "self", 
						new AbsAtomType(funDef.position, AtomTypeKind.INT));
				funDef.addParamater(parDef);
				
				def.accept(this);
			}
		}
		SymbTable.oldScope();
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
			// TODO: - handle static methods (when they are added)
			if (acceptor.expr2 instanceof AbsFunCall) {
				AbsFunCall funCall = (AbsFunCall) acceptor.expr2;
				
				// TODO: - Fix position
				AbsLabeledExpr selfArg = new AbsLabeledExpr(acceptor.expr2.position, acceptor.expr1, "self");
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
		AbsDef definition = SymbTable.fnd(acceptor.getStringRepresentation());
		if (definition == null)
			Report.error(acceptor.position, "Method " + acceptor.getStringRepresentation() + " is undefined");
		
		SymbDesc.setNameDef(acceptor, definition);
		
		for (int arg = 0; arg < acceptor.numArgs(); arg++)
			acceptor.arg(arg).accept(this);
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
		// TODO hardcodano notr test/
		SynAn synAn = new SynAn(new LexAn("test/" + acceptor.getName() + ".ar",
				false), false);
		AbsStmts source = (AbsStmts) synAn.parse();

		LinkedList<AbsDef> definitions = new LinkedList<>();
		
		for (AbsStmt s : source.statements) {
			// skip statements which are not definitions
			if (!(s instanceof AbsDef))
				continue;

			AbsDef definition = (AbsDef) s;

			if (acceptor.definitions.size() > 0) {
				String name = definition.getName();

				if (!acceptor.definitions.contains(name))
					continue;
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
	}

	@Override
	public void visit(AbsForceValueExpr acceptor) {
		acceptor.subExpr.accept(this);
	}
}
