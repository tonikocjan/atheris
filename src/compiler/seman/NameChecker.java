package compiler.seman;

import java.util.Vector;

import compiler.Report;
import compiler.abstr.*;
import compiler.abstr.tree.*;
import compiler.frames.*;
import compiler.lexan.LexAn;
import compiler.synan.SynAn;
import compiler.seman.type.*;

/**
 * Preverjanje in razresevanje imen (razen imen komponent).
 * 
 * @implementation Toni Kocjan
 */
public class NameChecker implements Visitor {
	
	public NameChecker() {
		try {
			{
				String name = "print";
				Vector<AbsPar> pars = new Vector<>();
				Vector<SemType> parTypes = new Vector<>();
				
				parTypes.add(new SemAtomType(AtomType.INT));
				pars.add(new AbsPar(null, "x", new AbsAtomType(null,
						AtomType.INT)));

				AbsFunDef print = new AbsFunDef(null, name, pars,
						new AbsAtomType(null, AtomType.VOID), new AbsStmts(
								null, new Vector<>()));
				SymbTable.insFunc(name, parTypes, print);
				SymbDesc.setType(print, new SemFunType(parTypes,
						new SemAtomType(AtomType.VOID)));

				FrmFrame frame = new FrmFrame(print, 1);
				frame.numPars = 1;
				frame.sizePars = 4;
				frame.label = FrmLabel.newLabel(name);
				FrmDesc.setFrame(print, frame);
			}
			{
				String name = "print";
				Vector<AbsPar> pars = new Vector<>();
				Vector<SemType> parTypes = new Vector<>();
				parTypes.add(new SemAtomType(AtomType.DOB));

				pars.add(new AbsPar(null, "x", new AbsAtomType(null,
						AtomType.DOB)));

				AbsFunDef print = new AbsFunDef(null, name, pars,
						new AbsAtomType(null, AtomType.VOID), new AbsStmts(
								null, new Vector<>()));
				SymbTable.insFunc(name, parTypes, print);
				SymbDesc.setType(print, new SemFunType(parTypes,
						new SemAtomType(AtomType.VOID)));

				FrmFrame frame = new FrmFrame(print, 1);
				frame.numPars = 1;
				frame.sizePars = 4;
				frame.label = FrmLabel.newLabel(name);
				FrmDesc.setFrame(print, frame);
			}
			{
				String name = "print";
				Vector<AbsPar> pars = new Vector<>();
				Vector<SemType> parTypes = new Vector<>();
				parTypes.add(new SemAtomType(AtomType.STR));

				pars.add(new AbsPar(null, "x", new AbsAtomType(null,
						AtomType.INT)));

				AbsFunDef print = new AbsFunDef(null, name, pars,
						new AbsAtomType(null, AtomType.VOID), new AbsStmts(
								null, new Vector<>()));
				SymbTable.insFunc(name, parTypes, print);
				SymbDesc.setType(print, new SemFunType(parTypes,
						new SemAtomType(AtomType.VOID)));

				FrmFrame frame = new FrmFrame(print, 1);
				frame.numPars = 1;
				frame.sizePars = 4;
				frame.label = FrmLabel.newLabel(name);
				FrmDesc.setFrame(print, frame);
			}
			{
				String name = "print";
				Vector<AbsPar> pars = new Vector<>();
				Vector<SemType> parTypes = new Vector<>();
				parTypes.add(new SemAtomType(AtomType.CHR));

				pars.add(new AbsPar(null, "x", new AbsAtomType(null,
						AtomType.CHR)));

				AbsFunDef print = new AbsFunDef(null, name, pars,
						new AbsAtomType(null, AtomType.VOID), new AbsStmts(
								null, new Vector<>()));
				SymbTable.insFunc(name, parTypes, print);
				SymbDesc.setType(print, new SemFunType(parTypes,
						new SemAtomType(AtomType.VOID)));

				FrmFrame frame = new FrmFrame(print, 1);
				frame.numPars = 1;
				frame.sizePars = 4;
				frame.label = FrmLabel.newLabel(name);
				FrmDesc.setFrame(print, frame);
			}
			{
				String name = "print";
				Vector<AbsPar> pars = new Vector<>();
				Vector<SemType> parTypes = new Vector<>();
				parTypes.add(new SemAtomType(AtomType.LOG));

				pars.add(new AbsPar(null, "x", new AbsAtomType(null,
						AtomType.LOG)));

				AbsFunDef print = new AbsFunDef(null, name, pars,
						new AbsAtomType(null, AtomType.VOID), new AbsStmts(
								null, new Vector<>()));
				SymbTable.insFunc(name, parTypes, print);
				SymbDesc.setType(print, new SemFunType(parTypes,
						new SemAtomType(AtomType.VOID)));

				FrmFrame frame = new FrmFrame(print, 1);
				frame.numPars = 1;
				frame.sizePars = 4;
				frame.label = FrmLabel.newLabel(name);
				FrmDesc.setFrame(print, frame);
			}
			// {
			// Vector<AbsPar> pars = new Vector<>();
			// Vector<SemType> parTypes = new Vector<>();
			// parTypes.add(new SemPtrType(new SemAtomType(AtomType.INT)));
			// pars.add(new AbsPar(null, "x", new AbsAtomType(null,
			// AtomType.INT)));
			// AbsFunDef print = new AbsFunDef(null, "getInt", pars,
			// new AbsAtomType(null, AtomType.INT), new AbsExpr(
			// null) {
			// @Override
			// public void accept(Visitor visitor) {
			// }
			// });
			// SymbTable.ins("getInt", print);
			// SymbDesc.setType(print, new SemFunType(parTypes,
			// new SemAtomType(AtomType.INT)));
			//
			// FrmFrame frame = new FrmFrame(print, 1);
			// frame.numPars = 1;
			// frame.sizePars = 4;
			// frame.label = FrmLabel.newLabel("getInt");
			// FrmDesc.setFrame(print, frame);
			// }
			// {
			// Vector<AbsPar> pars = new Vector<>();
			// Vector<SemType> parTypes = new Vector<>();
			// parTypes.add(new SemAtomType(AtomType.STR));
			// pars.add(new AbsPar(null, "x", new AbsAtomType(null,
			// AtomType.STR)));
			// AbsFunDef print = new AbsFunDef(null, "putString", pars,
			// new AbsAtomType(null, AtomType.INT), new AbsExpr(
			// null) {
			// @Override
			// public void accept(Visitor visitor) {
			// }
			// });
			// SymbTable.ins("putString", print);
			// SymbDesc.setType(print, new SemFunType(parTypes,
			// new SemAtomType(AtomType.INT)));
			//
			// FrmFrame frame = new FrmFrame(print, 1);
			// frame.numPars = 1;
			// frame.sizePars = 4;
			// frame.label = FrmLabel.newLabel("putString");
			// FrmDesc.setFrame(print, frame);
			// }
			// {
			// Vector<AbsPar> pars = new Vector<>();
			// Vector<SemType> parTypes = new Vector<>();
			// parTypes.add(new SemPtrType(new SemAtomType(AtomType.STR)));
			// pars.add(new AbsPar(null, "x", new AbsAtomType(null,
			// AtomType.STR)));
			// AbsFunDef print = new AbsFunDef(null, "getString", pars,
			// new AbsAtomType(null, AtomType.STR), new AbsExpr(
			// null) {
			// @Override
			// public void accept(Visitor visitor) {
			// }
			// });
			// SymbTable.ins("getString", print);
			// SymbDesc.setType(print, new SemFunType(parTypes,
			// new SemAtomType(AtomType.STR)));
			//
			// FrmFrame frame = new FrmFrame(print, 1);
			// frame.numPars = 1;
			// frame.sizePars = 4;
			// frame.label = FrmLabel.newLabel("getString");
			// FrmDesc.setFrame(print, frame);
			// }
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
			Report.error(acceptor.position, "Structure \"" + acceptor.getName()
					+ "\" already exists");
		}
		SymbTable.newScope();
		acceptor.getDefinitions().accept(this);
		SymbTable.oldScope();
		
		for (AbsFunDef c : acceptor.contrustors) {
			c.accept(this);
		}
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

		if (acceptor.oper == AbsBinExpr.DOT) {
			if (!(acceptor.expr2 instanceof AbsVarName ||
					acceptor.expr2 instanceof AbsFunCall))
				  Report.error(acceptor.position, "Invalid expression for DOT operator");
		}
		else
			acceptor.expr2.accept(this);
	}

	@Override
	public void visit(AbsDefs acceptor) {
		for (int def = 0; def < acceptor.numDefs(); def++)
			acceptor.def(def).accept(this);
	}

	@Override
	public void visit(AbsExprs acceptor) {
		for (int expr = 0; expr < acceptor.numExprs(); expr++)
			acceptor.expr(expr).accept(this);
	}

	@Override
	public void visit(AbsFor acceptor) {
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
		for (int arg = 0; arg < acceptor.numArgs(); arg++)
			acceptor.arg(arg).accept(this);
	}

	@Override
	public void visit(AbsFunDef acceptor) {
		SymbTable.newScope();

		for (int par = 0; par < acceptor.numPars(); par++)
			acceptor.par(par).accept(this);
		acceptor.type.accept(this);
		acceptor.func.accept(this);

		SymbTable.oldScope();
	}

	@Override
	public void visit(AbsIfThen acceptor) {
		acceptor.cond.accept(this);
		SymbTable.newScope();
		acceptor.thenBody.accept(this);
		SymbTable.oldScope();
	}

	@Override
	public void visit(AbsIfThenElse acceptor) {
		acceptor.cond.accept(this);

		SymbTable.newScope();
		acceptor.thenBody.accept(this);
		SymbTable.oldScope();

		SymbTable.newScope();
		acceptor.elseBody.accept(this);
		SymbTable.oldScope();
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
			acceptor.type.accept(this);
		} catch (SemIllegalInsertException e) {
			Report.error(acceptor.position, "Duplicate variable \""
					+ acceptor.name + "\"");
		}
	}

	@Override
	public void visit(AbsVarName acceptor) {
		AbsDef definition = SymbTable.fnd(acceptor.name);
		if (definition == null)
			Report.error(acceptor.position, "Error, variable \""
					+ acceptor.name + "\" is undefined");

		SymbDesc.setNameDef(acceptor, definition);
	}

	@Override
	public void visit(AbsWhile acceptor) {
		acceptor.cond.accept(this);

		SymbTable.newScope();
		acceptor.body.accept(this);
		SymbTable.oldScope();
	}

	@Override
	public void visit(AbsImportDef acceptor) {
		String tmp = Report.fileName;
		Report.fileName = acceptor.fileName;

		// parse the file
		// TODO hardcodano notr test/
		SynAn synAn = new SynAn(new LexAn("test/" + acceptor.fileName + ".ar",
				false), false);
		AbsStmts source = (AbsStmts) synAn.parse();

		Vector<AbsDef> definitions = new Vector<>();
		for (int i = 0; i < source.numStmts(); i++) {
			AbsStmt s = source.stmt(i);

			// skip statements which are not definitions
			if (!(s instanceof AbsDef))
				continue;

			AbsDef d = (AbsDef) s;

			if (acceptor.definitions.size() > 0) {
				String name = null;

				if (d instanceof AbsVarDef)
					name = ((AbsVarDef) d).name;
				if (d instanceof AbsFunDef)
					name = ((AbsFunDef) d).name;
				if (d instanceof AbsClassDef)
					name = ((AbsClassDef) d).getName();

				if (!acceptor.definitions.contains(name))
					continue;
			}
			definitions.add(d);
		}

		acceptor.imports = new AbsDefs(source.position, definitions);
		acceptor.imports.accept(this);

		Report.fileName = tmp;
	}

	@Override
	public void visit(AbsStmts stmts) {
		for (int stmt = 0; stmt < stmts.numStmts(); stmt++) {
			stmts.stmt(stmt).accept(this);
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

}
