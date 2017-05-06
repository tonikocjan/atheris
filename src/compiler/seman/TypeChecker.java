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

import java.util.*;

import Utils.Constants;
import compiler.Report;
import compiler.abstr.ASTVisitor;
import compiler.abstr.tree.*;
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
import compiler.seman.type.*;
import managers.LanguageManager;

/**
 * Preverjanje tipov.
 * 
 * @author toni
 */
public class TypeChecker implements ASTVisitor {

    /**
     * Traversal states.
     */
	private enum TraversalState {
		extensions, normal
	}

    /**
     * True if AST node is root node.
     */
	private boolean isRootNode = true;

	/**
	 * Current state of traversal
	 */
	private TraversalState traversalState = TraversalState.extensions;

	/**
     * True if assigning to variable, otherwise false.
	 */
	private boolean assign = false;

    /**
     *
     */
    private Stack<Type> lhsTypes = new Stack<>();

    /**
     *
     */
    private Stack<CanType> declarationContext = new Stack<>();

    /**
     *
     */
    private boolean resolveTypeOnly = false;


	// MARK: - Methods
	@Override
	public void visit(AbsListType acceptor) {
		acceptor.type.accept(this);

		Type type = SymbDesc.getType(acceptor.type);
		if (type.isCanType()) {
		    type = ((CanType) type).childType;
        }

		SymbDesc.setType(acceptor, new ArrayType(type, acceptor.count));
	}

	@Override
	public void visit(AbsClassDef acceptor) {
	    if (traversalState == TraversalState.extensions) {
            LinkedList<Type> types = new LinkedList<>();
            LinkedList<String> names = new LinkedList<>();

            LinkedList<Type> staticTypes = new LinkedList<>();
            LinkedList<String> staticNames = new LinkedList<>();
            LinkedList<AbsDef> staticDefinitions = new LinkedList<>();

            CanType baseClass = null;

            // check whether inheritance is legal
            if (acceptor.baseClass != null) {
                acceptor.baseClass.accept(this);

                Type type = SymbDesc.getType(acceptor.baseClass);

                if (acceptor instanceof AbsStructDef) {
                    // only classes are allowed to inherit
                    Report.error("Structs are not allowed to inherit");
                }

                if (!type.isInterfaceType() && (!type.isCanType() || !((CanType) type).childType.isClassType())) {
                    Report.error(acceptor.baseClass.position,
                            "Inheritance from non-class type \"" + type.friendlyName() + "\" is not allowed");
                }

                if (type.isInterfaceType()) {
                    acceptor.conformances.addFirst(acceptor.baseClass);
                }
                else {
                    baseClass = (CanType) type;
                }
            }

            // check whether conformance is legal
            for (AbsType conformance : acceptor.conformances) {
                conformance.accept(this);

                if (!SymbDesc.getType(conformance).isInterfaceType()) {
                    if (baseClass != null) {
                        Report.error(conformance.position, "Multiple inheritance is not allowed");
                    }
                    else {
                        Report.error(conformance.position, "Super class must appear first in inheritance clause");
                    }
                }
            }

            // type inference
            for (AbsStmt stmt: acceptor.defaultConstructor.func.statements) {
                AbsBinExpr initExpr = (AbsBinExpr) stmt;
                initExpr.expr2.accept(this);
                Type type = SymbDesc.getType(initExpr.expr2);

                AbsVarDef definition = (AbsVarDef) acceptor.definitions.findDefinitionForName(((AbsVarNameExpr) ((AbsBinExpr)initExpr.expr1).expr2).name);
                SymbDesc.setType(definition, type);
            }

            resolveTypeOnly = true;

            for (AbsDef def : acceptor.definitions.definitions) {
                if (names.contains(def.getName())) {
                    Report.error(def.position, "Invalid redeclaration of \"" + def.getName() + "\"");
                }
                else if (baseClass != null) {
                    // check whether inheritance (overriding) is legal
                    if (def.isOverriding()) {
                        AbsDef baseDefinition = baseClass.childType.findMemberForName(def.getName());

                        if (baseDefinition == null || baseDefinition.isStatic()) {
                            Report.error(def.position, "Method does not override any method from it's super class");
                        }
                        else if (baseDefinition.isFinal()) {
                            Report.error(def.position, "Cannot override \"final\" instance method");
                        }
                    }
                    else if (baseClass.containsMember(def.getName())) {
                        Report.error(def.position, "Invalid redeclaration of \"" + def.getName() + "\"");
                    }
                }
                else if (def.isOverriding()) {
                    Report.error(def.position, "Method does not override any method from it's super class");
                }

                def.accept(this);

                if (def.isStatic()) {
                    staticNames.add(def.getName());
                    staticDefinitions.add(def);
                }
                else {
                    names.add(def.getName());
                }
            }

            for (AbsFunDef c : acceptor.construstors) {
                c.accept(this);
            }

            for (AbsDef def : acceptor.definitions.definitions) {
                def.accept(this);

                if (def.isStatic()) {
                    staticTypes.add(SymbDesc.getType(def));
                }
                else {
                    types.add(SymbDesc.getType(def));
                }
            }

            ObjectType type;
            if (acceptor instanceof AbsStructDef) {
                type = new StructType(acceptor, names, types);
            }
            else {
                type = new ClassType(acceptor, names, types, baseClass);
            }

            CanType canType = new CanType(type);
            SymbDesc.setType(acceptor, canType);

            // add static member definitions to canType
            Iterator<String> namesIterator = staticNames.iterator();
            Iterator<Type> typesIterator = staticTypes.iterator();
            Iterator<AbsDef> defsIterator = staticDefinitions.iterator();

            while (namesIterator.hasNext()) {
                canType.addStaticDefinition(defsIterator.next(), namesIterator.next(), typesIterator.next());
            }
        }
        else {
            resolveTypeOnly = false;

            CanType staticType = (CanType) SymbDesc.getType(acceptor);
            ObjectType objectType = (ObjectType) staticType.childType;
	        CanType baseClass = objectType.baseClass;

	        declarationContext.push(staticType);

            AbsFunDef baseClassDefaultConstructor = null;
            if (baseClass != null) {
                baseClassDefaultConstructor = ((ClassType) baseClass.childType).classDefinition.defaultConstructor;
            }

            // check if all methods in conforming interfaces are implemented
            for (AbsType conformance : acceptor.conformances) {
                InterfaceType interfaceType = (InterfaceType) SymbDesc.getType(conformance);

                if (!objectType.conformsTo(interfaceType)) {
                    Report.error(conformance.position, "Type \"" + objectType.friendlyName() + "\" does not conform to type \"" + interfaceType.friendlyName() + "\"");
                }
            }

            // add implicit "self: classType" parameter to constructors
            for (AbsFunDef constructor : acceptor.construstors) {
                AbsParDef selfParDef = constructor.getParameterForIndex(0);
                selfParDef.type = new AbsTypeName(selfParDef.position, acceptor.name);

                SymbDesc.setNameDef(selfParDef.type, acceptor);
                SymbDesc.setType(selfParDef, objectType);

                // append base classes' initialization code
                if (baseClassDefaultConstructor != null) {
                    constructor.func.statements.addAll(0, baseClassDefaultConstructor.func.statements);
                }

                constructor.accept(this);

                FunctionType funType = (FunctionType) SymbDesc.getType(constructor);
                funType.resultType = objectType;

                SymbDesc.setType(constructor, funType);
            }

            for (AbsDef def : acceptor.definitions.definitions) {
                if (!def.isStatic() && def instanceof AbsFunDef) {
                    // add implicit "self: classType" parameter to instance methods
                    AbsFunDef funDef = (AbsFunDef) def;
                    AbsParDef selfParDef = funDef.getParameterForIndex(0);
                    selfParDef.type = new AbsTypeName(selfParDef.position, acceptor.name);

                    SymbDesc.setNameDef(selfParDef.type, acceptor);
                }

                def.accept(this);

                // push constructors from nested class definition
                if (def instanceof AbsClassDef) {
                    CanType context = declarationContext.peek();
                    if (context != null) {
                        AbsClassDef classDef = (AbsClassDef) def;
                        String className = classDef.name;
                        classDef.name = context.childType.friendlyName() + "." + classDef.getName();

                        for (AbsFunDef constructor: classDef.construstors) {
                            context.addStaticMember(
                                    constructor,
                                    constructor.getStringRepresentation(className),
                                    SymbDesc.getType(constructor));

                        }
                    }
                }
            }

            declarationContext.pop();

//            objectType.debugPrint();
        }
	}

	@Override
	public void visit(AbsAtomConstExpr acceptor) {
		SymbDesc.setType(acceptor, Type.atomType(acceptor.type));
	}

	@Override
	public void visit(AbsAtomType acceptor) {
		SymbDesc.setType(acceptor, Type.atomType(acceptor.type));
	}

	@Override
	public void visit(AbsBinExpr acceptor) {
	    assign = acceptor.oper == AbsBinExpr.ASSIGN;

		acceptor.expr1.accept(this);
        Type lhs = SymbDesc.getType(acceptor.expr1);

		lhsTypes.push(lhs);
		
		if (acceptor.oper != AbsBinExpr.DOT)
			acceptor.expr2.accept(this);

        lhsTypes.pop();

        assign = false;

		Type rhs = SymbDesc.getType(acceptor.expr2);

		int oper = acceptor.oper;

		/**
		 * expr1[expr2]
		 */
		if (oper == AbsBinExpr.ARR) {
			if (!rhs.isBuiltinIntType())
				Report.error(acceptor.expr2.position,
						LanguageManager.localize("type_error_expected_int_for_subscript"));
			/**
			 * expr1 is of type ARR(n, t)
			 */
			if (lhs.isArrayType()) {
				SymbDesc.setType(acceptor, ((ArrayType) lhs).type);
			}
			else {
                Report.error(acceptor.expr1.position,
                        LanguageManager.localize("type_error_type_has_no_subscripts",
                                lhs.toString()));
            }
		}

		/**
		 * expr1 = expr2
		 */
		else if (oper == AbsBinExpr.ASSIGN) {
		    boolean success = false;

            if (rhs.isArrayType()) {
                System.out.println();
            }

            // type inference: if lhs doesn't have type, assign rhs type
			if (lhs == null) {
                SymbDesc.setType(acceptor, rhs);
				SymbDesc.setType(acceptor.expr1, rhs);
				SymbDesc.setType(SymbDesc.getNameDef(acceptor.expr1), rhs);

				if (rhs.isArrayType() && ((ArrayType) rhs).type.sameStructureAs(Type.anyType)) {
				    Report.warning(acceptor.expr2.position, "Implicitly inferred Any type");
                }

				success = true;
			}
			
			// t1 and t2 are of same structure
			else if (lhs.sameStructureAs(rhs)) {
				SymbDesc.setType(acceptor, lhs);
				success = true;
			}
			// t2 can be casted to t1
			else if (rhs.canCastTo(lhs)) {
				SymbDesc.setType(acceptor, lhs);
				SymbDesc.setType(acceptor.expr2, rhs);
				success = true;
			}
			// nil can be assigned to any pointer type
			else if (rhs.isBuiltinNilType() && lhs.isReferenceType()) {
				SymbDesc.setType(acceptor.expr2, lhs);
				SymbDesc.setType(acceptor, lhs);
				success = true;
			}
			// optionals
			else if (lhs.isOptionalType() && ((OptionalType) lhs).childType.sameStructureAs(rhs)) {
				SymbDesc.setType(acceptor, lhs);
				success = true;
			}
			
			if (!success) {
                Report.error(acceptor.position,
                        LanguageManager.localize("type_error_cannot_convert_type",
                                rhs.friendlyName(), lhs.friendlyName()));
            }

			assign = false;
		}

		/**
		 * identifier.identifier
		 */
		else if (oper == AbsBinExpr.DOT) {
            String memberName = null;
            if (acceptor.expr2 instanceof AbsVarNameExpr) {
                memberName = ((AbsVarNameExpr) acceptor.expr2).name;
            }
            else if (acceptor.expr2 instanceof AbsFunCall) {
                memberName = ((AbsFunCall) acceptor.expr2).getStringRepresentation();
            }
            else if (acceptor.expr2 instanceof AbsAtomConstExpr) {
                memberName = ((AbsAtomConstExpr) acceptor.expr2).value;
            }
            else if (acceptor.expr2 instanceof AbsBinExpr) {
                Report.error(acceptor.position, "Not yet supported");
            }

			/**
			 * Handle list.count
			 */
			// FIXME: - remove this in the future
			if (lhs.isArrayType()) {
				if (!memberName.equals("count"))
                    Report.error(acceptor.position, "Value of type \"" + lhs.friendlyName() + "\" has no member named \"" + memberName + "\"");
				SymbDesc.setType(acceptor, Type.intType);
				return;
			}
            if (lhs.isOptionalType()) {
			    Report.error(acceptor.position, "Value of type \"" + lhs.friendlyName() + "\" not unwrapped");
            }

			if (lhs.isObjectType()) {
			    if (acceptor.expr2 instanceof AbsFunCall) {
			        AbsFunCall fnCall = (AbsFunCall) acceptor.expr2;

                    // add implicit "self" argument to instance method
                    AbsLabeledExpr selfArg = new AbsLabeledExpr(acceptor.position, acceptor.expr1, Constants.selfParameterIdentifier);
                    fnCall.addArgument(selfArg);

                    memberName = fnCall.getStringRepresentation();
                }

				if (!lhs.containsMember(memberName)) {
                    Report.error(acceptor.expr2.position,
                            LanguageManager.localize(
                                    "type_error_member_not_found",
                                    lhs.friendlyName(),
                                    memberName));
                }
				
				AbsDef definition = lhs.findMemberForName(memberName);
				AbsDef objectDefinition = SymbDesc.getNameDef(acceptor.expr1);

				// check for access control (if object's name is not "self")
                if (!objectDefinition.name.equals(Constants.selfParameterIdentifier)) {
                    if (definition.isPrivate()) {
                        Report.error(acceptor.expr2.position,
                                "Member '" + memberName + "' is inaccessible due to it's private protection level");
                    }
                }
				
				SymbDesc.setNameDef(acceptor.expr2, definition);
				SymbDesc.setNameDef(acceptor, definition);

				if (acceptor.expr2 instanceof AbsFunCall) {
                    for (AbsExpr arg: ((AbsFunCall) acceptor.expr2).args) {
                        arg.accept(this);
                    }
                }
	
				Type memberType = ((ObjectType) lhs).getMemberTypeForName(memberName);
				Type acceptorType = memberType.isFunctionType() ? ((FunctionType) memberType).resultType : memberType;

                SymbDesc.setType(acceptor.expr2, memberType);
				SymbDesc.setType(acceptor, acceptorType);
			}
			
			else if (lhs.isEnumType()) {
                EnumType enumType = (EnumType) lhs;

                if (enumType.selectedMember == null) {
                    if (!enumType.containsMember(memberName))
                        Report.error(acceptor.expr2.position,
                                LanguageManager.localize("type_error_member_not_found",
                                        enumType.friendlyName(),
                                        memberName));

                    AbsDef definition = enumType.findMemberForName(memberName);

                    if (definition.isPrivate())
                        Report.error(acceptor.expr2.position,
                                "Member '" + memberName + "' is inaccessible due to it's private protection level");

                    SymbDesc.setNameDef(acceptor.expr2, definition);
                    SymbDesc.setNameDef(acceptor, definition);

                    EnumType memberType = new EnumType(enumType, memberName);

                    SymbDesc.setType(acceptor.expr2, memberType);
                    SymbDesc.setType(acceptor, memberType);
                }
                else {
                    ClassType memberType = enumType.getMemberTypeForName(enumType.selectedMember);

                    if (!memberType.containsMember(memberName))
                        Report.error(acceptor.expr2.position,
                                LanguageManager.localize("type_error_member_not_found",
                                        lhs.toString(),
                                        memberName));

                    Type memberRawValueType = memberType.getMemberTypeForName(memberName);

                    SymbDesc.setType(acceptor.expr2, memberRawValueType);
                    SymbDesc.setType(acceptor, memberRawValueType);
                }
            }
			
			else if (lhs.isTupleType()) {
				TupleType tupleType = (TupleType) lhs;

				SymbDesc.setType(acceptor.expr2, tupleType.typeForName(memberName));
				SymbDesc.setType(acceptor, tupleType.typeForName(memberName));
			}

			else if (lhs.isCanType()) {
			    CanType canType = (CanType) lhs;

                if (acceptor.expr2 instanceof AbsFunCall) {
                    AbsDef def = canType.findMemberForName(((AbsFunCall) acceptor.expr2).name);

                    if (def != null && def instanceof AbsClassDef) {
                        AbsFunCall fnCall = (AbsFunCall) acceptor.expr2;

                        // add implicit "self" argument to instance method
                        AbsLabeledExpr selfArg = new AbsLabeledExpr(acceptor.position, acceptor.expr1, Constants.selfParameterIdentifier);
                        fnCall.addArgument(selfArg);

                        memberName = fnCall.getStringRepresentation();
                    }
                }

			    if (!canType.containsStaticMember(memberName)) {
                    Report.error(acceptor.expr2.position,
                            LanguageManager.localize("type_error_member_not_found",
                                    lhs.friendlyName(),
                                    memberName));
                }

                if (acceptor.expr2 instanceof AbsFunCall) {
                    for (AbsExpr arg: ((AbsFunCall) acceptor.expr2).args) {
                        arg.accept(this);
                    }
                }

                AbsDef definition = canType.findStaticMemberForName(memberName);

                if (definition.isPrivate()) {
                    Report.error(acceptor.expr2.position,
                            "Member '" + memberName + "' is inaccessible due to it's private protection level");
                }

                SymbDesc.setNameDef(acceptor.expr2, definition);
                SymbDesc.setNameDef(acceptor, definition);

                Type memberType = canType.getStaticMemberTypeForName(memberName);
                Type acceptorType = memberType.isFunctionType() ? ((FunctionType) memberType).resultType : memberType;

                SymbDesc.setType(acceptor.expr2, memberType);
                SymbDesc.setType(acceptor, acceptorType);
            }

            else if (lhs.isInterfaceType()) {
			    InterfaceType interfaceType = (InterfaceType) lhs;

                if (acceptor.expr2 instanceof AbsFunCall) {
                    AbsFunCall fnCall = (AbsFunCall) acceptor.expr2;

                    // add implicit "self" argument to instance method
                    fnCall.addArgument(new AbsLabeledExpr(acceptor.position, acceptor.expr1, Constants.selfParameterIdentifier));
                    memberName = fnCall.getStringRepresentation();

                    for (AbsExpr arg: fnCall.args) {
                        arg.accept(this);
                    }
                }
                else {
                    Report.error(acceptor.position, "Value of type \"" + lhs.friendlyName() + "\" has no member named \"" + memberName + "\"");
                }
                AbsDef def = interfaceType.findMemberForName(memberName);
			    if (def == null) {
                    Report.error(acceptor.position, "Value of type \"" + lhs.friendlyName() + "\" has no member named \"" + memberName + "\"");
                }

                FunctionType functionType = (FunctionType) SymbDesc.getType(def);

                SymbDesc.setNameDef(acceptor.expr2, def);
                SymbDesc.setType(acceptor.expr2, functionType);
            }
            else {
                Report.error(acceptor.position, "Value of type \"" + lhs.friendlyName() + "\" has no member named \"" + memberName + "\"");
            }
		}

        /**
         * reference type comparison to nil
         */
        else if (lhs.isReferenceType() && rhs.isBuiltinNilType()) {
            SymbDesc.setType(acceptor, Type.boolType);
        }

        /**
         * expr1 is expr2
         */
		else if (oper == AbsBinExpr.IS) {
		    if (!lhs.isCanType() && rhs.isCanType()) {
		        SymbDesc.setType(acceptor, Type.boolType);
            }
        }

        /**
         * expr1 as expr2
         */
        else if (oper == AbsBinExpr.AS) {
            if (rhs.isCanType()) {
                SymbDesc.setType(acceptor, ((CanType) rhs).childType);
            }
        }

		/**
		 * expr1 and expr2 are of type Bool
		 */
		else if (lhs.isBuiltinBoolType() && rhs.isBuiltinBoolType()) {
			// ==, !=, <=, >=, <, >, &, |
			if (oper >= 0 && oper <= 7) {
                SymbDesc.setType(acceptor, Type.boolType);
            }
			else {
                Report.error(
                        acceptor.position,
                        "Numeric operations \"+\", \"-\", \"*\", \"/\" and \"%\" are undefined for type Bool");
            }
		}
		/**
		 * expr1 and expr2 are of type Int
		 */
		else if (lhs.isBuiltinIntType() && rhs.isBuiltinIntType()) {
			// +, -, *, /, %
			if (oper >= 8 && oper <= 12) {
                SymbDesc.setType(acceptor, Type.intType);
            }
			// ==, !=, <=, >=, <, >
			else if (oper >= 2 && oper <= 7) {
                SymbDesc.setType(acceptor, Type.boolType);
            }
			else {
                Report.error(acceptor.position,
                        "Logical operations \"&\" and \"|\" are undefined for type Int");
            }
		}
		/**
		 * expr1 and expr2 are of type Double
		 */
		else if (lhs.isBuiltinDoubleType() && rhs.isBuiltinDoubleType()) {
			// +, -, *, /, %
			if (oper >= 8 && oper <= 12) {
                SymbDesc.setType(acceptor, Type.doubleType);
            }
			// ==, !=, <=, >=, <, >
			else if (oper >= 2 && oper <= 7) {
                SymbDesc.setType(acceptor, Type.boolType);
            }
			else {
                Report.error(acceptor.position,
                        "Logical operations \"&\" and \"|\" are undefined for type Double");
            }
		}
		/**
		 * expr1 or expr2 is Double and the other is Int (implicit cast to Double)
		 */
		// FIXME
		else if (lhs.isBuiltinDoubleType() && rhs.isBuiltinIntType()
				|| lhs.isAtomType() && rhs.isBuiltinDoubleType()) {
			// +, -, *, /, %
			if (oper >= 8 && oper <= 12) {
                SymbDesc.setType(acceptor, Type.doubleType);
            }
			// ==, !=, <=, >=, <, >
			else if (oper >= 2 && oper <= 7) {
                SymbDesc.setType(acceptor, Type.boolType);
            }
			else {
                Report.error(acceptor.position,
                        "Logical operations \"&\" and \"|\" are undefined for type Double");
            }
		}
		
		/**
		 * Enumerations comparison.
		 */
		else if (lhs.isEnumType() && lhs.sameStructureAs(rhs)) {
			if (oper == AbsBinExpr.EQU) {
                SymbDesc.setType(acceptor, Type.boolType);
            }
			else {
                Report.error(acceptor.position,
                        "Operator cannot be applied to operands of type \"" +
                                lhs.toString() + "\" and \"" + rhs.toString() + "\"");
            }
		}
		else {
			Report.error(acceptor.position, "No viable operation for types "
					+ lhs.friendlyName() + " and " + rhs.friendlyName());
		}
	}

	@Override
	public void visit(AbsDefs acceptor) {
        if (isRootNode) {
            isRootNode = false;

            for (TraversalState state : TraversalState.values()) {
                traversalState = state;

                for (AbsDef s : acceptor.definitions) {
                    s.accept(this);
                }
            }

            for (AtomType atomType: Type.atomTypes) {
                for (AbsType conformance : atomType.classDefinition.conformances) {
                    if (!atomType.conformsTo((InterfaceType) SymbDesc.getType(conformance))) {
                        Report.error(conformance.position, "Type \"" + atomType.friendlyName() + "\" does not conform to interface \"" + conformance.getName() + "\"");
                    }
                }
            }
        }
        else {
            for (AbsDef s : acceptor.definitions) {
                s.accept(this);
            }
        }
	}

	@Override
	public void visit(AbsExprs acceptor) {
		if (acceptor.expressions.size() == 0)
			SymbDesc.setType(acceptor, Type.voidType);
		else {
			for (AbsExpr e : acceptor.expressions)
				e.accept(this);
		}
	}

	@Override
	public void visit(AbsForStmt acceptor) {
		acceptor.collection.accept(this);
		Type type = ((ArrayType)SymbDesc.getType(acceptor.collection)).type;

		SymbDesc.setType(SymbDesc.getNameDef(acceptor.iterator), type);
		SymbDesc.setType(acceptor, Type.voidType);
		
		acceptor.iterator.accept(this);
		acceptor.body.accept(this);
	}

	@Override
	public void visit(AbsFunCall acceptor) {
		AbsFunDef definition = (AbsFunDef) SymbTable.fnd(acceptor.getStringRepresentation());
		FunctionType funType = (FunctionType) SymbDesc.getType(definition);

		SymbDesc.setType(acceptor, funType.resultType);
		boolean isConstructor = definition.isConstructor;

		for (int i = 0; i < acceptor.numArgs(); i++) {
			AbsExpr arg = acceptor.arg(i);

            // skip first ("self") argument if function is constructor
            if (isConstructor && i == 0) {
                SymbDesc.setType(arg, funType.resultType);
                continue;
            }

			arg.accept(this);

			Type argType = SymbDesc.getType(arg);

            if (argType == null) return;

			if (!(funType.getParType(i).sameStructureAs(argType))) {
				Report.error(arg.position, "Cannot assign value of type \"" +
                        argType.friendlyName() + "\" to type \"" + funType.getParType(i).friendlyName() + "\"");
			}
		}
	}

	@Override
	public void visit(AbsFunDef acceptor) {
        Vector<Type> parameters = new Vector<>();

        for (AbsParDef par : acceptor.getParamaters()) {
            par.accept(this);
            parameters.add(SymbDesc.getType(par));
        }

        acceptor.type.accept(this);
        Type returnType = SymbDesc.getType(acceptor.type);

        FunctionType funType = new FunctionType(parameters, returnType, acceptor);
        SymbDesc.setType(acceptor, funType);

        if (!resolveTypeOnly){
			// check if return type matches
			for (AbsStmt stmt : acceptor.func.statements) {
			    stmt.accept(this);

				if (stmt instanceof AbsReturnExpr) {
					Type t = SymbDesc.getType(stmt);

					if (!t.sameStructureAs(funType.resultType)) {
                        Report.error(stmt.position,
                                "Return type doesn't match, expected \""
                                        + funType.resultType.friendlyName()
                                        + "\", got \""
                                        + t.friendlyName()
                                        + "\" instead");
                    }
				}
			}
		}
	}

	@Override
	public void visit(AbsIfStmt acceptor) {
		for (Condition c : acceptor.conditions) {
			c.cond.accept(this);
			c.body.accept(this);
			
			if (SymbDesc.getType(c.cond).sameStructureAs(Type.boolType))
				SymbDesc.setType(acceptor, Type.voidType);
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
		
		Type type = SymbDesc.getType(acceptor.type);
		if (type.isCanType())
			type = ((CanType) type).childType;

		SymbDesc.setType(acceptor, type);
	}

	@Override
	public void visit(AbsTypeName acceptor) {
        if (acceptor.name.equals("Int")) {
            SymbDesc.setType(acceptor, Type.intType);
            return;
        }
        if (acceptor.name.equals("Double")) {
            SymbDesc.setType(acceptor, Type.doubleType);
            return;
        }
        if (acceptor.name.equals("String")) {
            SymbDesc.setType(acceptor, Type.stringType);
            return;
        }
        if (acceptor.name.equals("Char")) {
            SymbDesc.setType(acceptor, Type.charType);
            return;
        }
        if (acceptor.name.equals("Void")) {
            SymbDesc.setType(acceptor, Type.voidType);
            return;
        }

		AbsDef definition = SymbDesc.getNameDef(acceptor);
		
		if (!(definition instanceof AbsTypeDef))
			Report.error(acceptor.position, "Use of undeclared type \'" + definition.name + "\'");

		Type type = SymbDesc.getType(definition);

		if (type == null)
			Report.error(acceptor.position, "Type \"" + acceptor.name
					+ "\" is undefined");

		SymbDesc.setType(acceptor, type);
	}

	@Override
	public void visit(AbsUnExpr acceptor) {
		acceptor.expr.accept(this);
		Type type = SymbDesc.getType(acceptor.expr);

		if (acceptor.oper == AbsUnExpr.NOT) {
			if (type.sameStructureAs(Type.boolType))
				SymbDesc.setType(acceptor, Type.boolType);
			else
				Report.error(acceptor.position,
						"Operator \"!\" is not defined for type " + type);
		} else if (acceptor.oper == AbsUnExpr.ADD || acceptor.oper == AbsUnExpr.SUB) {
			if (type.isBuiltinIntType() || type.canCastTo(Type.intType))
				SymbDesc.setType(acceptor, type);
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

			Type type = SymbDesc.getType(acceptor.type);
			if (type.isCanType())
				type = ((CanType) type).childType;
			
			SymbDesc.setType(acceptor, type);
		}
	}

	@Override
	public void visit(AbsVarNameExpr acceptor) {
        if (acceptor.name.equals("Int")) {
            SymbDesc.setType(acceptor, Type.intType.staticType);
            return;
        }
        if (acceptor.name.equals("Double")) {
            SymbDesc.setType(acceptor, Type.doubleType.staticType);
            return;
        }
        if (acceptor.name.equals("String")) {
            SymbDesc.setType(acceptor, Type.stringType.staticType);
            return;
        }
        if (acceptor.name.equals("Char")) {
            SymbDesc.setType(acceptor, Type.charType.staticType);
            return;
        }
        if (acceptor.name.equals("Void")) {
            SymbDesc.setType(acceptor, Type.voidType.staticType);
            return;
        }

        Type type = SymbDesc.getType(SymbDesc.getNameDef(acceptor));

        if (!assign && type.isOptionalType()) {
            OptionalType optionalType = (OptionalType) type;

            // implicitly force forced OptionalTyped
            if (optionalType.isForced) {
                type = optionalType.childType;
            }
        }

		SymbDesc.setType(acceptor, type);
	}

	@Override
	public void visit(AbsWhileStmt acceptor) {
		acceptor.cond.accept(this);
		acceptor.body.accept(this);

		if (SymbDesc.getType(acceptor.cond).sameStructureAs(Type.boolType)) {
            SymbDesc.setType(acceptor, Type.voidType);
        }
		else {
            Report.error(acceptor.cond.position, "Condition must be typed as Boolean");
        }
	}

	@Override
	public void visit(AbsImportDef importDef) {
	    if (traversalState == TraversalState.extensions) {
            String tmp = Report.fileName;
            Report.fileName = importDef.getName();

            importDef.imports.accept(new TypeChecker());

            Report.fileName = tmp;
        }
	}

	@Override
	public void visit(AbsStmts stmts) {
	    if (isRootNode) {
	        isRootNode = false;

            for (TraversalState state : TraversalState.values()) {
                traversalState = state;

                for (AbsStmt s : stmts.statements) {
                    if (traversalState == TraversalState.extensions && !(s instanceof AbsDef))
                        continue;

                    s.accept(this);
                }
            }
        }
        else {
            for (AbsStmt s : stmts.statements) {
                s.accept(this);
            }
        }
	}

	@Override
	public void visit(AbsReturnExpr returnExpr) {
		if (returnExpr.expr != null) {
			returnExpr.expr.accept(this);

			SymbDesc.setType(returnExpr, SymbDesc.getType(returnExpr.expr));
		}
		else {
            SymbDesc.setType(returnExpr, Type.voidType);
        }
	}

	@Override
	public void visit(AbsListExpr absListExpr) {
		Vector<Type> vec = new Vector<>();
		Type base = null;

		if (!lhsTypes.isEmpty()) {
            base = lhsTypes.peek();
            if (base != null && base.isArrayType()) {
                base = ((ArrayType) base).type;
            }
        }

		for (AbsExpr e : absListExpr.expressions) {
			e.accept(this);

			Type t = SymbDesc.getType(e);

			if (base == null) {
			    base = t;
            }
			else if (t.sameStructureAs(base) || t.canCastTo(base)) {}
            else {
                if (base.canCastTo(t)) {
                    base = t;
                }
                else {
                    base = Type.anyType;
                }
            }

			vec.add(SymbDesc.getType(e));
		}

		SymbDesc.setType(absListExpr, new ArrayType(base, vec.size()));
	}

	@Override
	public void visit(AbsFunType funType) {
		Vector<Type> parameters = new Vector<>();
		for (AbsType t : funType.parameterTypes) {
			t.accept(this);
			parameters.add(SymbDesc.getType(t));
		}
		funType.returnType.accept(this);
		
		SymbDesc.setType(funType, new FunctionType(parameters, SymbDesc.getType(funType.returnType), null));
	}

	@Override
	public void visit(AbsControlTransferStmt acceptor) {
		///
	}

	@Override
	public void visit(AbsSwitchStmt switchStmt) {
		switchStmt.subjectExpr.accept(this);
		
		Type switchType = SymbDesc.getType(switchStmt.subjectExpr);
		
		for (AbsCaseStmt singleCase : switchStmt.cases) {
			singleCase.accept(this);
			for (AbsExpr e : singleCase.exprs) {
				Type caseType = SymbDesc.getType(e);
				if (!caseType.sameStructureAs(switchType))
					Report.error(e.position, 
							"Expression of type \"" + caseType.toString() + 
							"\" cannot match values of type \"" + switchType.toString() +"\"");
			}
		}
		
		if (switchStmt.defaultBody != null) {
            switchStmt.defaultBody.accept(this);
        }
		
		SymbDesc.setType(switchStmt, Type.voidType);
	}

	@Override
	public void visit(AbsCaseStmt acceptor) {
		for (AbsExpr e : acceptor.exprs) {
            e.accept(this);
        }

		SymbTable.newScope();
		acceptor.body.accept(this);
		SymbTable.oldScope();
	}

	@Override
	public void visit(AbsEnumDef acceptor) {
        if (traversalState == TraversalState.extensions) return;

        AtomType enumRawValueType = null;
		
		if (acceptor.type != null) {
			acceptor.type.accept(this);
			enumRawValueType = (AtomType) SymbDesc.getType(acceptor.type);
		}
		
		String previousValue = null;
		int iterator = 0;
		
		ArrayList<String> names = new ArrayList<>(acceptor.definitions.size());
		ArrayList<ClassType> types = new ArrayList<>(acceptor.definitions.size());
		
		for (AbsDef def : acceptor.definitions) {
			if (def instanceof AbsEnumMemberDef) {
				AbsEnumMemberDef enumMemberDef = (AbsEnumMemberDef) def;
				
				if (enumRawValueType != null && enumMemberDef.value == null) {
					if (!enumRawValueType.isBuiltinStringType() && !enumRawValueType.isBuiltinIntType())
						Report.error(enumMemberDef.position, 
								"Enum members require explicit raw values when "
								+ "the raw type is not Int or String literal");
					
					String value = null;
					
					if (enumRawValueType.type == AtomTypeKind.STR)
						value = "\"" + enumMemberDef.name.name + "\"";
					else if (enumRawValueType.type == AtomTypeKind.INT) {
						if (previousValue == null)
							value = "" + iterator;
						else
							value = "" + (Integer.parseInt(previousValue) + 1);
					}
					
					enumMemberDef.value = new AbsAtomConstExpr(enumMemberDef.position, enumRawValueType.type, value);
	
					previousValue = value;
					iterator++;
				}

				def.accept(this);
				ClassType defType = (ClassType) SymbDesc.getType(def);
				
				if (defType.containsMember("rawValue")) {
					Type rawValueType = defType.getMemberTypeForName("rawValue");
					
					if (enumRawValueType == null)
						Report.error(enumMemberDef.value.position, 
								"Enum member cannot have a raw value "
								+ "if the enum doesn't have a raw type");
					
					
					if (!rawValueType.sameStructureAs(enumRawValueType))
						Report.error(enumMemberDef.value.position, 
								"Cannot assign value of type \"" +
								rawValueType.toString() + "\" to type \"" + 
								enumRawValueType.toString() + "\"");

					previousValue = enumMemberDef.value.value;
				}
			}

			types.add((ClassType) SymbDesc.getType(def));
			names.add(def.getName());
		}

		SymbDesc.setType(acceptor, new EnumType(acceptor, names, types));
	}

	@Override
	public void visit(AbsEnumMemberDef acceptor) {
        LinkedList<String> names = new LinkedList<>();
        LinkedList<Type> types = new LinkedList<>();
		LinkedList<AbsDef> definitions = new LinkedList<>();
		
		acceptor.name.accept(this);
		
		if (acceptor.value != null) {
			acceptor.value.accept(this);
			
			names.add("rawValue");
			types.add(SymbDesc.getType(acceptor.value));
			definitions.add(new AbsVarDef(acceptor.position, "rawValue", new AbsAtomType(null, null)));
		}
		
		AbsClassDef classDef = new AbsClassDef(acceptor.getName(), acceptor.position, 
				 definitions, new LinkedList<>(), new LinkedList<>());
		ClassType type = new ClassType(classDef, names, types);
		SymbDesc.setType(acceptor, type);
	}

	@Override
	public void visit(AbsTupleDef acceptor) {
        if (traversalState == TraversalState.extensions) return;

        LinkedList<Type> types = new LinkedList<>();
		LinkedList<String> names = new LinkedList<>();
		
		for (AbsDef def : acceptor.definitions.definitions) {
			def.accept(this);
			
			names.add(def.getName());
			types.add(SymbDesc.getType(def));
		}
		
		TupleType tupleType = new TupleType(acceptor, types, names);
		SymbDesc.setType(acceptor, tupleType);
	}

	@Override
	public void visit(AbsLabeledExpr acceptor) {
		acceptor.expr.accept(this);
		
		Type exprType = SymbDesc.getType(acceptor.expr);
		SymbDesc.setType(acceptor, exprType);
	}

	@Override
	public void visit(AbsTupleExpr acceptor) {
		acceptor.expressions.accept(this);
		
		LinkedList<Type> types = new LinkedList<>();
		LinkedList<String> names = new LinkedList<>();

		for (AbsExpr e : acceptor.expressions.expressions) {
			AbsLabeledExpr labeledExpr = (AbsLabeledExpr) e;
			
			types.add(SymbDesc.getType(labeledExpr));
			names.add(labeledExpr.name);
		}

		// TODO:
        Type type = types.size() == 1 ? new TupleType(types, names) : new TupleType(types, names);
		SymbDesc.setType(acceptor, type);
	}

	@Override
	public void visit(AbsOptionalType acceptor) {
		acceptor.childType.accept(this);
		
		Type childType = SymbDesc.getType(acceptor.childType);
		if (childType.isCanType()) {
		    childType = ((CanType) childType).childType;
        }

		SymbDesc.setType(acceptor, new OptionalType(childType, acceptor.isForced));
	}

	@Override
	public void visit(AbsOptionalEvaluationExpr acceptor) {
		acceptor.subExpr.accept(this);

		Type childType = SymbDesc.getType(acceptor.subExpr);
        if (childType.isCanType()) {
            childType = ((CanType) childType).childType;
        }

		SymbDesc.setType(acceptor, new OptionalType(childType, false));
	}

	@Override
	public void visit(AbsForceValueExpr acceptor) {
		acceptor.subExpr.accept(this);

		Type type = SymbDesc.getType(acceptor.subExpr);
		
		if (type.isOptionalType()) {
            SymbDesc.setType(acceptor, ((OptionalType) type).childType);
        }
		else {
            Report.error(acceptor.position,
                    "Cannot unwrap value of non-optional type '" + type.toString() + "'");
        }
	}

    @Override
    public void visit(AbsExtensionDef acceptor) {
	    if (traversalState == TraversalState.extensions) {
            acceptor.extendingType.accept(this);

            for (AbsType conformance: acceptor.conformances) {
                conformance.accept(this);
            }

            Type type = SymbDesc.getType(acceptor.extendingType);
            boolean isAtomType = false;

            if (type.isAtomType()) {
                AtomType atomType = (AtomType) type;
                type = atomType.staticType;
                isAtomType = true;
            }

            SymbDesc.setType(acceptor, type);

            if (!type.isCanType() || !((CanType) type).childType.isObjectType()) {
                Report.error(acceptor.position, "Only classes can be extended (for now)");
            }

            ObjectType extendingType = (ObjectType) ((CanType) type).childType;

            resolveTypeOnly = true;

            for (AbsDef def : acceptor.definitions.definitions) {
                if (isAtomType) {
                    // atomic types methods are always final (not dynamic)
                    def.setModifier(Modifier.isFinal);
                }

                def.accept(this);

                String memberName = def.getName();
                Type memberType = SymbDesc.getType(def);

                if (def.isStatic()) {
                    if (!((CanType) type).addStaticMember(def, memberName, memberType)) {
                        Report.error(acceptor.position, "Invalid redeclaration of \"" + memberName + "\"");
                    }
                }
                else {
                    if (!extendingType.addMember(def, memberName, memberType)) {
                        Report.error(acceptor.position, "Invalid redeclaration of \"" + memberName + "\"");
                    }
                }
            }

            for (AbsType conformance : acceptor.conformances) {
                if (!extendingType.addConformance(conformance)) {
                    Report.error(conformance.position, "Redundant conformance \"" + conformance.getName() + "\"");
                }
            }

            resolveTypeOnly = false;
        }
        else {
            for (AbsDef def : acceptor.definitions.definitions) {
                if (def instanceof AbsFunDef) {
                    if (!def.isStatic()) {
                        // add implicit "self: classType" parameter to instance methods
                        AbsFunDef funDef = (AbsFunDef) def;
                        AbsParDef selfParDef = funDef.getParameterForIndex(0);
                        selfParDef.type = new AbsTypeName(selfParDef.position, acceptor.getName());

                        SymbDesc.setNameDef(selfParDef.type, acceptor);
                    }
                }

                def.accept(this);
            }
        }
    }

    @Override
    public void visit(AbsInterfaceDef acceptor) {
        acceptor.definitions.accept(this);

        InterfaceType type = new InterfaceType(acceptor);
        SymbDesc.setType(acceptor, type);

        if (acceptor.getName().equals(Constants.any)) {
            Type.anyType = type;
        }
    }
}
