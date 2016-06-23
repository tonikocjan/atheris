package compiler.seman;

import java.util.Vector;

import compiler.Report;
import compiler.abstr.*;
import compiler.abstr.tree.*;
import compiler.lexan.LexAn;
import compiler.synan.SynAn;

/**
 * Preverjanje in razresevanje imen (razen imen komponent).
 * 
 * @implementation Toni Kocjan
 */
public class NameChecker implements Visitor {

	private enum TraversalState {
		ETS_importDefinitions, ETS_variableDefinitions, ETS_prototypes, ETS_functions
	}

	private TraversalState currentState;
	private AbsFunDef main = null;

	public AbsFunDef getMain() {
		return main;
	}

	public NameChecker() {
//		try {
//			{
//				Vector<AbsPar> pars = new Vector<>();
//				Vector<SemType> parTypes = new Vector<>();
//				parTypes.add(new SemAtomType(SemAtomType.INT));
//
//				pars.add(new AbsPar(null, "x", new AbsAtomType(null,
//						AbsAtomType.INT)));
//
//				AbsFunDef putInt = new AbsFunDef(null, "putInt", pars,
//						new AbsAtomType(null, AbsAtomType.INT), new AbsExpr(
//								null) {
//					@Override
//					public void accept(Visitor visitor) {
//					}
//				});
//				SymbTable.ins("putInt", putInt);
//				SymbDesc.setType(putInt, new SemFunType(parTypes,
//						new SemAtomType(SemAtomType.INT)));
//
//				FrmFrame frame = new FrmFrame(putInt, 1);
//				frame.numPars = 1;
//				frame.sizePars = 4;
//				frame.label = FrmLabel.newLabel("putInt");
//				FrmDesc.setFrame(putInt, frame);
//			}
//			{
//				Vector<AbsPar> pars = new Vector<>();
//				Vector<SemType> parTypes = new Vector<>();
//				parTypes.add(new SemPtrType(new SemAtomType(SemAtomType.INT)));
//				pars.add(new AbsPar(null, "x", new AbsAtomType(null,
//						AbsAtomType.INT)));
//				AbsFunDef putInt = new AbsFunDef(null, "getInt", pars,
//						new AbsAtomType(null, AbsAtomType.INT), new AbsExpr(
//								null) {
//					@Override
//					public void accept(Visitor visitor) {
//					}
//				});
//				SymbTable.ins("getInt", putInt);
//				SymbDesc.setType(putInt, new SemFunType(parTypes,
//						new SemAtomType(SemAtomType.INT)));
//
//				FrmFrame frame = new FrmFrame(putInt, 1);
//				frame.numPars = 1;
//				frame.sizePars = 4;
//				frame.label = FrmLabel.newLabel("getInt");
//				FrmDesc.setFrame(putInt, frame);
//			}
//			{
//				Vector<AbsPar> pars = new Vector<>();
//				Vector<SemType> parTypes = new Vector<>();
//				parTypes.add(new SemAtomType(SemAtomType.STR));
//				pars.add(new AbsPar(null, "x", new AbsAtomType(null,
//						AbsAtomType.STR)));
//				AbsFunDef putInt = new AbsFunDef(null, "putString", pars,
//						new AbsAtomType(null, AbsAtomType.INT), new AbsExpr(
//								null) {
//					@Override
//					public void accept(Visitor visitor) {
//					}
//				});
//				SymbTable.ins("putString", putInt);
//				SymbDesc.setType(putInt, new SemFunType(parTypes,
//						new SemAtomType(SemAtomType.INT)));
//
//				FrmFrame frame = new FrmFrame(putInt, 1);
//				frame.numPars = 1;
//				frame.sizePars = 4;
//				frame.label = FrmLabel.newLabel("putString");
//				FrmDesc.setFrame(putInt, frame);
//			}
//			{
//				Vector<AbsPar> pars = new Vector<>();
//				Vector<SemType> parTypes = new Vector<>();
//				parTypes.add(new SemPtrType(new SemAtomType(SemAtomType.STR)));
//				pars.add(new AbsPar(null, "x", new AbsAtomType(null,
//						AbsAtomType.STR)));
//				AbsFunDef putInt = new AbsFunDef(null, "getString", pars,
//						new AbsAtomType(null, AbsAtomType.STR), new AbsExpr(
//								null) {
//					@Override
//					public void accept(Visitor visitor) {
//					}
//				});
//				SymbTable.ins("getString", putInt);
//				SymbDesc.setType(putInt, new SemFunType(parTypes,
//						new SemAtomType(SemAtomType.STR)));
//
//				FrmFrame frame = new FrmFrame(putInt, 1);
//				frame.numPars = 1;
//				frame.sizePars = 4;
//				frame.label = FrmLabel.newLabel("getString");
//				FrmDesc.setFrame(putInt, frame);
//			}
//		} catch (Exception e) {
//		}
	}

	@Override
	public void visit(AbsArrType acceptor) {
		acceptor.type.accept(this);
	}

	@Override
	public void visit(AbsPtrType acceptor) {
		acceptor.type.accept(this);
	}

	@Override
	public void visit(AbsStructType acceptor) {
		TraversalState tmp = currentState;
		SymbTable.newScope();
		acceptor.getDefinitions().accept(this);
		SymbTable.oldScope();
		currentState = tmp;
	}

	@Override
	public void visit(AbsAtomConst acceptor) {

	}

	@Override
	public void visit(AbsAtomType acceptor) {

	}

	@Override
	public void visit(AbsBinExpr acceptor) {
		acceptor.expr1.accept(this);

		if (acceptor.oper != AbsBinExpr.DOT)
			acceptor.expr2.accept(this);
	}

	@Override
	public void visit(AbsDefs acceptor) {
		for (TraversalState state : TraversalState.values()) {
			currentState = state;
			for (int def = 0; def < acceptor.numDefs(); def++)
				acceptor.def(def).accept(this);
		}
	}

	@Override
	public void visit(AbsExprs acceptor) {
		for (int expr = 0; expr < acceptor.numExprs(); expr++)
			acceptor.expr(expr).accept(this);
	}

	@Override
	public void visit(AbsFor acceptor) {
		if (currentState == TraversalState.ETS_prototypes) {
			SymbTable.newScope();
			
			try {
				SymbTable.ins(acceptor.count.name, new AbsVarDef(acceptor.count.position, acceptor.count.name, null));
			} catch (SemIllegalInsertException e) {
				Report.error("Error @ NameChecker::AbsFor");
			}
			acceptor.count.accept(this);
			acceptor.collection.accept(this);

			acceptor.body.accept(this);
			SymbTable.oldScope();
		}
	}

	@Override
	public void visit(AbsFunCall acceptor) {
		if (currentState != TraversalState.ETS_functions)
			return;

		AbsDef definition = SymbTable.fnd(acceptor.name);

		if (definition == null)
			Report.error(acceptor.position, "Error, function \""
					+ acceptor.name + "\" is undefined");

		SymbDesc.setNameDef(acceptor, definition);

		for (int arg = 0; arg < acceptor.numArgs(); arg++)
			acceptor.arg(arg).accept(this);
	}

	@Override
	public void visit(AbsFunDef acceptor) {
		if (acceptor.name.equals("main"))
			main = acceptor;

		if (currentState == TraversalState.ETS_prototypes) {
			try {
				SymbTable.ins(acceptor.name, acceptor);
			} catch (SemIllegalInsertException e) {
				Report.error(acceptor.position, "Duplicate method \""
						+ acceptor.name + "\"");
			}
		}

		else if (currentState == TraversalState.ETS_functions) {
			for (int par = 0; par < acceptor.numPars(); par++)
				acceptor.par(par).accept(this);
			acceptor.type.accept(this);
			
			SymbTable.newScope();
			acceptor.func.accept(this);
			SymbTable.oldScope();
		}
	}

	@Override
	public void visit(AbsIfThen acceptor) {
		if (currentState == TraversalState.ETS_prototypes) {
			acceptor.cond.accept(this);
			SymbTable.newScope();
			acceptor.thenBody.accept(this);
			SymbTable.oldScope();
		}
	}

	@Override
	public void visit(AbsIfThenElse acceptor) {
		if (currentState == TraversalState.ETS_prototypes) {
			acceptor.cond.accept(this);
			
			SymbTable.newScope();
			acceptor.thenBody.accept(this);
			SymbTable.oldScope();
			
			SymbTable.newScope();
			acceptor.elseBody.accept(this);
			SymbTable.oldScope();
		}
	}

	@Override
	public void visit(AbsPar acceptor) {
		try {
			SymbTable.ins(acceptor.name, acceptor);
		} catch (SemIllegalInsertException e) {
			Report.error(acceptor.position, "Duplicate parameter \""
					+ acceptor.name + "\"");
		}
		acceptor.type.accept(this);
	}

	@Override
	public void visit(AbsTypeDef acceptor) {
//		if (currentState == TraversalState.ETS_types) {
//			try {
//				SymbTable.ins(acceptor.name, acceptor);
//			} catch (SemIllegalInsertException e) {
//				Report.error(acceptor.position, "Type definition \""
//						+ acceptor.name + "\" already exists");
//			}
//		} else if (currentState == TraversalState.ETS_prototypes) {
//			acceptor.type.accept(this);
//		}
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
		if (currentState == TraversalState.ETS_variableDefinitions) {
			try {
				SymbTable.ins(acceptor.name, acceptor);
				acceptor.type.accept(this);
			} catch (SemIllegalInsertException e) {
				Report.error(acceptor.position, "Duplicate variable \""
						+ acceptor.name + "\"");
			}
		}
	}

	@Override
	public void visit(AbsConstDef acceptor) {
		if (currentState == TraversalState.ETS_variableDefinitions) {
			try {
				SymbTable.ins(acceptor.name, acceptor);
				acceptor.type.accept(this);
			} catch (SemIllegalInsertException e) {
				Report.error(acceptor.position, "Duplicate variable \""
						+ acceptor.name + "\"");
			}
		}		
	}

	@Override
	public void visit(AbsVarName acceptor) {
		if (currentState != TraversalState.ETS_functions)
			return;

		AbsDef definition = SymbTable.fnd(acceptor.name);
		if (definition == null)
			Report.error(acceptor.position, "Error, variable \""
					+ acceptor.name + "\" is undefined");

		SymbDesc.setNameDef(acceptor, definition);
	}

	@Override
	public void visit(AbsWhile acceptor) {
		if (currentState == TraversalState.ETS_prototypes) {
			acceptor.cond.accept(this);
			
			SymbTable.newScope();
			acceptor.body.accept(this);
			SymbTable.oldScope();
		}
	}

	@Override
	public void visit(AbsImportDef acceptor) {
		if (currentState == TraversalState.ETS_importDefinitions) {
			String tmp = Report.fileName;
			Report.fileName = acceptor.fileName;

			// parse the file
			// TODO hardcodano notr test/
			SynAn synAn = new SynAn(new LexAn("test/" + acceptor.fileName
					+ ".pins", false), false);
			AbsDefs source = (AbsDefs) synAn.parse();

			// TODO
			if (acceptor.definitions.size() > 0) {
				Vector<AbsDef> definitions = new Vector<>();
				for (int i = 0; i < source.numDefs(); i++) {
					String name = null;
					AbsDef d = source.def(i);

					if (d instanceof AbsVarDef)
						name = ((AbsVarDef) d).name;
					if (d instanceof AbsTypeDef)
						name = ((AbsTypeDef) d).name;
					if (d instanceof AbsFunDef)
						name = ((AbsFunDef) d).name;

					if (acceptor.definitions.contains(name))
						definitions.add(d);
				}

				acceptor.imports = new AbsDefs(source.position, definitions);
				acceptor.imports.accept(this);
			} else {
				acceptor.imports = (AbsDefs) source;
				acceptor.imports.accept(this);
			}

			currentState = TraversalState.ETS_importDefinitions;
			Report.fileName = tmp;
		}
	}

	@Override
	public void visit(AbsStmts stmts) {
		for (TraversalState state : TraversalState.values()) {
			currentState = state;
			for (int stmt = 0; stmt < stmts.numStmts(); stmt++) {
				stmts.stmt(stmt).accept(this);
			}
		}
	}

}
