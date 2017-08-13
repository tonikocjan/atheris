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
import compiler.seman.type.*;
import managers.LanguageManager;

/**
 * Preverjanje tipov.
 * 
 * @author toni
 */
public class TypeChecker implements ASTVisitor {

    private SymbolTableMap symbolTable;
    private SymbolDescriptionMap symbolDescription;
    private TraversalStates traversalState = TraversalStates.extensions;
    private boolean isRootNode = true;
    private boolean assigningToVariable = false;
    private Stack<Type> lhsTypes = new Stack<>();
    private Stack<CanType> declarationContext = new Stack<>();
    private boolean resolveTypeOnly = false;

    private enum TraversalStates {
        extensions, normal
    }

    public TypeChecker(SymbolTableMap symbolTable, SymbolDescriptionMap symbolDescription) {
        this.symbolTable = symbolTable;
        this.symbolDescription = symbolDescription;
    }

	@Override
	public void visit(AbsListType acceptor) {
		acceptor.type.accept(this);

		Type type = symbolDescription.getTypeForAstNode(acceptor.type);
		if (type.isCanType()) {
		    type = ((CanType) type).childType;
        }

		symbolDescription.setTypeForAstNode(acceptor, new ArrayType(type, acceptor.count));
	}

	@Override
	public void visit(AbsClassDef acceptor) {
	    if (traversalState == TraversalStates.extensions) {
            ArrayList<Type> types = new ArrayList<>();
            ArrayList<String> names = new ArrayList<>();

            ArrayList<Type> staticTypes = new ArrayList<>();
            ArrayList<String> staticNames = new ArrayList<>();
            ArrayList<AbsDef> staticDefinitions = new ArrayList<>();

            CanType baseClass = null;

            // check whether inheritance is legal
            if (acceptor.baseClass != null) {
                acceptor.baseClass.accept(this);

                Type type = symbolDescription.getTypeForAstNode(acceptor.baseClass);

                if (acceptor instanceof AbsStructDef) {
                    // only classes are allowed to inherit
                    Logger.error("Structs are not allowed to inherit");
                }

                if (!type.isInterfaceType() && (!type.isCanType() || !((CanType) type).childType.isClassType())) {
                    Logger.error(acceptor.baseClass.position,
                            "Inheritance from non-class type \"" + type.friendlyName() + "\" is not allowed");
                }

                if (type.isInterfaceType()) {
                    acceptor.conformances.add(0, acceptor.baseClass);
                }
                else {
                    baseClass = (CanType) type;
                }
            }

            // check whether conformance is legal
            for (AbsType conformance : acceptor.conformances) {
                conformance.accept(this);

                if (!symbolDescription.getTypeForAstNode(conformance).isInterfaceType()) {
                    if (baseClass != null) {
                        Logger.error(conformance.position, "Multiple inheritance is not allowed");
                    }
                    else {
                        Logger.error(conformance.position, "Super class must appear first in inheritance clause");
                    }
                }
            }

            // type inference
            for (AbsStmt stmt: acceptor.defaultConstructor.func.statements) {
                AbsBinExpr initExpr = (AbsBinExpr) stmt;
                initExpr.expr2.accept(this);
                Type type = symbolDescription.getTypeForAstNode(initExpr.expr2);

                AbsVarDef definition = (AbsVarDef) acceptor.definitions.findDefinitionForName(((AbsVarNameExpr) ((AbsBinExpr)initExpr.expr1).expr2).name);
                symbolDescription.setTypeForAstNode(definition, type);
            }

            resolveTypeOnly = true;

            for (AbsDef def : acceptor.definitions.definitions) {
                if (names.contains(def.getName())) {
                    Logger.error(def.position, "Invalid redeclaration of \"" + def.getName() + "\"");
                }
                else if (baseClass != null) {
                    // check whether inheritance (overriding) is legal
                    if (def.isOverriding()) {
                        AbsDef baseDefinition = baseClass.childType.findMemberDefinitionForName(def.getName());

                        if (baseDefinition == null || baseDefinition.isStatic()) {
                            Logger.error(def.position, "Method does not override any method from it's super class");
                        }
                        else if (baseDefinition.isFinal()) {
                            Logger.error(def.position, "Cannot override \"final\" instance method");
                        }
                    }
                    else if (baseClass.containsMember(def.getName())) {
                        Logger.error(def.position, "Invalid redeclaration of \"" + def.getName() + "\"");
                    }
                }
                else if (def.isOverriding()) {
                    Logger.error(def.position, "Method does not override any method from it's super class");
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
                    staticTypes.add(symbolDescription.getTypeForAstNode(def));
                }
                else {
                    types.add(symbolDescription.getTypeForAstNode(def));
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
            symbolDescription.setTypeForAstNode(acceptor, canType);

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

            CanType staticType = (CanType) symbolDescription.getTypeForAstNode(acceptor);
            ObjectType objectType = (ObjectType) staticType.childType;
	        CanType baseClass = objectType.baseClass;

	        declarationContext.push(staticType);

            AbsFunDef baseClassDefaultConstructor = null;
            if (baseClass != null) {
                baseClassDefaultConstructor = ((ClassType) baseClass.childType).classDefinition.defaultConstructor;
            }

            // check if all methods in conforming interfaces are implemented
            for (AbsType conformance : acceptor.conformances) {
                InterfaceType interfaceType = (InterfaceType) symbolDescription.getTypeForAstNode(conformance);

                if (!objectType.conformsTo(interfaceType)) {
                    Logger.error(conformance.position, "Type \"" + objectType.friendlyName() + "\" does not conform to interface \"" + interfaceType.friendlyName() + "\"");
                }
            }

            // add implicit "self: classType" parameter to constructors
            for (AbsFunDef constructor : acceptor.construstors) {
                AbsParDef selfParDef = constructor.getParameterForIndex(0);
                selfParDef.type = new AbsTypeName(selfParDef.position, acceptor.name);

                symbolDescription.setDefinitionForAstNode(selfParDef.type, acceptor);
                symbolDescription.setTypeForAstNode(selfParDef, objectType);

                // append base classes' initialization code
                if (baseClassDefaultConstructor != null) {
                    constructor.func.statements.addAll(0, baseClassDefaultConstructor.func.statements);
                }

                constructor.accept(this);

                FunctionType funType = (FunctionType) symbolDescription.getTypeForAstNode(constructor);
                funType.resultType = objectType;

                symbolDescription.setTypeForAstNode(constructor, funType);
            }

            for (AbsDef def : acceptor.definitions.definitions) {
                if (!def.isStatic() && def instanceof AbsFunDef) {
                    // add implicit "self: classType" parameter to instance methods
                    AbsFunDef funDef = (AbsFunDef) def;
                    AbsParDef selfParDef = funDef.getParameterForIndex(0);
                    selfParDef.type = new AbsTypeName(selfParDef.position, acceptor.name);

                    symbolDescription.setDefinitionForAstNode(selfParDef.type, acceptor);
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
                                    symbolDescription.getTypeForAstNode(constructor));

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
		symbolDescription.setTypeForAstNode(acceptor, Type.atomType(acceptor.type));
	}

	@Override
	public void visit(AbsAtomType acceptor) {
		symbolDescription.setTypeForAstNode(acceptor, Type.atomType(acceptor.type));
	}

	@Override
	public void visit(AbsBinExpr acceptor) {
	    assigningToVariable = acceptor.oper == AbsBinExpr.ASSIGN;

		acceptor.expr1.accept(this);
        Type lhs = symbolDescription.getTypeForAstNode(acceptor.expr1);

		lhsTypes.push(lhs);
		
		if (acceptor.oper != AbsBinExpr.DOT)
			acceptor.expr2.accept(this);

        lhsTypes.pop();

        assigningToVariable = false;

		Type rhs = symbolDescription.getTypeForAstNode(acceptor.expr2);

		int oper = acceptor.oper;

		/**
		 * expr1[expr2]
		 */
		if (oper == AbsBinExpr.ARR) {
			if (!rhs.isBuiltinIntType())
				Logger.error(acceptor.expr2.position,
						LanguageManager.localize("type_error_expected_int_for_subscript"));
			/**
			 * expr1 is of type ARR(n, t)
			 */
			if (lhs.isArrayType()) {
				symbolDescription.setTypeForAstNode(acceptor, ((ArrayType) lhs).type);
			}
			else {
                Logger.error(acceptor.expr1.position,
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

            // type inference: if lhs doesn't have type, assigningToVariable rhs type
			if (lhs == null) {
                symbolDescription.setTypeForAstNode(acceptor, rhs);
				symbolDescription.setTypeForAstNode(acceptor.expr1, rhs);
				symbolDescription.setTypeForAstNode(symbolDescription.getDefinitionForAstNode(acceptor.expr1), rhs);

				if (rhs.isArrayType() && ((ArrayType) rhs).type.sameStructureAs(Type.anyType)) {
				    Logger.warning(acceptor.expr2.position, "Implicitly inferred Any type");
                }

				success = true;
			}
			
			// t1 and t2 are of same structure
			else if (lhs.sameStructureAs(rhs)) {
				symbolDescription.setTypeForAstNode(acceptor, lhs);
				success = true;
			}
			// t2 can be casted to t1
			else if (rhs.canBeCastedToType(lhs)) {
				symbolDescription.setTypeForAstNode(acceptor, lhs);
				symbolDescription.setTypeForAstNode(acceptor.expr2, rhs);
				success = true;
			}
			// nil can be assigned to any pointer type
			else if (rhs.isBuiltinNilType() && lhs.isReferenceType()) {
				symbolDescription.setTypeForAstNode(acceptor.expr2, lhs);
				symbolDescription.setTypeForAstNode(acceptor, lhs);
				success = true;
			}
			// optionals
			else if (lhs.isOptionalType() && ((OptionalType) lhs).childType.sameStructureAs(rhs)) {
				symbolDescription.setTypeForAstNode(acceptor, lhs);
				success = true;
			}
			
			if (!success) {
                Logger.error(acceptor.position,
                        LanguageManager.localize("type_error_cannot_convert_type",
                                rhs.friendlyName(), lhs.friendlyName()));
            }

			assigningToVariable = false;
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
                Logger.error(acceptor.position, "Not yet supported");
            }

			/**
			 * Handle list.count
			 */
			// FIXME: - remove this in the future
			if (lhs.isArrayType()) {
				if (!memberName.equals("count"))
                    Logger.error(acceptor.position, "Value of type \"" + lhs.friendlyName() + "\" has no member named \"" + memberName + "\"");
				symbolDescription.setTypeForAstNode(acceptor, Type.intType);
				return;
			}
            if (lhs.isOptionalType()) {
			    Logger.error(acceptor.position, "Value of type \"" + lhs.friendlyName() + "\" not unwrapped");
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
                    Logger.error(acceptor.expr2.position,
                            LanguageManager.localize(
                                    "type_error_member_not_found",
                                    lhs.friendlyName(),
                                    memberName));
                }
				
				AbsDef definition = lhs.findMemberDefinitionForName(memberName);
				AbsDef objectDefinition = symbolDescription.getDefinitionForAstNode(acceptor.expr1);

				// check for access control (if object's getName is not "self")
                if (!objectDefinition.name.equals(Constants.selfParameterIdentifier)) {
                    if (definition.isPrivate()) {
                        Logger.error(acceptor.expr2.position,
                                "Member '" + memberName + "' is inaccessible due to it's private protection staticLevel");
                    }
                }
				
				symbolDescription.setDefinitionForAstNode(acceptor.expr2, definition);
				symbolDescription.setDefinitionForAstNode(acceptor, definition);

				if (acceptor.expr2 instanceof AbsFunCall) {
                    for (AbsExpr arg: ((AbsFunCall) acceptor.expr2).args) {
                        arg.accept(this);
                    }
                }
	
				Type memberType = ((ObjectType) lhs).getMemberTypeForName(memberName);
				Type acceptorType = memberType.isFunctionType() ? ((FunctionType) memberType).resultType : memberType;

                symbolDescription.setTypeForAstNode(acceptor.expr2, memberType);
				symbolDescription.setTypeForAstNode(acceptor, acceptorType);
			}
			
			else if (lhs.isEnumType()) {
                EnumType enumType = (EnumType) lhs;

                if (enumType.selectedMember == null) {
                    if (!enumType.containsMember(memberName))
                        Logger.error(acceptor.expr2.position,
                                LanguageManager.localize("type_error_member_not_found",
                                        enumType.friendlyName(),
                                        memberName));

                    AbsDef definition = enumType.findMemberDefinitionForName(memberName);

                    if (definition.isPrivate())
                        Logger.error(acceptor.expr2.position,
                                "Member '" + memberName + "' is inaccessible due to it's private protection staticLevel");

                    symbolDescription.setDefinitionForAstNode(acceptor.expr2, definition);
                    symbolDescription.setDefinitionForAstNode(acceptor, definition);

                    EnumType memberType = new EnumType(enumType, memberName);

                    symbolDescription.setTypeForAstNode(acceptor.expr2, memberType);
                    symbolDescription.setTypeForAstNode(acceptor, memberType);
                }
                else {
                    ClassType memberType = enumType.getMemberTypeForName(enumType.selectedMember);

                    if (!memberType.containsMember(memberName))
                        Logger.error(acceptor.expr2.position,
                                LanguageManager.localize("type_error_member_not_found",
                                        lhs.toString(),
                                        memberName));

                    Type memberRawValueType = memberType.getMemberTypeForName(memberName);

                    symbolDescription.setTypeForAstNode(acceptor.expr2, memberRawValueType);
                    symbolDescription.setTypeForAstNode(acceptor, memberRawValueType);
                }
            }
			
			else if (lhs.isTupleType()) {
				TupleType tupleType = (TupleType) lhs;

				symbolDescription.setTypeForAstNode(acceptor.expr2, tupleType.typeForName(memberName));
				symbolDescription.setTypeForAstNode(acceptor, tupleType.typeForName(memberName));
			}

			else if (lhs.isCanType()) {
			    CanType canType = (CanType) lhs;

                if (acceptor.expr2 instanceof AbsFunCall) {
                    AbsDef def = canType.findMemberDefinitionForName(((AbsFunCall) acceptor.expr2).name);

                    if (def != null && def instanceof AbsClassDef) {
                        AbsFunCall fnCall = (AbsFunCall) acceptor.expr2;

                        // add implicit "self" argument to instance method
                        AbsLabeledExpr selfArg = new AbsLabeledExpr(acceptor.position, acceptor.expr1, Constants.selfParameterIdentifier);
                        fnCall.addArgument(selfArg);

                        memberName = fnCall.getStringRepresentation();
                    }
                }

			    if (!canType.containsStaticMember(memberName)) {
                    Logger.error(acceptor.expr2.position,
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
                    Logger.error(acceptor.expr2.position,
                            "Member '" + memberName + "' is inaccessible due to it's private protection staticLevel");
                }

                symbolDescription.setDefinitionForAstNode(acceptor.expr2, definition);
                symbolDescription.setDefinitionForAstNode(acceptor, definition);

                Type memberType = canType.getStaticMemberTypeForName(memberName);
                Type acceptorType = memberType.isFunctionType() ? ((FunctionType) memberType).resultType : memberType;

                symbolDescription.setTypeForAstNode(acceptor.expr2, memberType);
                symbolDescription.setTypeForAstNode(acceptor, acceptorType);
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
                    Logger.error(acceptor.position, "Value of type \"" + lhs.friendlyName() + "\" has no member named \"" + memberName + "\"");
                }
                AbsDef def = interfaceType.findMemberDefinitionForName(memberName);
			    if (def == null) {
                    Logger.error(acceptor.position, "Value of type \"" + lhs.friendlyName() + "\" has no member named \"" + memberName + "\"");
                }

                FunctionType functionType = (FunctionType) symbolDescription.getTypeForAstNode(def);

                symbolDescription.setDefinitionForAstNode(acceptor.expr2, def);
                symbolDescription.setTypeForAstNode(acceptor.expr2, functionType);
            }
            else {
                Logger.error(acceptor.position, "Value of type \"" + lhs.friendlyName() + "\" has no member named \"" + memberName + "\"");
            }
		}

        /**
         * reference type comparison to nil
         */
        else if (lhs.isReferenceType() && rhs.isBuiltinNilType()) {
            symbolDescription.setTypeForAstNode(acceptor, Type.boolType);
        }

        /**
         * expr1 is expr2
         */
		else if (oper == AbsBinExpr.IS) {
		    if (!lhs.isCanType() && rhs.isCanType()) {
		        symbolDescription.setTypeForAstNode(acceptor, Type.boolType);
            }
        }

        /**
         * expr1 as expr2
         */
        else if (oper == AbsBinExpr.AS) {
            if (rhs.isCanType()) {
                symbolDescription.setTypeForAstNode(acceptor, ((CanType) rhs).childType);
            }
        }

		/**
		 * expr1 and expr2 are of type Bool
		 */
		else if (lhs.isBuiltinBoolType() && rhs.isBuiltinBoolType()) {
			// ==, !=, <=, >=, <, >, &, |
			if (oper >= 0 && oper <= 7) {
                symbolDescription.setTypeForAstNode(acceptor, Type.boolType);
            }
			else {
                Logger.error(
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
                symbolDescription.setTypeForAstNode(acceptor, Type.intType);
            }
			// ==, !=, <=, >=, <, >
			else if (oper >= 2 && oper <= 7) {
                symbolDescription.setTypeForAstNode(acceptor, Type.boolType);
            }
			else {
                Logger.error(acceptor.position,
                        "Logical operations \"&\" and \"|\" are undefined for type Int");
            }
		}
		/**
		 * expr1 and expr2 are of type Double
		 */
		else if (lhs.isBuiltinDoubleType() && rhs.isBuiltinDoubleType()) {
			// +, -, *, /, %
			if (oper >= 8 && oper <= 12) {
                symbolDescription.setTypeForAstNode(acceptor, Type.doubleType);
            }
			// ==, !=, <=, >=, <, >
			else if (oper >= 2 && oper <= 7) {
                symbolDescription.setTypeForAstNode(acceptor, Type.boolType);
            }
			else {
                Logger.error(acceptor.position,
                        "Logical operations \"&\" and \"|\" are undefined for type Double");
            }
		}
		/**
		 * expr1 or expr2 is Double and the other is Int (implicit cast to Double)
		 */
		// FIXME
		else if (lhs.isBuiltinDoubleType() && rhs.isBuiltinIntType() || lhs.isAtomType() && rhs.isBuiltinDoubleType()) {
			// +, -, *, /, %
			if (oper >= 8 && oper <= 12) {
                symbolDescription.setTypeForAstNode(acceptor, Type.doubleType);
            }
			// ==, !=, <=, >=, <, >
			else if (oper >= 2 && oper <= 7) {
                symbolDescription.setTypeForAstNode(acceptor, Type.boolType);
            }
			else {
                Logger.error(acceptor.position,
                        "Logical operations \"&\" and \"|\" are undefined for type Double");
            }
		}
		
		/**
		 * Enumerations comparison.
		 */
		else if (lhs.isEnumType() && lhs.sameStructureAs(rhs)) {
			if (oper == AbsBinExpr.EQU) {
                symbolDescription.setTypeForAstNode(acceptor, Type.boolType);
            }
			else {
                Logger.error(acceptor.position,
                        "Operator cannot be applied to operands of type \"" + lhs.toString() + "\" and \"" + rhs.toString() + "\"");
            }
		}
		else {
			Logger.error(acceptor.position, "No viable operation for types " + lhs.friendlyName() + " and " + rhs.friendlyName());
		}
	}

	@Override
	public void visit(AbsDefs acceptor) {
        if (isRootNode) {
            isRootNode = false;

            for (TraversalStates state : TraversalStates.values()) {
                traversalState = state;

                for (AbsDef s : acceptor.definitions) {
                    s.accept(this);
                }
            }

            // TODO: - Issue with tests
//            for (AtomType atomType: Type.atomTypes) {
//                for (AbsType conformance : atomType.classDefinition.conformances) {
//                    if (!atomType.conformsTo((InterfaceType) symbolDescription.getTypeForAstNode(conformance))) {
//                        Logger.error(conformance.position, "Type \"" + atomType.friendlyName() + "\" does not conform to interface \"" + conformance.getName() + "\"");
//                    }
//                }
//            }
        }
        else {
            for (AbsDef s : acceptor.definitions) {
                s.accept(this);
            }
        }
	}

	@Override
	public void visit(AbsExprs acceptor) {
		if (acceptor.expressions.isEmpty()) {
            symbolDescription.setTypeForAstNode(acceptor, Type.voidType);
        }
		else {
			for (AbsExpr e : acceptor.expressions) {
                e.accept(this);
            }
		}
	}

	@Override
	public void visit(AbsForStmt acceptor) {
		acceptor.collection.accept(this);
		Type type = ((ArrayType) symbolDescription.getTypeForAstNode(acceptor.collection)).type;

		symbolDescription.setTypeForAstNode(symbolDescription.getDefinitionForAstNode(acceptor.iterator), type);
		symbolDescription.setTypeForAstNode(acceptor, Type.voidType);
		
		acceptor.iterator.accept(this);
		acceptor.body.accept(this);
	}

	@Override
	public void visit(AbsFunCall acceptor) {
		AbsFunDef definition = (AbsFunDef) symbolTable.findDefinitionForName(acceptor.getStringRepresentation());
		FunctionType funType = (FunctionType) symbolDescription.getTypeForAstNode(definition);

		symbolDescription.setTypeForAstNode(acceptor, funType.resultType);
		boolean isConstructor = definition.isConstructor;

		for (int i = 0; i < acceptor.numArgs(); i++) {
			AbsExpr arg = acceptor.arg(i);

            // skip first ("self") argument if function is constructor
            if (isConstructor && i == 0) {
                symbolDescription.setTypeForAstNode(arg, funType.resultType);
                continue;
            }

			arg.accept(this);

			Type argType = symbolDescription.getTypeForAstNode(arg);

            if (argType == null) return;

			if (!(funType.getParType(i).sameStructureAs(argType))) {
				Logger.error(arg.position, "Cannot assigningToVariable value of type \"" +
                        argType.friendlyName() + "\" to type \"" + funType.getParType(i).friendlyName() + "\"");
			}
		}
	}

	@Override
	public void visit(AbsFunDef acceptor) {
        Vector<Type> parameters = new Vector<>();

        for (AbsParDef par : acceptor.getParamaters()) {
            par.accept(this);
            parameters.add(symbolDescription.getTypeForAstNode(par));
        }

        acceptor.type.accept(this);
        Type returnType = symbolDescription.getTypeForAstNode(acceptor.type);

        FunctionType funType = new FunctionType(parameters, returnType, acceptor);
        symbolDescription.setTypeForAstNode(acceptor, funType);

        if (!resolveTypeOnly){
			// check if return type matches
			for (AbsStmt stmt : acceptor.func.statements) {
			    stmt.accept(this);

				if (stmt instanceof AbsReturnExpr) {
					Type t = symbolDescription.getTypeForAstNode(stmt);

					if (!t.sameStructureAs(funType.resultType)) {
                        Logger.error(stmt.position,
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
			
			if (symbolDescription.getTypeForAstNode(c.cond).sameStructureAs(Type.boolType))
				symbolDescription.setTypeForAstNode(acceptor, Type.voidType);
			else
				Logger.error(c.cond.position,
						"Condition must be of type Bool");
		}

		if (acceptor.elseBody != null) {
			acceptor.elseBody.accept(this);
		}
	}

	@Override
	public void visit(AbsParDef acceptor) {
		acceptor.type.accept(this);
		
		Type type = symbolDescription.getTypeForAstNode(acceptor.type);
		if (type.isCanType())
			type = ((CanType) type).childType;

		symbolDescription.setTypeForAstNode(acceptor, type);
	}

	@Override
	public void visit(AbsTypeName acceptor) {
        if (acceptor.name.equals("Int")) {
            symbolDescription.setTypeForAstNode(acceptor, Type.intType);
            return;
        }
        if (acceptor.name.equals("Double")) {
            symbolDescription.setTypeForAstNode(acceptor, Type.doubleType);
            return;
        }
        if (acceptor.name.equals("String")) {
            symbolDescription.setTypeForAstNode(acceptor, Type.stringType);
            return;
        }
        if (acceptor.name.equals("Char")) {
            symbolDescription.setTypeForAstNode(acceptor, Type.charType);
            return;
        }
        if (acceptor.name.equals("Void")) {
            symbolDescription.setTypeForAstNode(acceptor, Type.voidType);
            return;
        }

		AbsDef definition = symbolDescription.getDefinitionForAstNode(acceptor);
		
		if (!(definition instanceof AbsTypeDef))
			Logger.error(acceptor.position, "Use of undeclared type \'" + definition.name + "\'");

		Type type = symbolDescription.getTypeForAstNode(definition);

		if (type == null)
			Logger.error(acceptor.position, "Type \"" + acceptor.name
					+ "\" is undefined");

		symbolDescription.setTypeForAstNode(acceptor, type);
	}

	@Override
	public void visit(AbsUnExpr acceptor) {
		acceptor.expr.accept(this);
		Type type = symbolDescription.getTypeForAstNode(acceptor.expr);

		if (acceptor.oper == AbsUnExpr.NOT) {
			if (type.sameStructureAs(Type.boolType))
				symbolDescription.setTypeForAstNode(acceptor, Type.boolType);
			else
				Logger.error(acceptor.position,
						"Operator \"!\" is not defined for type " + type);
		} else if (acceptor.oper == AbsUnExpr.ADD || acceptor.oper == AbsUnExpr.SUB) {
			if (type.isBuiltinIntType() || type.canBeCastedToType(Type.intType))
				symbolDescription.setTypeForAstNode(acceptor, type);
			else
				Logger.error(acceptor.position,
						"Operators \"+\" and \"-\" are not defined for type "
								+ type);
		}
	}

	@Override
	public void visit(AbsVarDef acceptor) {
        if (acceptor.type != null) {
			acceptor.type.accept(this);

			Type type = symbolDescription.getTypeForAstNode(acceptor.type);
			if (type.isCanType())
				type = ((CanType) type).childType;
			
			symbolDescription.setTypeForAstNode(acceptor, type);
		}
	}

	@Override
	public void visit(AbsVarNameExpr acceptor) {
        if (acceptor.name.equals("Int")) {
            symbolDescription.setTypeForAstNode(acceptor, Type.intType.staticType);
            return;
        }
        if (acceptor.name.equals("Double")) {
            symbolDescription.setTypeForAstNode(acceptor, Type.doubleType.staticType);
            return;
        }
        if (acceptor.name.equals("String")) {
            symbolDescription.setTypeForAstNode(acceptor, Type.stringType.staticType);
            return;
        }
        if (acceptor.name.equals("Char")) {
            symbolDescription.setTypeForAstNode(acceptor, Type.charType.staticType);
            return;
        }
        if (acceptor.name.equals("Void")) {
            symbolDescription.setTypeForAstNode(acceptor, Type.voidType.staticType);
            return;
        }

        Type type = symbolDescription.getTypeForAstNode(symbolDescription.getDefinitionForAstNode(acceptor));

        if (!assigningToVariable && type.isOptionalType()) {
            OptionalType optionalType = (OptionalType) type;

            // implicitly force forced OptionalTyped
            if (optionalType.isForced) {
                type = optionalType.childType;
            }
        }

		symbolDescription.setTypeForAstNode(acceptor, type);
	}

	@Override
	public void visit(AbsWhileStmt acceptor) {
		acceptor.cond.accept(this);
		acceptor.body.accept(this);

		if (symbolDescription.getTypeForAstNode(acceptor.cond).sameStructureAs(Type.boolType)) {
            symbolDescription.setTypeForAstNode(acceptor, Type.voidType);
        }
		else {
            Logger.error(acceptor.cond.position, "Condition must be typed as Boolean");
        }
	}

	@Override
	public void visit(AbsImportDef importDef) {
	    if (traversalState == TraversalStates.extensions) {
            String currentFile = Logger.fileName;

            Logger.fileName = importDef.getName();

            importDef.imports.accept(new TypeChecker(symbolTable, symbolDescription));

            Logger.fileName = currentFile;
        }
	}

	@Override
	public void visit(AbsStmts stmts) {
	    if (isRootNode) {
	        isRootNode = false;

            for (TraversalStates state : TraversalStates.values()) {
                traversalState = state;

                for (AbsStmt s : stmts.statements) {
                    if (traversalState == TraversalStates.extensions && !(s instanceof AbsDef))
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

			symbolDescription.setTypeForAstNode(returnExpr, symbolDescription.getTypeForAstNode(returnExpr.expr));
		}
		else {
            symbolDescription.setTypeForAstNode(returnExpr, Type.voidType);
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

			Type t = symbolDescription.getTypeForAstNode(e);

			if (base == null) {
			    base = t;
            }
			else if (t.sameStructureAs(base) || t.canBeCastedToType(base)) {}
            else {
                if (base.canBeCastedToType(t)) {
                    base = t;
                }
                else {
                    base = Type.anyType;
                }
            }

			vec.add(symbolDescription.getTypeForAstNode(e));
		}

		symbolDescription.setTypeForAstNode(absListExpr, new ArrayType(base, vec.size()));
	}

	@Override
	public void visit(AbsFunType funType) {
		Vector<Type> parameters = new Vector<>();
		for (AbsType t : funType.parameterTypes) {
			t.accept(this);
			parameters.add(symbolDescription.getTypeForAstNode(t));
		}
		funType.returnType.accept(this);
		
		symbolDescription.setTypeForAstNode(funType, new FunctionType(parameters, symbolDescription.getTypeForAstNode(funType.returnType), null));
	}

	@Override
	public void visit(AbsControlTransferStmt acceptor) {
		///
	}

	@Override
	public void visit(AbsSwitchStmt switchStmt) {
		switchStmt.subjectExpr.accept(this);
		
		Type switchType = symbolDescription.getTypeForAstNode(switchStmt.subjectExpr);
		
		for (AbsCaseStmt singleCase : switchStmt.cases) {
			singleCase.accept(this);
			for (AbsExpr e : singleCase.exprs) {
				Type caseType = symbolDescription.getTypeForAstNode(e);
				if (!caseType.sameStructureAs(switchType))
					Logger.error(e.position,
							"Expression of type \"" + caseType.toString() + 
							"\" cannot match values of type \"" + switchType.toString() +"\"");
			}
		}
		
		if (switchStmt.defaultBody != null) {
            switchStmt.defaultBody.accept(this);
        }
		
		symbolDescription.setTypeForAstNode(switchStmt, Type.voidType);
	}

	@Override
	public void visit(AbsCaseStmt acceptor) {
		for (AbsExpr e : acceptor.exprs) {
            e.accept(this);
        }

        symbolTable.newScope();
		acceptor.body.accept(this);
        symbolTable.oldScope();
	}

	@Override
	public void visit(AbsEnumDef acceptor) {
        if (traversalState != TraversalStates.extensions) return;

        AtomType enumRawValueType = null;
		
		if (acceptor.type != null) {
			acceptor.type.accept(this);
			enumRawValueType = (AtomType) symbolDescription.getTypeForAstNode(acceptor.type);
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
						Logger.error(enumMemberDef.position,
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
				ClassType defType = (ClassType) symbolDescription.getTypeForAstNode(def);
				
				if (defType.containsMember("rawValue")) {
					Type rawValueType = defType.getMemberTypeForName("rawValue");
					
					if (enumRawValueType == null)
						Logger.error(enumMemberDef.value.position,
								"Enum member cannot have a raw value "
								+ "if the enum doesn't have a raw type");
					
					
					if (!rawValueType.sameStructureAs(enumRawValueType))
						Logger.error(enumMemberDef.value.position,
								"Cannot assigningToVariable value of type \"" +
								rawValueType.toString() + "\" to type \"" + 
								enumRawValueType.toString() + "\"");

					previousValue = enumMemberDef.value.value;
				}
			}

			types.add((ClassType) symbolDescription.getTypeForAstNode(def));
			names.add(def.getName());
		}

		symbolDescription.setTypeForAstNode(acceptor, new EnumType(acceptor, names, types));
	}

	@Override
	public void visit(AbsEnumMemberDef acceptor) {
        ArrayList<String> names = new ArrayList<>();
        ArrayList<Type> types = new ArrayList<>();
        ArrayList<AbsDef> definitions = new ArrayList<>();
		
		if (acceptor.value != null) {
			acceptor.value.accept(this);
			
			names.add("rawValue");
			types.add(symbolDescription.getTypeForAstNode(acceptor.value));
			definitions.add(new AbsVarDef(acceptor.position, "rawValue", new AbsAtomType(null, null)));
		}
		
		AbsClassDef classDef = new AbsClassDef(acceptor.getName(), acceptor.position, definitions, new ArrayList<>(), new ArrayList<>());
		ClassType type = new ClassType(classDef, names, types);
		symbolDescription.setTypeForAstNode(acceptor, type);
	}

	@Override
	public void visit(AbsTupleDef acceptor) {
        if (traversalState == TraversalStates.extensions) return;

        LinkedList<Type> types = new LinkedList<>();
		LinkedList<String> names = new LinkedList<>();
		
		for (AbsDef def : acceptor.definitions.definitions) {
			def.accept(this);
			
			names.add(def.getName());
			types.add(symbolDescription.getTypeForAstNode(def));
		}
		
		TupleType tupleType = new TupleType(acceptor, types, names);
		symbolDescription.setTypeForAstNode(acceptor, tupleType);
	}

	@Override
	public void visit(AbsLabeledExpr acceptor) {
		acceptor.expr.accept(this);
		
		Type exprType = symbolDescription.getTypeForAstNode(acceptor.expr);
		symbolDescription.setTypeForAstNode(acceptor, exprType);
	}

	@Override
	public void visit(AbsTupleExpr acceptor) {
		acceptor.expressions.accept(this);
		
		LinkedList<Type> types = new LinkedList<>();
		LinkedList<String> names = new LinkedList<>();

		for (AbsExpr e : acceptor.expressions.expressions) {
			AbsLabeledExpr labeledExpr = (AbsLabeledExpr) e;
			
			types.add(symbolDescription.getTypeForAstNode(labeledExpr));
			names.add(labeledExpr.name);
		}

		// TODO:
        Type type = types.size() == 1 ? new TupleType(types, names) : new TupleType(types, names);
		symbolDescription.setTypeForAstNode(acceptor, type);
	}

	@Override
	public void visit(AbsOptionalType acceptor) {
		acceptor.childType.accept(this);
		
		Type childType = symbolDescription.getTypeForAstNode(acceptor.childType);
		if (childType.isCanType()) {
		    childType = ((CanType) childType).childType;
        }

		symbolDescription.setTypeForAstNode(acceptor, new OptionalType(childType, acceptor.isForced));
	}

	@Override
	public void visit(AbsOptionalEvaluationExpr acceptor) {
		acceptor.subExpr.accept(this);

		Type childType = symbolDescription.getTypeForAstNode(acceptor.subExpr);
        if (childType.isCanType()) {
            childType = ((CanType) childType).childType;
        }

		symbolDescription.setTypeForAstNode(acceptor, new OptionalType(childType, false));
	}

	@Override
	public void visit(AbsForceValueExpr acceptor) {
		acceptor.subExpr.accept(this);

		Type type = symbolDescription.getTypeForAstNode(acceptor.subExpr);
		
		if (type.isOptionalType()) {
            symbolDescription.setTypeForAstNode(acceptor, ((OptionalType) type).childType);
        }
		else {
            Logger.error(acceptor.position,
                    "Cannot unwrap value of non-optional type '" + type.toString() + "'");
        }
	}

    @Override
    public void visit(AbsExtensionDef acceptor) {
	    if (traversalState == TraversalStates.extensions) {
            acceptor.extendingType.accept(this);

            for (AbsType conformance: acceptor.conformances) {
                conformance.accept(this);
            }

            Type type = symbolDescription.getTypeForAstNode(acceptor.extendingType);
            boolean isAtomType = false;

            if (type.isAtomType()) {
                AtomType atomType = (AtomType) type;
                type = atomType.staticType;
                isAtomType = true;
            }

            symbolDescription.setTypeForAstNode(acceptor, type);

            if (!type.isCanType() || !((CanType) type).childType.isObjectType()) {
                Logger.error(acceptor.position, "Only classes can be extended (for now)");
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
                Type memberType = symbolDescription.getTypeForAstNode(def);

                if (def.isStatic()) {
                    if (!((CanType) type).addStaticMember(def, memberName, memberType)) {
                        Logger.error(acceptor.position, "Invalid redeclaration of \"" + memberName + "\"");
                    }
                }
                else {
                    if (!extendingType.addMember(def, memberName, memberType)) {
                        Logger.error(acceptor.position, "Invalid redeclaration of \"" + memberName + "\"");
                    }
                }
            }

            for (AbsType conformance : acceptor.conformances) {
                if (!extendingType.addConformance(conformance)) {
                    Logger.error(conformance.position, "Redundant conformance \"" + conformance.getName() + "\"");
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

                        symbolDescription.setDefinitionForAstNode(selfParDef.type, acceptor);
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
        symbolDescription.setTypeForAstNode(acceptor, type);

        // TODO: - Bad design!!!!
        if (acceptor.getName().equals(Constants.any)) {
            Type.anyType = type;
        }
    }
}
