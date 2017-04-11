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

package compiler.synan;

import java.util.LinkedList;
import java.util.Vector;

import Utils.Constants;
import compiler.Position;
import compiler.Report;
import compiler.abstr.tree.*;
import compiler.abstr.tree.def.AbsClassDef;
import compiler.abstr.tree.def.AbsDef;
import compiler.abstr.tree.def.AbsEnumDef;
import compiler.abstr.tree.def.AbsEnumMemberDef;
import compiler.abstr.tree.def.AbsFunDef;
import compiler.abstr.tree.def.AbsImportDef;
import compiler.abstr.tree.def.AbsParDef;
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
import compiler.abstr.tree.stmt.AbsConditionalStmt;
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
import compiler.lexan.*;

/**
 * Syntax parser.
 * 
 * @author Toni Kocjan
 */
public class SynAn {

	/** Leksikalni analizator. */
	private LexAn lexAn;

	/** Ali se izpisujejo vmesni rezultati. */
	private boolean dump;

	/** Current & previous symbol */
	private Symbol symbol = null;
	private Symbol previous = null;

	public boolean parseStandardLibrary = false;

	/**
	 * Ustvari nov sintaksni analizator.
	 * 
	 * @param lexAn
	 *            Leksikalni analizator.
	 * @param dump
	 *            Ali se izpisujejo vmesni rezultati.
	 */
	public SynAn(LexAn lexAn, boolean dump) {
		this.lexAn = lexAn;
		this.dump = dump;

		this.symbol = this.lexAn.lexAn();
		this.previous = this.symbol;
	}

	/**
	 * Opravi sintaksno analizo.
	 */
	public AbsTree parse() {
		if (symbol == null)
			Report.error("Error accessing LexAn");
		
		if (symbol.token == TokenType.NEWLINE)
			skip();

		dump("source -> statements");
		AbsTree abstrTree = parseStatements();

		if (symbol.token != TokenType.EOF && symbol.token != TokenType.NEWLINE)
			Report.error(symbol.position, "Syntax error on token \""
					+ previous.lexeme + "\"");

		return abstrTree;
	}

	private AbsStmts parseStatements() {
		dump("statements -> statement statements'");
		
		LinkedList<AbsStmt> absStmts = new LinkedList<>();
		if (symbol.token == TokenType.RBRACE)
			return new AbsStmts(symbol.position, absStmts);
		
		AbsStmt statement = parseStatement();

        absStmts.add(statement);
		absStmts.addAll(parseStatements_(statement));
		
		return new AbsStmts(new Position(absStmts.getFirst().position,
				absStmts.getLast().position), absStmts);
	}

	private Vector<AbsStmt> parseStatements_(AbsStmt prevStmt) {
		switch (symbol.token) {
		case EOF:
			dump("statements' -> $");
			return new Vector<>();
		case RBRACE:
			dump("statements' -> e");
			return new Vector<>();
		case SEMIC:
			skip();
			if (symbol.token == TokenType.NEWLINE)
				skip();
			
			if (symbol.token == TokenType.EOF || symbol.token == TokenType.RBRACE)
				return new Vector<AbsStmt>();
		case IDENTIFIER:
			AbsStmt statement = parseStatement();
			Vector<AbsStmt> absStmts = parseStatements_(statement);
			absStmts.add(0, statement);
			return absStmts;
		case NEWLINE:
			skip();
			
			if (symbol.token == TokenType.EOF || symbol.token == TokenType.RBRACE || 
				symbol.token == TokenType.KW_CASE || symbol.token == TokenType.KW_DEFAULT)
				return new Vector<AbsStmt>();

			statement = parseStatement();
			absStmts = parseStatements_(statement);
			absStmts.add(0, statement);
			return absStmts;
		case ASSIGN:
			if (!(prevStmt instanceof AbsVarDef))
				Report.error(prevStmt.position, "Syntax error");
			
			skip();
			dump("var_definition -> = expression");
			
			AbsVarDef var = (AbsVarDef) prevStmt;
			AbsVarNameExpr varName = new AbsVarNameExpr(var.position, var.name);

			AbsExpr e = parseExpression();
			absStmts = parseStatements_(null);
			
			absStmts.add(0, new AbsBinExpr(new Position(var.position, e.position), 
					AbsBinExpr.ASSIGN, varName, e));
			
			return absStmts;
		default:
			Report.error(symbol.position, "Consecutive statements must be separated by separator");
		}
		return null;
	}

	private AbsStmt parseStatement() {
		switch (symbol.token) {
		/**
		 * Parse definition.
		 */
		case KW_VAR:
		case KW_LET:
		case KW_STRUCT:
		case KW_CLASS:
		case KW_ENUM:
		case KW_FUN:
		case KW_IMPORT:
		case LPARENT:
			dump("statement -> definition");
			return parseDefinition();
			
		/**
		 * Parse control transfer statement.
		 */
		case KW_CONTINUE:
			dump("expression -> continue");
			skip();
			return new AbsControlTransferStmt(symbol.position, 
					ControlTransferKind.Continue);
		case KW_BREAK:
			dump("expression -> break");
			skip();
			return new AbsControlTransferStmt(symbol.position,
					ControlTransferKind.Break);
		
		/**
		 * Parse conditional statements
		 */
		case KW_IF:
			dump("atom_expression -> if_statement");
			return parseIf();
		case KW_SWITCH:
			dump("atom_expression -> switch_statement");
			return parseSwitch();
		case KW_WHILE:
			dump("atom_expression -> while expression { expression }");
			return parseWhileLoop();
		case KW_FOR:
			dump("atom_expression -> for indentifier in expression { expression }");
			return parseForLoop();
		
		/**
		 * Parse expression.
		 */
		default:
			dump("statement -> expression");
			return parseExpression();
		}
	}

//	private AbsDefs parseDefinitions() {
//		dump("definitions -> definition definitions'");
//		AbsDef definition = parseDefinition();
//
//		Vector<AbsDef> absDefs = parseDefinitions_();
//		absDefs.add(0, definition);
//		return new AbsDefs(new Position(absDefs.firstElement().position,
//				absDefs.lastElement().position), absDefs);
//	}

//	private Vector<AbsDef> parseDefinitions_() {
//		switch (symbol.token) {
//		case EOF:
//			dump("definitions' -> $");
//
//			return new Vector<>();
//		case RBRACE:
//			dump("definitions' -> e");
//			skip();
//
//			return new Vector<>();
//		case SEMIC:
//			dump("definitions' -> ; definitions");
//			skip();
//			if (symbol.token == Token.NEWLINE)
//				skip();
//
//			if (symbol.token == Token.EOF || symbol.token == Token.RBRACE)
//				return new Vector<>();
//
//			AbsDef definition = parseDefinition();
//			Vector<AbsDef> absDefs = parseDefinitions_();
//			absDefs.add(0, definition);
//			return absDefs;
//		case NEWLINE:
//			dump("definitions' -> \n definitions");
//			skip();
//
//			if (symbol.token == Token.EOF || symbol.token == Token.RBRACE)
//				return new Vector<>();
//
//			definition = parseDefinition();
//			absDefs = parseDefinitions_();
//			absDefs.add(0, definition);
//			return absDefs;
//		default:
//			Report.error(symbol.position, "Syntax error on token \""
//					+ previous.lexeme
//					+ "\", expected \";\" or \"}\" after this token");
//		}
//		return null;
//	}

	private AbsDef parseDefinition() {
		AbsDef definition = null;
		AccessControl accessControl = AccessControl.Public;

        boolean isOverriding = false;

        if (symbol.token == TokenType.KW_OVERRIDE) {
            isOverriding = true;
            skip();
        }

        if (symbol.token == TokenType.KW_PUBLIC)
			skip();
		else if (symbol.token == TokenType.KW_PRIVATE) {
			accessControl = AccessControl.Private;
			skip();
		}

		switch (symbol.token) {
		case KW_FUN:
            dump("definition -> function_definition");
            definition = parseFunDefinition();
            break;
        case KW_INIT:
            dump("definition -> constructor_definition");
            definition = parseConstructorDefinition();
			break;
		case KW_VAR:
		case KW_LET:
			dump("definition -> variable_definition");
			definition = parseVarDefinition(); 
			break;
		case KW_IMPORT:
			dump("definition -> import_definition");
			definition = parseImportDefinition();
			break;
		case KW_CLASS:
			dump("definition -> class_definition");
			definition = parseClassDefinition();
			break;
		case KW_ENUM:
			dump("definition -> enum_definition");
			definition = parseEnumDefinition();
			break;
		case KW_CASE:
			dump("definition -> enum_member_definition");
			definition = parseEnumCaseDefinition();
			break;
		default:
			if (symbol.token != TokenType.EOF)
				Report.error(symbol.position, "Syntax error on token \""
						+ symbol.lexeme + "\", delete this token");
			else
				Report.error(previous.position, "Syntax error on token \""
						+ previous.lexeme + "\", delete this token");
		}

		definition.setAccessControl(accessControl);
		definition.setOverriding(isOverriding);

		return definition;
	}

	private AbsFunDef parseFunDefinition() {
		Position startPos = symbol.position;

        if (symbol.token == TokenType.KW_FUN) {
			Symbol functionName = skip(new Symbol(TokenType.IDENTIFIER, "identifier", null));

			skip(new Symbol(TokenType.LPARENT, "(", null));
			skip();
			dump("function_definition -> func identifier ( parameters ) function_definition'");

			LinkedList<AbsParDef> params = parseParameters();

			return parseFunDefinition_(startPos, functionName, params);
		}
		Report.error(previous.position, "Syntax error on token \""
				+ previous.lexeme + "\", expected keyword \"fun\"");

		return null;
	}

    private AbsFunDef parseConstructorDefinition() {
        Position startPos = symbol.position;

        if (symbol.token == TokenType.KW_INIT) {
            Symbol functionName = symbol;

            skip(new Symbol(TokenType.LPARENT, "(", null));
            skip();
            dump("constructor_definition -> init ( parameters ) function_definition'");

            LinkedList<AbsParDef> params = parseParameters();

            return parseFunDefinition_(startPos, functionName, params);
        }

        Report.error(previous.position, "Syntax error on token \""
                + previous.lexeme + "\", expected keyword \"init\"");

        return null;
    }

	private AbsFunDef parseFunDefinition_(Position startPos, Symbol functionName, LinkedList<AbsParDef> params) {
		AbsType type;

		if (symbol.token == TokenType.LBRACE) {
			dump("function_definition' -> { statements } ");
			type = new AbsAtomType(symbol.position, AtomTypeKind.VOID);
		}
		else {
			dump("function_definition' -> type { statements } ");
			type = parseType();
		}

		if (symbol.token != TokenType.LBRACE)
			Report.error(symbol.position, "Syntax error on token \""
					+ previous.lexeme + "\", expected \"{\" after this token");

		skip(new Symbol(TokenType.NEWLINE, "NEWLINE", null));
		skip();
		
		AbsStmts expr = parseStatements();
		if (symbol.token != TokenType.RBRACE)
			Report.error(symbol.position, "Syntax error on token \""
					+ previous.lexeme + "\", expected \"}\" after this token");
		skip();

		return new AbsFunDef(new Position(startPos, expr.position), functionName.lexeme, params, type, expr);
	}

	private AbsDef parseVarDefinition() {
		Position startPos = symbol.position;
		boolean isMutable = true;
		Symbol id = null;
		
		if (symbol.token == TokenType.KW_VAR) {
            id = skip(new Symbol(TokenType.IDENTIFIER, "identifier", null));

            isMutable = true;
        }
		else {
			id = skip(new Symbol(TokenType.IDENTIFIER, "identifier", null));

            isMutable = false;
		}
		
		skip();
		
		AbsType type = null;
		
		if (symbol.token == TokenType.ASSIGN) {
			dump("var_definition -> var identifier = expr");
			return new AbsVarDef(startPos, id.lexeme, type, isMutable);
		}
		else if (symbol.token != TokenType.COLON) {
            Report.error(previous.position, "Syntax error on token \""
                    + previous.lexeme + "\", expected \":\"");
        }
		
		skip();

		dump("var_definition -> var identifier : type");

		type = parseType();
		return new AbsVarDef(new Position(startPos, type.position), 
				id.lexeme, type, isMutable);
	}

	private AbsImportDef parseImportDefinition() {
		Position pos = symbol.position;
		skip(new Symbol(TokenType.IDENTIFIER, "IDENTIFIER", null));

		String file = symbol.lexeme;
		skip();

		if (symbol.token == TokenType.DOT) {
			skip();
			return parseImportDefinition_(new AbsImportDef(pos, file));
		}
		else {
            return new AbsImportDef(pos, file);
        }
	}

	private AbsImportDef parseImportDefinition_(AbsImportDef def) {
		switch (symbol.token) {
		case IDENTIFIER:
			def.definitions.add(symbol.lexeme);
			skip();
			return parseImportDefinition__(def);
		default:
			Report.error(symbol.position,
					"Syntax error, expected \"IDENTIFIER\"");
			return null;
		}
	}

	private AbsImportDef parseImportDefinition__(AbsImportDef def) {
		switch (symbol.token) {
		case COMMA:
			skip();
			def.definitions.add(symbol.lexeme);
			skip();
			return parseImportDefinition__(def);
		default:
			return def;
		}
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private AbsClassDef parseClassDefinition() {
		Position start = symbol.position;
		String className = skip(new Symbol(TokenType.IDENTIFIER, "IDENTIFIER", null)).lexeme;
		skip();

		AbsType baseClass = null;
		if (symbol.token == TokenType.COLON) {
		    skip();

		    // parse base class type name
            baseClass = parseType();
        }

        if (symbol.token != TokenType.LBRACE) {
		    Report.error(symbol.position, "Expected \"{\"");
        }
		
		skip();
		
		LinkedList[] data = parseClassMemberDefinitions();
		LinkedList<AbsDef> definitions = data[0];
        LinkedList<AbsStmt> defaultConstructor = data[1];
        LinkedList<AbsFunDef> constructors = data[2];

		if (symbol.token != TokenType.RBRACE) {
            Report.error(symbol.position, "Syntax error on token \"" + symbol.lexeme + "\", expected \"}\"");
        }

		Position end = symbol.position;
		Position definitionPosition = new Position(start, end);

		skip();
		
		return new AbsClassDef(className, definitionPosition, baseClass, definitions, defaultConstructor, constructors);
	}
	
	@SuppressWarnings("rawtypes")
	private LinkedList[] parseClassMemberDefinitions() {
		LinkedList<AbsDef> definitions = new LinkedList<>();
        LinkedList<AbsFunDef> constructors = new LinkedList<>();
		LinkedList<AbsStmt> defaultConstructor = new LinkedList<>();

        if (symbol.token == TokenType.RBRACE) {
            return new LinkedList[]{definitions, defaultConstructor, constructors};
        }

        if (symbol.token != TokenType.NEWLINE) {
            Report.error(symbol.position, "Invalid token");
        }
        skip();
		
		while (true) {
            AbsDef definition;

            switch (symbol.token) {
                case KW_PUBLIC:
                case KW_PRIVATE:
                case KW_VAR:
                case KW_LET:
                case KW_OVERRIDE:
                    definition = parseDefinition();
                    definitions.add(definition);

                    if (symbol.token == TokenType.ASSIGN) {
                        skip();
                        dump("var_definition -> = expression");

                        AbsVarNameExpr varNameExpr = new AbsVarNameExpr(definition.position, ((AbsVarDef) definition).name);
                        AbsExpr valueExpr = parseExpression();
                        AbsBinExpr dotExpr = new AbsBinExpr(
                                new Position(definition.position, valueExpr.position), AbsBinExpr.DOT,
                                new AbsVarNameExpr(definition.position, Constants.selfParameterIdentifier), varNameExpr);
                        AbsBinExpr assignExpr = new AbsBinExpr(definition.position, AbsBinExpr.ASSIGN, dotExpr, valueExpr);

                        defaultConstructor.add(assignExpr);
                    }
                    break;
                case KW_FUN:
                    AbsFunDef funDef = (AbsFunDef) parseDefinition();
                    definitions.add(funDef);
                    break;
                case KW_INIT:
                    funDef = (AbsFunDef) parseDefinition();
                    funDef.isConstructor = true;

                    constructors.add(funDef);
                    break;
                case RBRACE:
                    return new LinkedList[]{definitions, defaultConstructor, constructors};
                case NEWLINE:
                case SEMIC:
                    skip();
                    continue;
                default:
                    Report.error(symbol.position, "Consecutive statements must be separated by a separator");
            }
        }
	}
	
	private AbsEnumDef parseEnumDefinition() {
		Position start = symbol.position;
		String name = skip(new Symbol(TokenType.IDENTIFIER, "IDENTIFIER", null)).lexeme;
		AbsType type = null;
		
		skip();
		if (symbol.token == TokenType.COLON) {
			skip();
			type = parseType();
		}
		
		if (symbol.token != TokenType.LBRACE)
			Report.error(symbol.position, "Enum must begin with \"{\"");
		
		skip(new Symbol(TokenType.NEWLINE, "\n", null));
		skip(new Symbol(TokenType.KW_CASE, "CASE", null));
		
		LinkedList<AbsDef> enumDefinitions = parseEnumMemberDefinitions();
		if (symbol.token != TokenType.RBRACE)
			Report.error(symbol.position, "Syntax error on token \""
					+ symbol.lexeme + "\", expected \"}\"");
		skip();
		
		Position end = enumDefinitions.getLast().position;
		return new AbsEnumDef(new Position(start, end), name, enumDefinitions, 
				(AbsAtomType) type);
	}
	
	private LinkedList<AbsDef> parseEnumMemberDefinitions() {
		LinkedList<AbsDef> definitions = new LinkedList<>();
		
		while (true) {
			AbsDef definition = parseDefinition();
			definitions.add(definition);
			
			if (symbol.token == TokenType.COMMA) {
				while (symbol.token == TokenType.COMMA) {
					skip();
					if (symbol.token != TokenType.IDENTIFIER)
						Report.error(symbol.position, "Expected idenfitifer "
								+ "after comma in enum member definition");
					definitions.add(parseEnumCaseDefinition());
				}
			}
			
			if (symbol.token == TokenType.NEWLINE) {
				skip();
				if (symbol.token == TokenType.RBRACE)
					break;

				if (symbol.token == TokenType.IDENTIFIER)
					Report.error(symbol.position, "Enum member definition "
							+ "must begin with \"case\" keyword");
			}
		}

		if (symbol.token != TokenType.RBRACE)
			Report.error("todo");
		
		return definitions;
	}
	
	private AbsEnumMemberDef parseEnumCaseDefinition() {
		if (symbol.token == TokenType.KW_CASE)
			skip(new Symbol(TokenType.IDENTIFIER, "identifier", null));
		AbsVarNameExpr name = new AbsVarNameExpr(symbol.position, symbol.lexeme);
		
		skip();
		if (symbol.token == TokenType.ASSIGN) {
			skip();
			
			AbsExpr value = parseExpression();
			if (!(value instanceof AbsAtomConstExpr))
				Report.error(value.position, "Raw value for enum definition must be literal");
			
			Position definitionPos = new Position(name.position, value.position);
			return new AbsEnumMemberDef(definitionPos, name, (AbsAtomConstExpr) value);
		}
		return new AbsEnumMemberDef(name.position, name, null);
	}
	
	private AbsType parseType() {
		AbsType type = parseChildType();
		
		if (symbol.token == TokenType.QMARK || symbol.token == TokenType.NOT) {
		    boolean isForced = symbol.token == TokenType.NOT;

			Position pos = new Position(type.position, symbol.position);
			skip();
			
			return new AbsOptionalType(pos, type, isForced);
		}
		
		return type;
	}

	private AbsType parseChildType() {
		Symbol s = symbol;

		switch (symbol.token) {
		case IDENTIFIER:
			dump("type -> identifier");
			skip();

			return new AbsTypeName(s.position, s.lexeme);
		case BOOL:
			dump("type -> logical");
			skip();

			return new AbsAtomType(s.position, AtomTypeKind.LOG);
		case INTEGER:
			dump("type -> integer");
			skip();

			return new AbsAtomType(s.position, AtomTypeKind.INT);
		case STRING:
			dump("type -> string");
			skip();

			return new AbsAtomType(s.position, AtomTypeKind.STR);
		case CHAR:
			dump("type -> char");
			skip();

			return new AbsAtomType(s.position, AtomTypeKind.CHR);
		case DOUBLE:
			dump("type -> double");
			skip();

			return new AbsAtomType(s.position, AtomTypeKind.DOB);
		case VOID:
			dump("type -> void");
			skip();

			return new AbsAtomType(s.position, AtomTypeKind.VOID);
		case LBRACKET:
			skip();
			dump("type -> [ type ]");
			AbsType type = parseType();

			if (symbol.token != TokenType.RBRACKET)
				Report.error(symbol.position, "Syntax error, insert \"]\"");

			skip();
			return new AbsListType(new Position(s.position, type.position), 0,
					type);
		case LPARENT:
			Position start = symbol.position;
			dump("type -> (parameters) -> type");
			skip();
			
			Vector<AbsType> parameters = new Vector<>();
			if (symbol.token != TokenType.RPARENT) {
				while (true) {
					parameters.add(parseType());
					if (symbol.token != TokenType.COMMA)
						break;
					skip();
				}
			}
			if (symbol.token != TokenType.RPARENT)
				Report.error(symbol.position,
						"Syntax error, insert \")\" to complete function declaration");
			skip(new Symbol(TokenType.ARROW, "->", null));
			skip();
			
			type = parseType();
			return new AbsFunType(new Position(start, type.position), 
					parameters, type);
		default:
			Report.error(symbol.position, "Syntax error on token \""
					+ symbol.lexeme + "\", expected \"variable type\"");
		}

		return null;
	}

	private LinkedList<AbsParDef> parseParameters() {
		if (symbol.token == TokenType.RPARENT) {
			skip();
			return new LinkedList<>();
		}

		dump("parameters -> parameter parameters'");

		AbsParDef paramater = parseParameter();
		LinkedList<AbsParDef> params = new LinkedList<>();
		params.add(paramater);
		params.addAll(parseParameters_());

		return params;
	}

	private Vector<AbsParDef> parseParameters_() {
		if (symbol.token == TokenType.COMMA) {
			dump("parameters' -> parameters");
			skip();

			AbsParDef parameter = parseParameter();
			Vector<AbsParDef> params = new Vector<>();
			params.add(parameter);
			params.addAll(parseParameters_());
			return params;
		} else if (symbol.token != TokenType.RPARENT)
			Report.error(symbol.position,
					"Syntax error, insert \")\" to complete function declaration");

		dump("parameters' -> e");
		skip();

		return new Vector<>();
	}

	private AbsParDef parseParameter() {
		if (symbol.token == TokenType.IDENTIFIER) {
			Symbol argumentLabel = symbol;
			String parameterName = null;

			skip();

			if (symbol.token == TokenType.IDENTIFIER) {
                parameterName = symbol.lexeme;
                skip();
            }
			else if (symbol.token != TokenType.COLON)
                Report.error(symbol.position,
                        "Syntax error, expected paramater definition");
			skip();

			dump("parameter -> identifier : type");

			AbsType type = parseType();
			return new AbsParDef(new Position(argumentLabel.position, type.position),
					parameterName, argumentLabel.lexeme, type);
		}

		Report.error(symbol.position,
				"Syntax error, expected paramater definition");

		return null;
	}

	private Vector<AbsExpr> parseExpressions() {
		AbsExpr e = null;

		switch (symbol.token) {
		case ADD:
		case SUB:
		case NOT:
		case AND:
		case MUL:
		case LOG_CONST:
		case CHAR_CONST:
		case DOUBLE_CONST:
		case INT_CONST:
		case STR_CONST:
		case LBRACE:
		case LPARENT:
		case LBRACKET:
		case IDENTIFIER:
		case KW_RETURN:
			dump("expressions -> expression expression'");
			e = parseExpression();

			Vector<AbsExpr> expressions = new Vector<>();
			expressions.add(e);
			expressions.addAll(parseExpressions_());

			return expressions;
		default:
			Report.error(symbol.position, "Syntax error on token \""
					+ symbol.lexeme + "\", delete this token");
		}

		return null;
	}

	private Vector<AbsExpr> parseExpressions_() {
		switch (symbol.token) {
		case COMMA:
			dump("expressions' -> , expression expression'");
			skip();

			AbsExpr e = parseExpression();

			Vector<AbsExpr> expressions = new Vector<>();
			expressions.add(e);
			expressions.addAll(parseExpressions_());

			return expressions;
		case RPARENT:
			dump("expressions' -> e");
			skip();
			break;
		default:
			Report.error(symbol.position, "Syntax error on token \""
					+ previous.lexeme
					+ "\", expected \",\" or \")\" to end expression");
		}
		return new Vector<>();
	}

	private AbsExpr parseExpression() {
		switch (symbol.token) {
		case ADD:
		case SUB:
		case NOT:
		case LOG_CONST:
		case INT_CONST:
		case STR_CONST:
		case CHAR_CONST:
		case DOUBLE_CONST:
		case KW_NIL:
		case LBRACKET:
		case LPARENT:
		case KW_IF:
		case KW_WHILE:
		case KW_FOR:
		case IDENTIFIER:
		case KW_RETURN:
			dump("expression -> logical_ior_expression");
			return parseExpression_(parseIorExpression());
		default:
			Report.error(symbol.position, "Syntax error on token \""
					+ symbol.lexeme + "\", delete this token");
		}

		return null;
	}

	private AbsExpr parseExpression_(AbsExpr e) {
		switch (symbol.token) {
		case SEMIC:
		case COLON:
		case NEWLINE:
		case RPARENT:
		case RBRACE:
		case RBRACKET:
		case KW_ELSE:
		case LBRACE:
		case IDENTIFIER:
		case KW_FOR:
		case COMMA:
		case EOF:
			dump("expression' -> e");
			return e;
		case ASSIGN:
			skip();
			AbsExpr e2 = parseExpression();
			return new AbsBinExpr(new Position(e.position, e2.position), AbsBinExpr.ASSIGN, e, e2);
		default:
			Report.error(symbol.position, "Syntax error on token \""
					+ symbol.lexeme + "\", delete this token");
		}

		return null;
	}

	private AbsExpr parseIorExpression() {
		switch (symbol.token) {
		case ADD:
		case SUB:
		case NOT:
		case LOG_CONST:
		case INT_CONST:
		case STR_CONST:
		case CHAR_CONST:
		case DOUBLE_CONST:
		case KW_NIL:
		case LBRACKET:
		case KW_IF:
		case KW_WHILE:
		case KW_FOR:
		case LPARENT:
		case IDENTIFIER:
		case KW_RETURN:
			dump("logical_ior_expression -> logical_and_expression logical_ior_expression'");

			return parseIorExpression_(parseAndExpression());
		default:
			Report.error(symbol.position, "Syntax error on token \""
					+ symbol.lexeme + "\", delete this token");
		}

		return null;
	}

	private AbsExpr parseIorExpression_(AbsExpr e) {
		switch (symbol.token) {
		case IOR:
			dump("logical_ior_expression' -> | log_ior_expression");
			skip();
			AbsExpr expr = parseAndExpression();
			return parseIorExpression_(new AbsBinExpr(new Position(e.position,
					expr.position), AbsBinExpr.IOR, e, expr));
		case SEMIC:
		case COLON:
		case NEWLINE:
		case RPARENT:
		case ASSIGN:
		case IDENTIFIER:
		case RBRACE:
		case LBRACE:
		case RBRACKET:
		case KW_ELSE:
		case KW_FOR:
		case COMMA:
		case EOF:
			dump("logical_ior_expression' -> e");
			return e;
		default:
			Report.error(symbol.position, "Syntax error on token \""
					+ symbol.lexeme + "\", delete this token");
		}

		return null;
	}

	private AbsExpr parseAndExpression() {
		switch (symbol.token) {
		case ADD:
		case SUB:
		case NOT:
		case LOG_CONST:
		case INT_CONST:
		case STR_CONST:
		case CHAR_CONST:
		case DOUBLE_CONST:
		case KW_NIL:
		case LBRACKET:
		case KW_IF:
		case KW_WHILE:
		case KW_FOR:
		case LPARENT:
		case IDENTIFIER:
		case KW_RETURN:
			dump("logical_and_expression -> logical_compare_expression logical_and_expression'");

			return parseAndExpression_(parseCmpExpression());
		default:
			Report.error(symbol.position, "Syntax error on token \""
					+ symbol.lexeme + "\", delete this token");
		}

		return null;
	}

	private AbsExpr parseAndExpression_(AbsExpr e) {
		switch (symbol.token) {
            case LOG_CONST:
                break;
            case INT_CONST:
                break;
            case STR_CONST:
                break;
            case DOUBLE_CONST:
                break;
            case CHAR_CONST:
                break;
            case AND:
			dump("logical_and_expression' -> & logical_and_expression");
			skip();

			AbsExpr expr = parseCmpExpression();
			return parseAndExpression_(new AbsBinExpr(new Position(e.position,
					expr.position), AbsBinExpr.AND, e, expr));
            case NOT:
                break;
            case EQU:
                break;
            case KW_IS:
                break;
            case NEQ:
                break;
            case LTH:
                break;
            case GTH:
                break;
            case LEQ:
                break;
            case GEQ:
                break;
            case MUL:
                break;
            case DIV:
                break;
            case MOD:
                break;
            case ADD:
                break;
            case SUB:
                break;
            case LPARENT:
                break;
            case LBRACKET:
                break;
            case IOR:
		case SEMIC:
		case NEWLINE:
		case COLON:
		case RPARENT:
		case ASSIGN:
		case IDENTIFIER:
		case RBRACE:
		case LBRACE:
		case RBRACKET:
		case KW_ELSE:
		case KW_FOR:
		case COMMA:
		case EOF:
			dump("logical_and_expression' -> e");
			return e;
            case DOT:
                break;
            case ARROW:
                break;
            case QMARK:
                break;
            case EMARK:
                break;
            case INTEGER:
                break;
            case STRING:
                break;
            case DOUBLE:
                break;
            case BOOL:
                break;
            case CHAR:
                break;
            case VOID:
                break;
            case KW_FUN:
                break;
            case KW_IF:
                break;
            case KW_VAR:
                break;
            case KW_WHILE:
                break;
            case KW_STRUCT:
                break;
            case KW_IMPORT:
                break;
            case KW_LET:
                break;
            case KW_NIL:
                break;
            case KW_CLASS:
                break;
            case KW_IN:
                break;
            case KW_RETURN:
                break;
            case KW_PUBLIC:
                break;
            case KW_PRIVATE:
                break;
            case KW_CONTINUE:
                break;
            case KW_BREAK:
                break;
            case KW_SWITCH:
                break;
            case KW_CASE:
                break;
            case KW_DEFAULT:
                break;
            case KW_ENUM:
                break;
            case KW_INIT:
                break;
            default:
			Report.error(symbol.position, "Syntax error on token \""
					+ symbol.lexeme + "\", delete this token");
		}

		return null;
	}

	private AbsExpr parseCmpExpression() {
		switch (symbol.token) {
		case ADD:
		case SUB:
		case NOT:
		case LOG_CONST:
		case INT_CONST:
		case STR_CONST:
		case CHAR_CONST:
		case DOUBLE_CONST:
		case KW_NIL:
		case LBRACKET:
		case KW_IF:
		case KW_WHILE:
		case KW_FOR:
		case LPARENT:
		case IDENTIFIER:
		case KW_RETURN:
			dump("compare_expression -> add_expression compare_expression'");

			return parseCmpExpression_(parseAddExpression());
		default:
			Report.error(symbol.position, "Syntax error on token \""
					+ symbol.lexeme + "\", delete this token");
		}

		return null;
	}

	private AbsExpr parseCmpExpression_(AbsExpr e) {
		AbsExpr expr = null;
		int oper = -1;

		switch (symbol.token) {
		case AND:
		case IOR:
		case SEMIC:
		case NEWLINE:
		case COLON:
		case RPARENT:
		case ASSIGN:
		case IDENTIFIER:
		case RBRACE:
		case LBRACE:
		case RBRACKET:
		case KW_ELSE:
		case KW_FOR:
		case COMMA:
		case EOF:
			dump("compare_expression' -> e");
			return e;
		case EQU:
			dump("compare_expression' -> == compare_expression");
			skip();

			expr = parseAddExpression();
			oper = AbsBinExpr.EQU;
			break;
        case KW_IS:
            dump("compare_expression' -> is compare_expression");
            skip();

            expr = parseAddExpression();
            oper = AbsBinExpr.IS;
            break;
		case NEQ:
			dump("compare_expression' -> != compare_expression");
			skip();

			expr = parseAddExpression();
			oper = AbsBinExpr.NEQ;
			break;
		case GTH:
			dump("compare_expression' -> > compare_expression");
			skip();

			expr = parseAddExpression();
			oper = AbsBinExpr.GTH;
			break;
		case LTH:
			dump("compare_expression' -> < compare_expression");
			skip();

			expr = parseAddExpression();
			oper = AbsBinExpr.LTH;
			break;
		case GEQ:
			dump("compare_expression' -> >= compare_expression");
			skip();

			expr = parseAddExpression();
			oper = AbsBinExpr.GEQ;
			break;
		case LEQ:
			dump("compare_expression' -> <= compare_expression");
			skip();

			expr = parseAddExpression();
			oper = AbsBinExpr.LEQ;
			break;
		default:
			Report.error(symbol.position, "Syntax error on token \""
					+ symbol.lexeme + "\", delete this token");
		}

		return new AbsBinExpr(new Position(e.position, expr.position), oper, e,
				expr);
	}

	private AbsExpr parseAddExpression() {
		switch (symbol.token) {
		case ADD:
		case SUB:
		case NOT:
		case LOG_CONST:
		case INT_CONST:
		case STR_CONST:
		case CHAR_CONST:
		case DOUBLE_CONST:
		case KW_NIL:
		case LBRACKET:
		case KW_IF:
		case KW_WHILE:
		case KW_FOR:
		case LPARENT:
		case IDENTIFIER:
		case KW_RETURN:
			dump("add_expression -> multiplicative_expression add_expression'");

			return parseAddExpression_(parseMulExpression());
		default:
			Report.error(symbol.position, "Syntax error on token \""
					+ symbol.lexeme + "\", delete this token");
		}

		return null;
	}

	private AbsExpr parseAddExpression_(AbsExpr e) {
		AbsExpr expr = null;

		switch (symbol.token) {
		case AND:
		case IOR:
		case SEMIC:
		case COLON:
		case NEWLINE:
		case RPARENT:
		case ASSIGN:
		case IDENTIFIER:
		case RBRACE:
		case LBRACE:
		case RBRACKET:
		case KW_ELSE:
		case COMMA:
		case EOF:
		case EQU:
        case KW_IS:
		case NEQ:
		case GTH:
		case LTH:
		case GEQ:
		case LEQ:
		case KW_FOR:
			dump("add_expression' -> e");
			return e;
		case ADD:
			dump("add_expression' -> multiplicative_expression add_expresison'");
			skip();

			expr = parseMulExpression();
			return parseAddExpression_(new AbsBinExpr(new Position(e.position,
					expr.position), AbsBinExpr.ADD, e, expr));
		case SUB:
			dump("add_expression' -> - add_expression");
			skip();

			expr = parseMulExpression();
			return parseAddExpression_(new AbsBinExpr(e.position,
					AbsBinExpr.SUB, e, expr));
		default:
			Report.error(symbol.position, "Syntax error on parseAddExpression_");
		}

		return null;
	}

	private AbsExpr parseMulExpression() {
		switch (symbol.token) {
		case ADD:
		case SUB:
		case NOT:
		case LOG_CONST:
		case INT_CONST:
		case STR_CONST:
		case CHAR_CONST:
		case DOUBLE_CONST:
		case KW_NIL:
		case LBRACKET:
		case KW_IF:
		case KW_WHILE:
		case KW_FOR:
		case LPARENT:
		case IDENTIFIER:
		case KW_RETURN:
			dump("multiplicative_expression -> prefix_expression multiplicative_expression'");

			return parseMulExpression_(parsePrefixExpression());
		default:
			Report.error(symbol.position, "Syntax error on token \""
					+ previous.lexeme + "\", expected prefix expression");
		}

		return null;
	}

	private AbsExpr parseMulExpression_(AbsExpr e) {
		AbsExpr expr = null;
		int oper = -1;

		switch (symbol.token) {
		case AND:
		case IOR:
		case SEMIC:
		case NEWLINE:
		case COLON:
		case RPARENT:
		case ASSIGN:
		case RBRACE:
		case LBRACE:
		case IDENTIFIER:
		case RBRACKET:
		case KW_ELSE:
		case COMMA:
		case EOF:
		case EQU:
        case KW_IS:
		case NEQ:
		case GTH:
		case LTH:
		case GEQ:
		case LEQ:
		case ADD:
		case SUB:
		case KW_FOR:
			dump("multiplicative_expression' -> e");
			return e;
		case MUL:
			oper = AbsBinExpr.MUL;
			dump("multiplicative_expression' -> prefix_expression multiplicative_expression'");
			skip();
			expr = parsePrefixExpression();
			break;
		case DIV:
			oper = AbsBinExpr.DIV;
			dump("multiplicative_expression' -> prefix_expression multiplicative_expression'");
			skip();
			expr = parsePrefixExpression();
			break;
		case MOD:
			oper = AbsBinExpr.MOD;
			dump("multiplicative_expression' -> prefix_expression multiplicative_expression'");
			skip();
			expr = parsePrefixExpression();
			break;
		case DOT:
			oper = AbsBinExpr.DOT;
			dump("multiplicative_expression' -> prefix_expression multiplicative_expression'");
			skip();
			expr = parsePrefixExpression();

			break;
		default:
			Report.error(symbol.position, "Syntax error on token \""
					+ symbol.lexeme + "\", delete this token");
		}

		expr = new AbsBinExpr(new Position(e.position, expr.position), oper, e, expr);

        if (expr instanceof AbsBinExpr) {
            AbsBinExpr binExpr = (AbsBinExpr) expr;

            // TODO: - Fix this (object.data[index] is not parsed in the right way at the moment
            // so this is a temporary solution
            if (binExpr.oper == AbsBinExpr.DOT && binExpr.expr2 instanceof AbsBinExpr) {
                AbsBinExpr binExpr2 = (AbsBinExpr) binExpr.expr2;

                if (binExpr2.oper == AbsBinExpr.ARR) {
                    expr = new AbsBinExpr(binExpr.position, AbsBinExpr.ARR,
                            new AbsBinExpr(binExpr.expr1.position, AbsBinExpr.DOT, binExpr.expr1, binExpr2.expr1), binExpr2.expr2);
                }
            }
        }


        return parseMulExpression_(expr);
	}

	private AbsExpr parsePrefixExpression() {
		AbsExpr e = null;
		Symbol op = symbol;

		switch (symbol.token) {
		case ADD:
			dump("prefix_expression -> + prefix_expression");
			skip();

			e = parsePrefixExpression();
			return new AbsUnExpr(new Position(op.position, e.position),
					AbsUnExpr.ADD, e);
		case SUB:
			dump("prefix_expression -> - prefix_expression");
			skip();

			e = parsePrefixExpression();
			return new AbsUnExpr(new Position(op.position, e.position),
					AbsUnExpr.SUB, e);
		case NOT:
			dump("prefix_expression -> ! prefix_expression");
			skip();

			e = parsePrefixExpression();
			return new AbsUnExpr(new Position(op.position, e.position),
					AbsUnExpr.NOT, e);
		case AND:
			dump("prefix_expression -> & prefix_expression");
			skip();

			e = parsePrefixExpression();
			return new AbsUnExpr(new Position(op.position, e.position),
					AbsUnExpr.MEM, e);
		case MUL:
			dump("prefix_expression -> * prefix_expression");
			skip();

			e = parsePrefixExpression();
			return new AbsUnExpr(new Position(op.position, e.position),
					AbsUnExpr.VAL, e);
		case LOG_CONST:
		case INT_CONST:
		case STR_CONST:
		case CHAR_CONST:
		case DOUBLE_CONST:
		case KW_NIL:
		case LBRACKET:
		case KW_IF:
		case KW_WHILE:
		case KW_FOR:
		case LPARENT:
		case IDENTIFIER:
		case KW_RETURN:
			dump("prefix_expression -> postfix_expression");
			return parsePostfixExpression();
		default:
			Report.error(symbol.position, "Syntax error on token \""
					+ symbol.lexeme + "\", delete this token");
		}

		return null;
	}

	private AbsExpr parsePostfixExpression() {
		switch (symbol.token) {
		case LOG_CONST:
		case INT_CONST:
		case STR_CONST:
		case CHAR_CONST:
		case DOUBLE_CONST:
		case KW_NIL:
		case LBRACKET:
		case KW_IF:
		case KW_WHILE:
		case KW_FOR:
		case LPARENT:
		case IDENTIFIER:
		case KW_RETURN:
		case QMARK:
		case EMARK:
			dump("postfix_expression -> atom_expression postfix_expression'");

			return parsePostfixExpression_(parseAtomExpression());
		default:
			Report.error(symbol.position, "Syntax error on token \""
					+ symbol.lexeme + "\", delete this token");
		}

		return null;
	}

	private AbsExpr parsePostfixExpression_(AbsExpr e) {
		Position symbolPos = symbol.position;
		
		switch (symbol.token) {
		case AND:
		case IOR:
		case SEMIC:
		case NEWLINE:
		case COLON:
		case RPARENT:
		case IDENTIFIER:
		case ASSIGN:
		case RBRACE:
		case LBRACE:
		case RBRACKET:
		case KW_ELSE:
		case COMMA:
		case EOF:
        case EQU:
        case KW_IS:
		case NEQ:
		case GTH:
		case LTH:
		case GEQ:
		case LEQ:
		case ADD:
		case SUB:
		case MUL:
		case DIV:
		case MOD:
		case DOT:
		case KW_FOR:
			dump("postfix_expression' -> e");
			return e;
		case LBRACKET:
			dump("postfix_expression' -> [ expression ] postfix_expression'");
			skip();
			AbsExpr expr = parseExpression();
			if (symbol.token != TokenType.RBRACKET)
				Report.error(previous.position,
						"Syntax error, insert \"]\" to complete expression");
			skip();

			return parsePostfixExpression_(new AbsBinExpr(new Position(
					e.position, expr.position), AbsBinExpr.ARR, e, expr));
		case QMARK:
			dump("postfix_expression' -> optional evaluation expression");
			skip();
			return new AbsOptionalEvaluationExpr(new Position(e.position, symbolPos), e);
		case NOT:
			dump("postfix_expression' -> force value expression");
			skip();
			return new AbsForceValueExpr(
					new Position(e.position, symbolPos), e);
		default:
			Report.error(symbol.position, "Syntax error on token \""
					+ symbol.lexeme + "\", delete this token");
		}

		return null;
	}

	private AbsExpr parseAtomExpression() {
		Symbol current = symbol;

		switch (symbol.token) {
		case LOG_CONST:
			dump("atom_expression -> log_const");
			skip();

			return new AbsAtomConstExpr(current.position, AtomTypeKind.LOG,
					current.lexeme);
		case INT_CONST:
			dump("atom_expression -> int_const");
			skip();

			return new AbsAtomConstExpr(current.position, AtomTypeKind.INT,
					current.lexeme);
		case STR_CONST:
			dump("atom_expression -> str_const");
			skip();

			return new AbsAtomConstExpr(current.position, AtomTypeKind.STR,
					current.lexeme);
		case CHAR_CONST:
			dump("atom_expression -> char_const");
			skip();

			return new AbsAtomConstExpr(current.position, AtomTypeKind.CHR,
					current.lexeme);
		case DOUBLE_CONST:
			dump("atom_expression -> double_const");
			skip();

			return new AbsAtomConstExpr(current.position, AtomTypeKind.DOB,
					current.lexeme);
		case KW_NIL:
			dump("atom_expression -> nil");
			skip();

			return new AbsAtomConstExpr(current.position, AtomTypeKind.NIL,
					current.lexeme);
		case IDENTIFIER:
			skip();
			if (symbol.token == TokenType.LPARENT) {
				skip();
				
				if (symbol.token == TokenType.RPARENT) {
					dump("atom_expression -> identifier ( )");
					skip();
					return new AbsFunCall(symbol.position, current.lexeme, new Vector<>());
				}

				dump("atom_expression -> identifier ( expressions )");

				// TODO: - Optimize this
				// FIXME: - Function arguments should be parsed with their own method
				LinkedList<AbsExpr> arguments = parseTupleExpressions(true);

				Vector<AbsLabeledExpr> absExprs = new Vector<>();
				for (AbsExpr e : arguments)
					absExprs.add((AbsLabeledExpr) e);

				if (symbol.token != TokenType.RPARENT)
					Report.error(symbol.position, "Expected ')'");
				skip();
				
				return new AbsFunCall(new Position(current.position, absExprs.lastElement().position), current.lexeme, absExprs);
			} else {
				dump("atom_expression -> identifier");
				return new AbsVarNameExpr(current.position, current.lexeme);
			}
		case LBRACKET:
			return parseBracket();
		case KW_RETURN:
			Position pos = symbol.position;
			skip();
			// FIXME
			if (symbol.token == TokenType.SEMIC || symbol.token == TokenType.NEWLINE) {
				dump("atom_expression -> return");
				return new AbsReturnExpr(pos, null);
			}
			dump("atom_expression -> return expression");
			AbsExpr e = parseExpression();
			return new AbsReturnExpr(new Position(pos, e.position), e);
		case LPARENT:
			return parseTupleExpression(false);
		default:
			Report.error("Syntax error on token \"" + symbol.lexeme + "\", delete this token");
		}
		return null;
	}

	private AbsConditionalStmt parseForLoop() {
		if (symbol.token == TokenType.KW_FOR) {
			Position start = symbol.position;
			Symbol count = skip(new Symbol(TokenType.IDENTIFIER, "identifier", null));
			skip();
			
			if (symbol.token != TokenType.KW_IN)
				Report.error(previous.position, "Syntax error on token \""
						+ previous.lexeme
						+ "\", expected keyword \"in\" after this token");
			skip();

			AbsExpr e = parseExpression();
			if (symbol.token != TokenType.LBRACE)
				Report.error(previous.position, "Syntax error on token \""
						+ previous.lexeme
						+ "\", expected \"{\" after this token");
			skip(new Symbol(TokenType.NEWLINE, "NEWLINE", null));
			skip();
			
			AbsStmts s = parseStatements();

			if (symbol.token != TokenType.RBRACE)
				Report.error(previous.position, "Syntax error on token \""
						+ previous.lexeme
						+ "\", expected \"}\" after this token");
			skip();

			return new AbsForStmt(new Position(start, s.position), new AbsVarNameExpr(
					count.position, count.lexeme), e, s);
		}
		Report.error(symbol.position, "Syntax error, expected keyword \"for\"");

		return null;
	}

	private AbsConditionalStmt parseWhileLoop() {
		if (symbol.token == TokenType.KW_WHILE) {
			Position start = symbol.position;
			skip();
			AbsExpr e1 = parseExpression();
			if (symbol.token == TokenType.LBRACE) {
				skip(new Symbol(TokenType.NEWLINE, "NEWLINE", null));
				skip();
				AbsStmts s = parseStatements();

				if (symbol.token != TokenType.RBRACE)
					Report.error(symbol.position, "Syntax error on token \""
							+ previous.lexeme
							+ "\", expected '}' after this token");
				skip();

				return new AbsWhileStmt(new Position(start, s.position), e1, s);
			}
			Report.error(previous.position, "Syntax error on token \""
					+ previous.lexeme + "\", expected \"{\" after this token");
		}
		Report.error(previous.position,
				"Syntax error, expected keyword \"while\"");

		return null;
	}

	private Condition parseIfCondition() {
		dump("if_expression -> if epression { statements }");

		skip();
		AbsExpr condition = parseExpression();
		if (symbol.token != TokenType.LBRACE)
			Report.error(symbol.position, "Syntax error on token \""
					+ previous.lexeme + "\", expected '{' after this token");
		skip(new Symbol(TokenType.NEWLINE, "NEWLINE", null));
		skip();
		AbsStmts s = parseStatements();
		if (symbol.token != TokenType.RBRACE)
			Report.error(symbol.position, "Syntax error on token \""
					+ previous.lexeme + "\", expected '}' after this token");
		skip();
		return new Condition(condition, s);
	}
	
	private AbsIfStmt parseIf() {
		if (symbol.token == TokenType.KW_IF) {
			Position start = symbol.position;
			return parseIf_(start, parseIfCondition());
		}
		Report.error(previous.position,
				"Syntax error, expected keyword \"while\"");
		return null;
	}

	private AbsIfStmt parseIf_(Position start, Condition condition) {
		if (symbol.token == TokenType.NEWLINE) 
			skip();
		
		Vector<Condition> conditions = new Vector<>();
		conditions.add(condition);
		AbsStmts elseBody = null;
		
		while (true) {
			if (symbol.token == TokenType.KW_ELSE) {
				skip();
				
				if (symbol.token == TokenType.KW_IF) {
					dump("if_expression' -> else if { statements }");
					conditions.add(parseIfCondition());
					
					if (symbol.token == TokenType.NEWLINE) 
						skip();
					continue;
				}

				if (symbol.token != TokenType.LBRACE)
					Report.error(symbol.position, "Syntax error on token \""
							+ previous.lexeme + "\", expected '{' after this token");
				
				dump("if_expression' -> else { statements }");

				skip(new Symbol(TokenType.NEWLINE, "NEWLINE", null));
				skip();

				elseBody = parseStatements();
				skip();
			}
			break;
		}

		Position lastPos = elseBody != null ? 
				elseBody.position : conditions.lastElement().body.position;
		return new AbsIfStmt(new Position(condition.cond.position, lastPos), 
				conditions, elseBody);
	}
	
	private AbsSwitchStmt parseSwitch() {
		if (symbol.token != TokenType.KW_SWITCH)
			Report.error(symbol.position,
					"Syntax error, expected keyword \"switch\"");
		
		Position start = symbol.position;
		skip();

		AbsExpr subjectExpr = parseExpression();
		skip(new Symbol(TokenType.NEWLINE, "newline", null));
		skip();
		
		if (symbol.token != TokenType.KW_CASE)
			Report.error(symbol.position, "Syntax error, \"switch\" must be followed by at least one \"case\" statement");
		
		Vector<AbsCaseStmt> cases = new Vector<>();
		AbsStmts defaultBody = null;
		
		while (symbol.token == TokenType.KW_CASE)
			cases.add(parseCase());
		
		if (symbol.token == TokenType.KW_DEFAULT) {
			skip(new Symbol(TokenType.COLON, ":", null));
			skip(new Symbol(TokenType.NEWLINE, "newline", null));
			skip();
			defaultBody = parseStatements();
		}

		if (symbol.token != TokenType.RBRACE)
			Report.error(symbol.position,
					"Syntax error, expected \"}\"");
		skip();
		
		Position switchPos = new Position(start, 
				defaultBody != null ? defaultBody.position : cases.lastElement().position);
		
		return new AbsSwitchStmt(switchPos, subjectExpr, cases, defaultBody);
	}
	
	private AbsCaseStmt parseCase() {
		if (symbol.token != TokenType.KW_CASE)
			Report.error(symbol.position,
					"Syntax error, expected keyword \"case\"");
		
		Position start = symbol.position;
		skip();
		
		LinkedList<AbsExpr> expressions = new LinkedList<>();
		expressions.add(parseExpression());
		
		// join conditions
		while (symbol.token == TokenType.COMMA) {
			skip();
			expressions.add(parseExpression());
		}

		if (symbol.token != TokenType.COLON)
			Report.error(symbol.position,
					"Syntax error, case expression must be followed by \":\"");
		skip();
		
		if (symbol.token == TokenType.NEWLINE)
			skip();
		
		if (symbol.token == TokenType.KW_CASE || symbol.token == TokenType.KW_DEFAULT)
			Report.error(symbol.position, "Case should have at least one executable statement");
		
		AbsStmts body = parseStatements();
		Position casePos = new Position(start, body.position);
		
		return new AbsCaseStmt(casePos, expressions, body);
	}
	
	private AbsExpr parseBracket() {
		dump("atom_expression -> []");

		Position start = symbol.position;
		skip();
		
		if (symbol.token == TokenType.RBRACKET) {
			skip();
			return new AbsListExpr(start, new Vector<AbsExpr>());
		}
		
		AbsExpr e1 = parseExpression();
		
//		if (symbol.token == Token.KW_FOR) {
//			dump("[] -> [ expression for identifier in expression ]");
//			Vector<AbsStmt> stmt = new Vector<>();
//			stmt.add(e1);
//			AbsStmts s = new AbsStmts(new Position(start, e1.position), stmt);
//			
//			if (symbol.token != Token.KW_FOR)
//				Report.error(previous.position, "Syntax error on token \""
//						+ previous.lexeme
//						+ "\", expected keyword \"for\" after this token");
//			
//			Symbol count = skip(new Symbol(Token.IDENTIFIER, "identifier", null));
//			AbsVarNameExpr var = new AbsVarNameExpr(count.position, count.lexeme);
//			skip();
//			
//			if (symbol.token != Token.KW_IN)
//				Report.error(previous.position, "Syntax error on token \""
//						+ previous.lexeme
//						+ "\", expected keyword \"in\" after this token");
//			skip();
//			
//			AbsExpr e2 = parseExpression();
//			
//			if (symbol.token != Token.RBRACKET)
//				Report.error(previous.position, "Syntax error on token \""
//						+ previous.lexeme
//						+ "\", expected \"]\" after this token");
//			skip();
//			
//			return new AbsForStmt(new Position(start, e2.position), var, e2, s);
//		}
		
		/*else */if (symbol.token == TokenType.COMMA) {
			dump("[] -> [expression, expressions']");
			Vector<AbsExpr> elements = new Vector<>();
			elements.add(e1);
			while (symbol.token == TokenType.COMMA) {
				skip();
				elements.add(parseExpression());
			}
			if (symbol.token != TokenType.RBRACKET)
				Report.error(previous.position, "Syntax error on token \""
						+ previous.lexeme
						+ "\", expected \"]\" after this token");
			skip();
			return new AbsListExpr(new Position(elements.firstElement().position, 
					elements.lastElement().position), elements);
		}
		else if (symbol.token == TokenType.RBRACKET) {
			dump("[] -> [expression]");
			Vector<AbsExpr> elements = new Vector<>();
			elements.add(e1);
			skip();
			return new AbsListExpr(new Position(elements.firstElement().position, 
					elements.lastElement().position), elements);
		}
		
		return null;
	}
	
	
	private AbsTupleExpr parseTupleExpression(boolean argumentTuple) {
		Position start = symbol.position;
		skip();

		LinkedList<AbsExpr> expressions = parseTupleExpressions(argumentTuple);
		
		if (symbol.token != TokenType.RPARENT)
			Report.error(symbol.position, "Expected ')'");
		
		Position tuplePos = new Position(start, symbol.position);
		skip();
		
		return new AbsTupleExpr(tuplePos, expressions);
	}
	
	private LinkedList<AbsExpr> parseTupleExpressions(boolean argumentTuple) {
		int index = 0;
		LinkedList<AbsExpr> expressions = new LinkedList<>(); 
		
		while (true) {
			AbsExpr e1 = parseExpression();
			
			if (symbol.token == TokenType.COLON) {
				if (!(e1 instanceof AbsVarNameExpr))
					Report.error(e1.position, "Expected identifier for tuple member name");
				
				String memberName = ((AbsVarNameExpr) e1).name;
				
				// TODO
//				if (names.contains(memberName))
//					Report.error(e1.position, "This tuple already contains member named \"" + memberName + "\"");
				
				skip();
				AbsExpr e2 = parseExpression();
				Position pos = new Position(e1.position, e2.position);
				expressions.add(new AbsLabeledExpr(pos, e2, memberName));
			}
			else {
				String memberName;

				if (argumentTuple)
					memberName = "_";
				else
					memberName = String.valueOf(index);

				expressions.add(new AbsLabeledExpr(e1.position, e1, memberName));
			}
			
			if (symbol.token == TokenType.RPARENT)
				break;
			if (symbol.token != TokenType.COMMA)
				Report.error(symbol.position, "Insert ',' separator");
			skip();
			if (symbol.token == TokenType.NEWLINE)
				skip();
			
			index++;
		}
		
		return expressions;
	}

	/**
	 * Get next symbol from lexan.
	 */
	private Symbol skip() {
		previous = symbol;
		symbol = lexAn.lexAn();
		return symbol;
	}

	/**
	 * Get next symbol from lexan.
	 * 
	 * @param expected
	 *            symbol which we expect
	 * @return next symbol
	 */
	private Symbol skip(Symbol expected) {
		if (skip().token != expected.token)
			Report.error(previous.position, "Syntax error on token \""
					+ previous.lexeme + "\", expected \"" + expected.lexeme
					+ "\" after this token");
		return symbol;
	}

	/**
	 * Izpise produkcijo v datoteko z vmesnimi rezultati.
	 * 
	 * @param production
	 *            Produkcija, ki naj bo izpisana.
	 */
	private void dump(String production) {
		if (!dump)
			return;
		if (Report.dumpFile() == null)
			return;
		Report.dumpFile().println(production);
	}
}
