package compiler.seman;

import java.util.*;

import compiler.*;
import compiler.abstr.*;
import compiler.abstr.tree.*;
import compiler.seman.type.*;

/**
 * Preverjanje tipov.
 * 
 * @author sliva
 * @implementation Toni Kocjan
 */
public class TypeChecker implements Visitor {

	@Override
	public void visit(AbsListType acceptor) {
		acceptor.type.accept(this);
		SymbDesc.setType(acceptor, new SemListType(SymbDesc.getType(acceptor.type), 
				acceptor.count));
	}

	@Override
	public void visit(AbsClassDef acceptor) {
		ArrayList<SemType> types = new ArrayList<>();
		ArrayList<String> names = new ArrayList<>();

		for (int i = 0; i < acceptor.statements.numStmts(); i++) {
			AbsStmt stmt = acceptor.statements.stmt(i);
			stmt.accept(this);
			
			if (stmt instanceof AbsDef) {
				types.add(SymbDesc.getType(stmt));
	
				if (stmt instanceof AbsVarDef)
					names.add(((AbsVarDef) stmt).name);
				else if (stmt instanceof AbsFunDef)
					names.add(((AbsFunDef) stmt).name);
				else
					Report.error("Semantic error @ AbsClassDef-typeChecker");
			}
		}
		SymbDesc.setType(acceptor, new SemClassType(acceptor.getName(), 
													names,
													types));
		for (AbsFunDef c : acceptor.contrustors) {
			c.accept(this);
			SymbDesc.setType(c, new SemFunType(new Vector<>(), 
					SymbDesc.getType(acceptor)));
		}
	}

	@Override
	public void visit(AbsAtomConst acceptor) {
		SymbDesc.setType(acceptor, new SemAtomType(acceptor.type));
	}

	@Override
	public void visit(AbsAtomType acceptor) {
		SymbDesc.setType(acceptor, new SemAtomType(acceptor.type));
	}

	@Override
	public void visit(AbsBinExpr acceptor) {
		acceptor.expr1.accept(this);
		
		if (acceptor.oper != AbsBinExpr.DOT)
			acceptor.expr2.accept(this);

		SemType t1 = SymbDesc.getType(acceptor.expr1);
		SemType t2 = SymbDesc.getType(acceptor.expr2);

		SemType integer = new SemAtomType(AtomType.INT);
		SemType logical = new SemAtomType(AtomType.LOG);
		SemType double_ = new SemAtomType(AtomType.DOB);

		int oper = acceptor.oper;

		/**
		 * expr1[expr2]
		 */
		if (oper == AbsBinExpr.ARR) {
			if (!t2.sameStructureAs(integer))
				Report.error(acceptor.expr2.position,
						"Expected Int type for array index");
			/**
			 * expr1 is of type ARR(n, t)
			 */
			if (t1 instanceof SemListType) {
				SymbDesc.setType(acceptor, ((SemListType) t1).type);
			} else
				Report.error(acceptor.expr1.position,
						"Type \'" + t1 + "\' has no subscript members");
			return;
		}

		/**
		 * expr1 = expr2
		 */
		if (oper == AbsBinExpr.ASSIGN) {
			boolean success = false;
			
			// if left variable doesn't have type, assign right type
			if (t1 == null) {
				t1 = t2;
				SymbDesc.setType(acceptor.expr1, t1);
			}
			
			if (t1.sameStructureAs(t2)) {
				SymbDesc.setType(SymbDesc.getNameDef(acceptor.expr1), t2);
				SymbDesc.setType(acceptor, t2);
				success = true;
			}
			else if (t1 instanceof SemListType && 
					t2 instanceof SemListType && 
					(((SemListType) t1).type.sameStructureAs(((SemListType) t2).type))) {
				SymbDesc.setType(acceptor.expr1, t2);
				SymbDesc.setType(acceptor, t2);
				SymbDesc.setType(SymbDesc.getNameDef(acceptor.expr1), t2);
				success = true;
			}
			else if (t2.canCastTo(t1)) {
				SymbDesc.setType(acceptor, t1);
				SymbDesc.setType(acceptor.expr2, t2);
			}
			else if (t2 instanceof SemAtomType && ((SemAtomType) t2).type == AtomType.NIL
					&& t1 instanceof SemPtrType) {
				SymbDesc.setType(acceptor.expr2, t1);
				SymbDesc.setType(acceptor, t1);
				success = true;
			}
			
			if (!success)
				Report.error(acceptor.position, "Cannot assign type " + t2
						+ " to type " + t1);
			
			return;
		}

		/**
		 * identifier.identifier
		 */
		if (oper == AbsBinExpr.DOT) {
			/**
			 * Handle list.length
			 */
			if (t1 instanceof SemListType) {
				String name = ((AbsVarName) acceptor.expr2).name;
				if (!name.equals("count"))
					Report.error("Lists have no attribute named \"" + name
							+ "\"");

				SymbDesc.setType(acceptor, integer);
				return;
			}

			if (!(t1 instanceof SemClassType))
				Report.error(acceptor.position,
						"Left expression must be a class type to use '.' operator");
			
			String name;
			if (acceptor.expr2 instanceof AbsVarName)
				name = ((AbsVarName) acceptor.expr2).name;
			else
				name = ((AbsFunCall) acceptor.expr2).name;

			if (!(SymbDesc.getNameDef(acceptor.expr1) instanceof AbsPar)) {
				// TODO: zrihtej to vse je v pizdi!!
				AbsVarDef varDef = (AbsVarDef) SymbDesc.getNameDef(acceptor.expr1);
				AbsClassDef classDef = (AbsClassDef) SymbTable.fnd(((AbsTypeName)varDef.type).name);
				AbsDef definition = classDef.statements.findDefinition(name);
				
				if (definition == null) {
					Report.error(acceptor.expr2.position,
							"Value of type '" + classDef.name + "' has no member '" + name + "'");
				}
				else {
					if (definition instanceof AbsVarDef && 
							((AbsVarDef) definition).visibility == Visibility.Private)
						Report.error(acceptor.expr2.position,
								"Member '" + name + "' is private");
				}
				
				SymbDesc.setNameDef(acceptor.expr2, definition);
				SymbDesc.setNameDef(acceptor, definition);
			}

			SemClassType sType = (SemClassType) t1;
			SemType type = sType.getMembers().get(name);

			SymbDesc.setType(acceptor.expr2, type);
			SymbDesc.setType(acceptor, type);
			return;
		}

		/**
		 * expr1 and expr2 are of type LOGICAL
		 */
		if (t1.sameStructureAs(logical) && t1.sameStructureAs(t2)) {
			// ==, !=, <=, >=, <, >, &, |
			if (oper >= 0 && oper <= 7)
				SymbDesc.setType(acceptor, logical);
			else
				Report.error(
						acceptor.position,
						"Numeric operations \"+\", \"-\", \"*\", \"/\" and \"%\" are undefined for type LOGICAL");
		}
		/**
		 * expr1 and expr2 are of type INTEGER
		 */
		else if (t1.sameStructureAs(integer) && t1.sameStructureAs(t2)) {
			// +, -, *, /, %
			if (oper >= 8 && oper <= 12)
				SymbDesc.setType(acceptor, integer);
			// ==, !=, <=, >=, <, >
			else if (oper >= 2 && oper <= 7)
				SymbDesc.setType(acceptor, logical);
			else
				Report.error(acceptor.position,
						"Logical operations \"&\" and \"|\" are undefined for type INTEGER");
		}
		/**
		 * expr1 and expr2 are of type INTEGER
		 */
		else if (t1.sameStructureAs(double_) && t1.sameStructureAs(t2)) {
			// +, -, *, /, %
			if (oper >= 8 && oper <= 12)
				SymbDesc.setType(acceptor, double_);
			// ==, !=, <=, >=, <, >
			else if (oper >= 2 && oper <= 7)
				SymbDesc.setType(acceptor, logical);
			else
				Report.error(acceptor.position,
						"Logical operations \"&\" and \"|\" are undefined for type INTEGER");
		}
		/**
		 * expr1 or expr2 is DOUBLE and the other is INTEGER (implicit cast to
		 * double)
		 */
		else if (t1.sameStructureAs(double_) && t2.sameStructureAs(integer)
				|| t1.sameStructureAs(integer) && t2.sameStructureAs(double_)) {
			// +, -, *, /, %
			if (oper >= 8 && oper <= 12)
				SymbDesc.setType(acceptor, double_);
			// ==, !=, <=, >=, <, >
			else if (oper >= 2 && oper <= 7)
				SymbDesc.setType(acceptor, logical);
			else
				Report.error(acceptor.position,
						"Logical operations \"&\" and \"|\" are undefined for type INTEGER");
		}

		else {
			Report.error(acceptor.position, "No viable operation for types "
					+ t1 + " and " + t2);
		}
	}

	@Override
	public void visit(AbsDefs acceptor) {
		for (int def = 0; def < acceptor.numDefs(); def++)
			acceptor.def(def).accept(this);
	}

	@Override
	public void visit(AbsExprs acceptor) {
		if (acceptor.numExprs() == 0)
			SymbDesc.setType(acceptor, new SemAtomType(AtomType.VOID));
		else {
			for (int expr = 0; expr < acceptor.numExprs(); expr++)
				acceptor.expr(expr).accept(this);

			SymbDesc.setType(acceptor,
					SymbDesc.getType(acceptor.expr(acceptor.numExprs() - 1)));
		}
	}

	@Override
	public void visit(AbsFor acceptor) {
		acceptor.collection.accept(this);
		SemType type = ((SemListType)SymbDesc.getType(acceptor.collection)).type;

		SymbDesc.setType(SymbDesc.getNameDef(acceptor.iterator), type);
		SymbDesc.setType(acceptor, new SemAtomType(AtomType.VOID));
		
		acceptor.iterator.accept(this);
		acceptor.body.accept(this);
	}

	@Override
	public void visit(AbsFunCall acceptor) {
		Vector<SemType> parameters = new Vector<>();
		for (int arg = 0; arg < acceptor.numArgs(); arg++) {
			acceptor.arg(arg).accept(this);
			SemType parType = SymbDesc.getType(acceptor.arg(arg));
			parameters.add(parType);
		}

		AbsDef def = SymbDesc.getNameDef(acceptor);
		
		if (def instanceof AbsVarDef || def instanceof AbsPar) {
			SemType type = SymbDesc.getType(def);
			if (!(type instanceof SemFunType))
				Report.error(acceptor.position, "Cannot call value of non-function type \'"
								+ type.toString() + "\'");
			SemFunType t = new SemFunType(parameters, ((SemFunType)type).resultType);
			if (!type.sameStructureAs(t)) 
				Report.error("Error todo");
			SymbDesc.setNameDef(acceptor, def);
			SymbDesc.setType(acceptor, ((SemFunType) SymbDesc.getType(def)).resultType);
		}
		else {
			AbsDef definition = SymbTable.fndFunc(acceptor.name, parameters);
			if (definition == null) {
				Report.error(acceptor.position, "Method " + acceptor.name
						+ new SemFunType(parameters, null).toString()
						+ " is undefined");
			}
			SymbDesc.setNameDef(acceptor, definition);
			SymbDesc.setType(acceptor,
					((SemFunType) SymbDesc.getType(definition)).resultType);
		}
	}

	@Override
	public void visit(AbsFunDef acceptor) {
		Vector<SemType> parameters = new Vector<>();
		for (int par = 0; par < acceptor.numPars(); par++) {
			acceptor.par(par).accept(this);
			parameters.add(SymbDesc.getType(acceptor.par(par)));
		}

		acceptor.type.accept(this);

		SemFunType funType = new SemFunType(parameters,
				SymbDesc.getType(acceptor.type));
		SymbDesc.setType(acceptor, funType);

		// insert function into symbol table
		try {
			SymbTable.insFunc(acceptor.name, parameters, acceptor);
		} catch (SemIllegalInsertException e) {
			Report.error(acceptor.position, "Duplicate method \""
					+ acceptor.name + "\"");
		}
		
		acceptor.func.accept(this);

		// check if return type matches
		for (int i = 0; i < acceptor.func.numStmts(); i++) {
			AbsStmt stmt = acceptor.func.stmt(i);
			if (stmt instanceof AbsReturnExpr) {
				SemType t = SymbDesc.getType(stmt);
				if (!t.sameStructureAs(funType.resultType))
					Report.error(stmt.position,
							"Return type doesn't match, expected \""
									+ funType.resultType.actualType()
											.toString() + "\", got \""
									+ t.actualType().toString() + "\" instead");
			}
		}
	}

	@Override
	public void visit(AbsIfThen acceptor) {
		acceptor.cond.accept(this);
		acceptor.thenBody.accept(this);

		if (SymbDesc.getType(acceptor.cond).sameStructureAs(
				new SemAtomType(AtomType.LOG)))
			SymbDesc.setType(acceptor, new SemAtomType(AtomType.VOID));
		else
			Report.error(acceptor.cond.position,
					"Condition must be of type LOGICAL");
	}

	@Override
	public void visit(AbsIfThenElse acceptor) {
		acceptor.cond.accept(this);
		acceptor.thenBody.accept(this);
		acceptor.elseBody.accept(this);

		if (SymbDesc.getType(acceptor.cond).sameStructureAs(
				new SemAtomType(AtomType.LOG)))
			SymbDesc.setType(acceptor, new SemAtomType(AtomType.VOID));
		else
			Report.error(acceptor.cond.position,
					"Condition must be of type LOGICAL");
	}

	@Override
	public void visit(AbsPar acceptor) {
		acceptor.type.accept(this);
		SemType type = SymbDesc.getType(acceptor.type);

		SymbDesc.setType(acceptor, type);
	}

	@Override
	public void visit(AbsTypeName acceptor) {
		AbsDef definition = SymbDesc.getNameDef(acceptor);
		if (!(definition instanceof AbsTypeDef))
			Report.error(acceptor.position, "Expected type definition");

		SemType type = SymbDesc.getType(definition);

		if (type == null)
			Report.error(acceptor.position, "Type \"" + acceptor.name
					+ "\" is undefined");

		SymbDesc.setType(acceptor, type);
	}

	@Override
	public void visit(AbsUnExpr acceptor) {
		acceptor.expr.accept(this);
		SemType type = SymbDesc.getType(acceptor.expr);

		if (acceptor.oper == AbsUnExpr.NOT) {
			if (type.sameStructureAs(new SemAtomType(AtomType.LOG)))
				SymbDesc.setType(acceptor, new SemAtomType(AtomType.LOG));
			else
				Report.error(acceptor.position,
						"Operator \"!\" is not defined for type " + type);
		} else if (acceptor.oper == AbsUnExpr.ADD
				|| acceptor.oper == AbsUnExpr.SUB) {
			if (type.sameStructureAs(new SemAtomType(AtomType.INT)))
				SymbDesc.setType(acceptor, new SemAtomType(AtomType.INT));
			else
				Report.error(acceptor.position,
						"Operators \"+\" and \"-\" are not defined for type "
								+ type);
		}
	}

	@Override
	public void visit(AbsVarDef acceptor) {
		if (acceptor.type != null) {
			acceptor.type.accept(this);
			SemType type = SymbDesc.getType(acceptor.type);
			SymbDesc.setType(acceptor, type);
		}
	}

	@Override
	public void visit(AbsVarName acceptor) {
		SymbDesc.setType(acceptor,
				SymbDesc.getType(SymbDesc.getNameDef(acceptor)));
	}

	@Override
	public void visit(AbsWhile acceptor) {
		acceptor.cond.accept(this);
		acceptor.body.accept(this);

		if (SymbDesc.getType(acceptor.cond).sameStructureAs(
				new SemAtomType(AtomType.LOG)))
			SymbDesc.setType(acceptor, new SemAtomType(AtomType.VOID));
		else
			Report.error(acceptor.cond.position,
					"Condition must be typed as Boolean");
	}

	@Override
	public void visit(AbsImportDef importDef) {
		String tmp = Report.fileName;
		Report.fileName = importDef.fileName;

		for (int def = 0; def < importDef.imports.numDefs(); def++)
			importDef.imports.def(def).accept(this);

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
		if (returnExpr.expr != null) {
			returnExpr.expr.accept(this);
			SymbDesc.setType(returnExpr, SymbDesc.getType(returnExpr.expr));
		} else
			SymbDesc.setType(returnExpr, new SemAtomType(AtomType.VOID));
	}

	@Override
	public void visit(AbsListExpr absListExpr) {
		Vector<SemType> vec = new Vector<>();
		for (AbsExpr e : absListExpr.expressions) {
			e.accept(this);
			SemType t = SymbDesc.getType(e);

			if (!vec.isEmpty() && !vec.firstElement().sameStructureAs(t))
				Report.error(e.position, "Error, invalid expression type");

			vec.add(SymbDesc.getType(e));
		}

		SymbDesc.setType(absListExpr, new SemListType(vec.firstElement(), vec.size()));
	}

	@Override
	public void visit(AbsFunType funType) {
		Vector<SemType> parameters = new Vector<>();
		for (AbsType t : funType.parameterTypes) {
			t.accept(this);
			parameters.add(SymbDesc.getType(t));
		}
		funType.returnType.accept(this);
		
		SymbDesc.setType(funType, new SemFunType(parameters, 
				SymbDesc.getType(funType.returnType)));
	}
}
