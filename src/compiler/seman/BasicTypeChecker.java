package compiler.seman;

import java.util.*;

import compiler.*;
import compiler.abstr.*;
import compiler.abstr.tree.*;
import compiler.abstr.tree.def.AbsClassDef;
import compiler.abstr.tree.def.AbsDef;
import compiler.abstr.tree.def.AbsEnumDef;
import compiler.abstr.tree.def.AbsEnumMemberDef;
import compiler.abstr.tree.def.AbsFunDef;
import compiler.abstr.tree.def.AbsImportDef;
import compiler.abstr.tree.def.AbsParDef;
import compiler.abstr.tree.def.AbsTypeDef;
import compiler.abstr.tree.def.AbsVarDef;
import compiler.abstr.tree.expr.AbsAtomConstExpr;
import compiler.abstr.tree.expr.AbsBinExpr;
import compiler.abstr.tree.expr.AbsExpr;
import compiler.abstr.tree.expr.AbsFunCall;
import compiler.abstr.tree.expr.AbsListExpr;
import compiler.abstr.tree.expr.AbsReturnExpr;
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
import compiler.abstr.tree.type.AbsType;
import compiler.abstr.tree.type.AbsTypeName;
import compiler.seman.type.*;

/**
 * Preverjanje tipov.
 * 
 * @author sliva
 * @implementation Toni Kocjan
 */
public class BasicTypeChecker implements ASTVisitor {

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
				SemType memberType = SymbDesc.getType(stmt);
				
				if (memberType == null && 
						i + 1 < acceptor.statements.numStmts() &&
						!(acceptor.statements.stmt(i + 1) instanceof AbsDef)) {
					acceptor.statements.stmt(i + 1).accept(this);
					memberType = SymbDesc.getType(acceptor.statements.stmt(i + 1));
				}
				
				types.add(memberType);
				
				if (stmt instanceof AbsVarDef)
					names.add(((AbsVarDef) stmt).name);
				else if (stmt instanceof AbsFunDef)
					names.add(((AbsFunDef) stmt).name);
				else
					Report.error("Semantic error @ AbsClassDef-typeChecker");
			}
		}
		SymbDesc.setType(acceptor, new SemClassType(acceptor, 
													names,
													types));
		for (AbsFunDef c : acceptor.contrustors) {
			c.accept(this);
			SymbDesc.setType(c, new SemFunType(new Vector<>(), 
					SymbDesc.getType(acceptor)));
		}
	}

	@Override
	public void visit(AbsAtomConstExpr acceptor) {
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

		SemType integer = new SemAtomType(AtomTypeEnum.INT);
		SemType logical = new SemAtomType(AtomTypeEnum.LOG);
		SemType double_ = new SemAtomType(AtomTypeEnum.DOB);

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
				success = true;
			}
			else if (t2 instanceof SemAtomType && ((SemAtomType) t2).type == AtomTypeEnum.NIL
					&& t1 instanceof SemPtrType) {
				SymbDesc.setType(acceptor.expr2, t1);
				SymbDesc.setType(acceptor, t1);
				success = true;
			}
			
			if (!success)
				Report.error(acceptor.position, "Cannot convert valueof type " + t2
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
				String name = ((AbsVarNameExpr) acceptor.expr2).name;
				if (!name.equals("count"))
					Report.error("Lists have no attribute named \"" + name
							+ "\"");

				SymbDesc.setType(acceptor, integer);
				return;
			}

			if (!(t1 instanceof SemClassType) && !(t1 instanceof SemEnumType))
				Report.error(acceptor.position,
						"Cannot use dot ('.') operator on type \"" + t1.toString() + "\"");
			if (t1 instanceof SemClassType) {
				String name;
				if (acceptor.expr2 instanceof AbsVarNameExpr)
					name = ((AbsVarNameExpr) acceptor.expr2).name;
				else
					name = ((AbsFunCall) acceptor.expr2).name;
	
				if (!(SymbDesc.getNameDef(acceptor.expr1) instanceof AbsParDef)) {
					SemClassType classType = (SemClassType) t1;
					AbsDef definition = classType.findMemberForName(name);
					
					if (definition == null) {
						Report.error(acceptor.expr2.position,
								"Value of type '" + classType.getName() + "' has no member '" + name + "'");
					}
					if (definition instanceof AbsVarDef && 
							((AbsVarDef) definition).visibilityEnum == VisibilityEnum.Private)
						Report.error(acceptor.expr2.position,
								"Member '" + name + "' is private");
					
					SymbDesc.setNameDef(acceptor.expr2, definition);
					SymbDesc.setNameDef(acceptor, definition);
				}
	
				SemClassType sType = (SemClassType) t1;
				SemType type = sType.getMembers().get(name);
	
				SymbDesc.setType(acceptor.expr2, type);
				SymbDesc.setType(acceptor, type);
				return;
			}
			
			if (t1 instanceof SemEnumType) {
				if (!(acceptor.expr2 instanceof AbsVarNameExpr)) {
					if (acceptor.expr2 instanceof AbsFunCall)
						Report.error(acceptor.expr2.position, "Invalid use of '()' to call a value of non-function type");
					Report.error(acceptor.expr2.position, "todo");
				}

				AbsDef definition = ((SemEnumType) t1).findMemberDefinitionForName(((AbsVarNameExpr)acceptor.expr2).name);
				t2 = SymbDesc.getType(definition);
				String enumName = ((SemEnumType) t1).definition.name;
				
				if (!t1.sameStructureAs(t2))
					Report.error(acceptor.expr2.position, "Type \"" + enumName + "\" has no member \"" + t2.toString() + "\"");
				
				SymbDesc.setType(acceptor.expr2, t1);
				SymbDesc.setType(acceptor, t1);
				SymbDesc.setNameDef(acceptor.expr2, definition);
				return;
			}
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
			SymbDesc.setType(acceptor, new SemAtomType(AtomTypeEnum.VOID));
		else {
			for (int expr = 0; expr < acceptor.numExprs(); expr++)
				acceptor.expr(expr).accept(this);

			SymbDesc.setType(acceptor,
					SymbDesc.getType(acceptor.expr(acceptor.numExprs() - 1)));
		}
	}

	@Override
	public void visit(AbsForStmt acceptor) {
		acceptor.collection.accept(this);
		SemType type = ((SemListType)SymbDesc.getType(acceptor.collection)).type;

		SymbDesc.setType(SymbDesc.getNameDef(acceptor.iterator), type);
		SymbDesc.setType(acceptor, new SemAtomType(AtomTypeEnum.VOID));
		
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
		
		if (def instanceof AbsVarDef || def instanceof AbsParDef) {
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
	public void visit(AbsIfStmt acceptor) {
		for (Condition c : acceptor.conditions) {
			c.cond.accept(this);
			c.body.accept(this);
			
			if (SymbDesc.getType(c.cond).sameStructureAs(
					new SemAtomType(AtomTypeEnum.LOG)))
				SymbDesc.setType(acceptor, new SemAtomType(AtomTypeEnum.VOID));
			else
				Report.error(c.cond.position,
						"Condition must be of type Bool");
		}

		if (acceptor.elseBody != null) {
			acceptor.elseBody.accept(this);
		}
	}

	@Override
	public void visit(AbsParDef acceptor) {
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
			if (type.sameStructureAs(new SemAtomType(AtomTypeEnum.LOG)))
				SymbDesc.setType(acceptor, new SemAtomType(AtomTypeEnum.LOG));
			else
				Report.error(acceptor.position,
						"Operator \"!\" is not defined for type " + type);
		} else if (acceptor.oper == AbsUnExpr.ADD
				|| acceptor.oper == AbsUnExpr.SUB) {
			if (type.sameStructureAs(new SemAtomType(AtomTypeEnum.INT)))
				SymbDesc.setType(acceptor, new SemAtomType(AtomTypeEnum.INT));
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
	public void visit(AbsVarNameExpr acceptor) {
		SymbDesc.setType(acceptor,
				SymbDesc.getType(SymbDesc.getNameDef(acceptor)));
	}

	@Override
	public void visit(AbsWhileStmt acceptor) {
		acceptor.cond.accept(this);
		acceptor.body.accept(this);

		if (SymbDesc.getType(acceptor.cond).sameStructureAs(
				new SemAtomType(AtomTypeEnum.LOG)))
			SymbDesc.setType(acceptor, new SemAtomType(AtomTypeEnum.VOID));
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
			SymbDesc.setType(returnExpr, new SemAtomType(AtomTypeEnum.VOID));
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

	@Override
	public void visit(AbsControlTransferStmt acceptor) {
		///
	}

	@Override
	public void visit(AbsSwitchStmt switchStmt) {
		switchStmt.subjectExpr.accept(this);
		
		SemType switchType = SymbDesc.getType(switchStmt.subjectExpr);
		
		for (AbsCaseStmt singleCase : switchStmt.cases) {
			singleCase.accept(this);
			for (AbsExpr e : singleCase.exprs) {
				SemType caseType = SymbDesc.getType(e);
				if (!caseType.sameStructureAs(switchType))
					Report.error(e.position, 
							"Expression of type \"" + caseType.toString() + 
							"\" cannot match values of type \"" + switchType.toString() +"\"");
			}
		}
		
		if (switchStmt.defaultBody != null)
			switchStmt.defaultBody.accept(this);
		
		SymbDesc.setType(switchStmt, new SemAtomType(AtomTypeEnum.VOID));
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
		SemEnumType enumType = new SemEnumType(acceptor);
		SemAtomType enumRawValueType = null;
		
		if (acceptor.type != null) {
			acceptor.type.accept(this);
			enumRawValueType = (SemAtomType) SymbDesc.getType(acceptor.type);
		}
		
		String previousValue = null;
		int iterator = 0;
		
		for (AbsEnumMemberDef def : acceptor.definitions) {
			def.accept(this);
			SymbDesc.setType(def, enumType);
			
			if (def.value != null) {
				if (enumRawValueType == null)
					Report.error(def.value.position, "Enum member cannot have a raw value "
							+ "if the enum doesn't have a raw type");
				
				SemType rawValueType = SymbDesc.getType(def.value);
				if (!rawValueType.sameStructureAs(enumRawValueType))
					Report.error(def.value.position, "Cannot convert value of type \"" + 
							rawValueType.toString() + "\" to type \"" + enumRawValueType.toString() + "\"");
				SymbDesc.setType(def, enumType);
				
				previousValue = def.value.value;
			}
			else if (enumRawValueType != null) {
				if (enumRawValueType.type != AtomTypeEnum.STR &&
						enumRawValueType.type != AtomTypeEnum.INT)
					Report.error(def.position, "Enum members require explicit raw values when the raw type is not integer or string literal");
				
				String value = null;
				
				if (enumRawValueType.type == AtomTypeEnum.STR)
					value = def.name.name;
				else if (enumRawValueType.type == AtomTypeEnum.INT) {
					if (previousValue == null)
						value = "" + iterator;
					else
						value = "" + (Integer.parseInt(previousValue) + 1);
				}
				
				def.value = new AbsAtomConstExpr(def.position, enumRawValueType.type, value);

				previousValue = value;
				iterator++;
			}
		}
		
		SymbDesc.setType(acceptor, enumType);
	}

	@Override
	public void visit(AbsEnumMemberDef acceptor) {
		acceptor.name.accept(this);
		
		if (acceptor.value != null)
			acceptor.value.accept(this);
	}
}
