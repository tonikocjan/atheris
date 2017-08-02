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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Vector;

import utils.Constants;
import compiler.Position;
import compiler.Report;
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
import compiler.ast.tree.stmt.AbsConditionalStmt;
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

		this.symbol = this.lexAn.nextSymbol();
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

        ArrayList<AbsStmt> absStmts = new ArrayList<>();
		if (symbol.token == TokenType.RBRACE)
			return new AbsStmts(symbol.position, absStmts);

		AbsStmt statement = parseStatement();

        absStmts.add(statement);
		absStmts.addAll(parseStatements_(statement));

		return new AbsStmts(new Position(absStmts.get(0).position, absStmts.get(absStmts.size() - 1).position), absStmts);
	}

	private ArrayList<AbsStmt> parseStatements_(AbsStmt prevStmt) {
		switch (symbol.token) {
		case EOF:
			dump("statements' -> $");
			return new ArrayList<>();
		case RBRACE:
			dump("statements' -> e");
			return new ArrayList<>();
		case SEMIC:
			skip();
			if (symbol.token == TokenType.NEWLINE)
				skip();

			if (symbol.token == TokenType.EOF || symbol.token == TokenType.RBRACE)
				return new ArrayList<>();
		case IDENTIFIER:
			AbsStmt statement = parseStatement();
            ArrayList<AbsStmt> absStmts = parseStatements_(statement);
			absStmts.add(0, statement);
			return absStmts;
		case NEWLINE:
			skip();

			if (symbol.token == TokenType.EOF || symbol.token == TokenType.RBRACE ||
				symbol.token == TokenType.KW_CASE || symbol.token == TokenType.KW_DEFAULT)
				return new ArrayList<>();

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
        case KW_INTERFACE:
		case KW_FUN:
		case KW_IMPORT:
        case KW_EXTENSION:
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

	private AbsDefs parseDefinitions() {
		dump("definitions -> definition definitions'");

        if (symbol.token == TokenType.NEWLINE) {
            skip();
        }

        if (symbol.token == TokenType.RBRACE) {
            return new AbsDefs(symbol.position, new ArrayList<>());
        }

        AbsDef definition = parseDefinition();

        ArrayList<AbsDef> absDefs = parseDefinitions_();
		absDefs.add(0, definition);
		return new AbsDefs(new Position(absDefs.get(0).position, absDefs.get(absDefs.size() - 1).position), absDefs);
	}

	private ArrayList<AbsDef> parseDefinitions_() {
		switch (symbol.token) {
		case EOF:
			dump("definitions' -> $");

			return new ArrayList<>();
		case RBRACE:
			dump("definitions' -> e");
			skip();

			return new ArrayList<>();
		case SEMIC:
        case NEWLINE:
			dump("definitions' -> ; definitions");
			skip();
			if (symbol.token == TokenType.NEWLINE)
				skip();

			if (symbol.token == TokenType.EOF || symbol.token == TokenType.RBRACE)
				return new ArrayList<>();

			AbsDef definition = parseDefinition();
            ArrayList<AbsDef> absDefs = parseDefinitions_();
			absDefs.add(0, definition);
			return absDefs;
		default:
			Report.error(symbol.position, "Syntax error on token \""
					+ previous.lexeme
					+ "\", expected \";\" or \"}\" after this token");
		}
		return null;
	}

	private AbsDef parseDefinition() {
		AbsDef definition = null;

		HashSet<Modifier> modifiers = parseModifiers();

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
        case KW_STRUCT:
			dump("definition -> class_definition");
			definition = parseClassDefinition(symbol.token == TokenType.KW_STRUCT);
			break;
        case KW_INTERFACE:
            dump("definition -> interface_definition");
            definition = parseInterfaceDefinition();
            break;
		case KW_ENUM:
			dump("definition -> enum_definition");
			definition = parseEnumDefinition();
			break;
        case KW_EXTENSION:
            dump("definition -> extension_definition");
            definition = parseExtensionDefinition();
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

		definition.setModifiers(modifiers);

		return definition;
	}

	private HashSet<Modifier> parseModifiers() {
	    HashSet<Modifier> modifiers = new HashSet<>();

	    Modifier modifier = parseModifier();

        while (modifier != Modifier.none) {
	        if (modifiers.contains(modifier)) {
	            Report.error(symbol.position, "Duplicate modifier");
            }

            modifiers.add(modifier);
            modifier = parseModifier();
        }

	    return modifiers;
    }

    private Modifier parseModifier() {
	    if (symbol.token == TokenType.KW_STATIC) {
	        skip();
	        return Modifier.isStatic;
        }
        if (symbol.token == TokenType.KW_FINAL) {
            skip();
            return Modifier.isFinal;
        }
        if (symbol.token == TokenType.KW_OVERRIDE) {
            skip();
            return Modifier.isOverriding;
        }
        if (symbol.token == TokenType.KW_PUBLIC) {
            skip();
            return Modifier.isPublic;
        }
        if (symbol.token == TokenType.KW_PRIVATE) {
            skip();
            return Modifier.isPrivate;
        }

        return Modifier.none;
    }

	private AbsFunDef parseFunDefinition() {
		Position startPos = symbol.position;

        if (symbol.token == TokenType.KW_FUN) {
			Symbol functionName = skip(new Symbol(TokenType.IDENTIFIER, "identifier", null));

			skip(new Symbol(TokenType.LPARENT, "(", null));
			skip();
			dump("function_definition -> func identifier ( parameters ) function_definition'");

            ArrayList<AbsParDef> params = parseParameters();

			return parseFunDefinition_(startPos, functionName, params, false);
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

            ArrayList<AbsParDef> params = parseParameters();

            return parseFunDefinition_(startPos, functionName, params, true);
        }

        Report.error(previous.position, "Syntax error on token \""
                + previous.lexeme + "\", expected keyword \"init\"");

        return null;
    }

	private AbsFunDef parseFunDefinition_(Position startPos, Symbol functionName, ArrayList<AbsParDef> params, boolean isConstructor) {
		AbsType type;

		if (symbol.token == TokenType.LBRACE) {
			dump("function_definition' -> { statements } ");
			type = new AbsAtomType(symbol.position, AtomTypeKind.VOID);
		}
		else {
			dump("function_definition' -> type { statements } ");
			type = parseType();
		}

		if (symbol.token != TokenType.LBRACE) {
            Report.error(symbol.position, "Syntax error on token \""
                    + previous.lexeme + "\", expected \"{\" after this token");
        }

		skip(new Symbol(TokenType.NEWLINE, "NEWLINE", null));
		skip();

		AbsStmts expr = parseStatements();
		if (symbol.token != TokenType.RBRACE)
			Report.error(symbol.position, "Syntax error on token \""
					+ previous.lexeme + "\", expected \"}\" after this token");
		skip();

		return new AbsFunDef(new Position(startPos, expr.position), functionName.lexeme, params, type, expr, isConstructor);
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
	private AbsClassDef parseClassDefinition(boolean parseStructure) {
		Position start = symbol.position;
		skip();

		if (!symbol.isIdentifier()) {
		    Report.error(symbol.position, "Expected identifier");
        }

		String className = symbol.lexeme;
		skip();

		AbsType baseClass = null;

        // parse conformances
        ArrayList<AbsType> conformances = parseConformances();

        if (conformances.size() > 0) {
            baseClass = conformances.get(0);
            conformances.remove(0);
        }

        if (symbol.token != TokenType.LBRACE) {
		    Report.error(symbol.position, "Expected \"{\"");
        }

		skip();

        ArrayList[] data = parseClassMemberDefinitions();
        ArrayList<AbsDef> definitions = data[0];
        ArrayList<AbsStmt> defaultConstructor = data[1];
        ArrayList<AbsFunDef> constructors = data[2];

		if (symbol.token != TokenType.RBRACE) {
            Report.error(symbol.position, "Syntax error on token \"" + symbol.lexeme + "\", expected \"}\"");
        }

		Position end = symbol.position;
		Position definitionPosition = new Position(start, end);

		skip();

        if (parseStructure) {
            return new AbsStructDef(className, definitionPosition, baseClass, definitions, defaultConstructor, constructors);
        }

        return new AbsClassDef(className, definitionPosition, baseClass, conformances, definitions, defaultConstructor, constructors);
	}

	@SuppressWarnings("rawtypes")
	private ArrayList[] parseClassMemberDefinitions() {
        ArrayList<AbsDef> definitions = new ArrayList<>();
        ArrayList<AbsFunDef> constructors = new ArrayList<>();
        ArrayList<AbsStmt> defaultConstructor = new ArrayList<>();

        if (symbol.token == TokenType.RBRACE) {
            return new ArrayList[]{definitions, defaultConstructor, constructors};
        }

        if (symbol.token != TokenType.NEWLINE) {
            Report.error(symbol.position, "Invalid token");
        }
        skip();

		while (true) {
            if (symbol.token == TokenType.RBRACE) {
                return new ArrayList[] { definitions, defaultConstructor, constructors };
            }

            AbsDef definition = parseDefinition();

            if (definition instanceof AbsFunDef && ((AbsFunDef) definition).isConstructor) {
                constructors.add((AbsFunDef) definition);
            }
            else {
                definitions.add(definition);

                if (definition instanceof AbsVarDef) {
                    if (symbol.token == TokenType.ASSIGN) {
                        skip();
                        dump("var_definition -> = expression");

                        AbsVarNameExpr varNameExpr = new AbsVarNameExpr(definition.position, ((AbsVarDef) definition).name);
                        AbsExpr valueExpr = parseExpression();
                        AbsBinExpr dotExpr = new AbsBinExpr(
                                new Position(definition.position, valueExpr.position), AbsBinExpr.DOT,
                                new AbsVarNameExpr(definition.position, Constants.selfParameterIdentifier), varNameExpr);
                        AbsBinExpr assignExpr = new AbsBinExpr(definition.position, AbsBinExpr.ASSIGN, dotExpr, valueExpr);

                        if (!definition.isStatic()) {
                            defaultConstructor.add(assignExpr);
                        }
                    }
                }
            }


            if (symbol.token == TokenType.RBRACE) {
                return new ArrayList[] { definitions, defaultConstructor, constructors };
            }

            if (symbol.token == TokenType.NEWLINE || symbol.token == TokenType.SEMIC) {
                skip();
                continue;
            }

            Report.error(symbol.position, "Consecutive statements must be separated by a separator");
        }
	}

	private ArrayList<AbsType> parseConformances() {
        ArrayList<AbsType> conformances = new ArrayList<>();

        if (symbol.token == TokenType.COLON) {
            do {
                skip();
                conformances.add(parseType());
            }
            while (symbol.token == TokenType.COMMA);
        }

        return conformances;
    }

	private AbsInterfaceDef parseInterfaceDefinition() {
        Position start = symbol.position;
        String interfaceName = skip(new Symbol(TokenType.IDENTIFIER, "IDENTIFIER", null)).lexeme;
        skip();

        if (symbol.token != TokenType.LBRACE) {
            Report.error(symbol.position, "Expected \"{\"");
        }

        skip();

        if (symbol.token == TokenType.NEWLINE) {
            skip();
        }

        ArrayList<AbsDef> defs = parseInterfaceDefinitions();
        Position positition = start;

        if (defs.size() > 0) {
            positition = new Position(start, defs.get(defs.size() - 1).position);
        }

        if (symbol.token != TokenType.RBRACE) {
            Report.error(symbol.position, "Expected \"}\"");
        }

        skip();

        return new AbsInterfaceDef(positition, interfaceName, new AbsDefs(positition, defs));
    }

    private ArrayList<AbsDef> parseInterfaceDefinitions() {
        ArrayList<AbsDef> defs = new ArrayList<>();

        if (symbol.token == TokenType.NEWLINE) {
            skip();
        }
        while (symbol.token == TokenType.KW_FUN) {
            defs.add(parseFunctionPrototype());

            if (symbol.token == TokenType.SEMIC) {
                skip();
            }
            if (symbol.token == TokenType.NEWLINE) {
                skip();
            }
        }

	    return defs;
    }

    private AbsFunDef parseFunctionPrototype() {
        Position startPos = symbol.position;

        if (symbol.token == TokenType.KW_FUN) {
            Symbol functionName = skip(new Symbol(TokenType.IDENTIFIER, "identifier", null));

            skip(new Symbol(TokenType.LPARENT, "(", null));
            skip();
            dump("function_prototype -> func identifier ( parameters ) function_prototype'");

            ArrayList<AbsParDef> params = parseParameters();

            AbsType type;

            if (symbol.token == TokenType.NEWLINE || symbol.token == TokenType.SEMIC) {
                dump("function_prototype' -> $ ");
                skip();
                type = new AbsAtomType(symbol.position, AtomTypeKind.VOID);
            }
            else {
                dump("function_prototype' -> type ");
                type = parseType();
            }

            return new AbsFunDef(new Position(startPos, type.position), functionName.lexeme, params, type, new AbsStmts(startPos, new ArrayList<>()));
        }
        Report.error(previous.position, "Syntax error on token \"" + previous.lexeme + "\", expected keyword \"fun\"");

        return null;
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

        ArrayList<AbsDef> enumDefinitions = parseEnumMemberDefinitions();
		if (symbol.token != TokenType.RBRACE)
			Report.error(symbol.position, "Syntax error on token \""
					+ symbol.lexeme + "\", expected \"}\"");
		skip();

		Position end = enumDefinitions.get(enumDefinitions.size() - 1).position;
		return new AbsEnumDef(new Position(start, end), name, enumDefinitions, (AbsAtomType) type);
	}

	private ArrayList<AbsDef> parseEnumMemberDefinitions() {
        ArrayList<AbsDef> definitions = new ArrayList<>();

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

	private AbsExtensionDef parseExtensionDefinition() {
	    skip();
        if (!symbol.isIdentifier()) {
            Report.error(symbol.position, "Expected type name");
        }

        AbsType type = parseChildType();

        ArrayList<AbsType> conformances = parseConformances();

        if (symbol.token != TokenType.LBRACE) {
            Report.error(symbol.position, "Expected \"{\"");
        }

        skip();

        AbsDefs defs = parseDefinitions();

        if (symbol.token != TokenType.RBRACE) {
            Report.error(symbol.position, "Expected \"}\"");
        }

        skip();

        return new AbsExtensionDef(new Position(symbol.position, defs.position), type.getName(), type, defs, conformances);
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
            if (symbol.lexeme.equals("Bool")) {
                dump("type -> logical");
                skip();

                return new AbsAtomType(s.position, AtomTypeKind.LOG);
            }
            if (symbol.lexeme.equals("Int")) {
                dump("type -> integer");
                skip();

                return new AbsAtomType(s.position, AtomTypeKind.INT);
            }
            if (symbol.lexeme.equals("Double")) {
                dump("type -> double");
                skip();

                return new AbsAtomType(s.position, AtomTypeKind.DOB);
            }
            if (symbol.lexeme.equals("String")) {
                dump("type -> string");
                skip();

                return new AbsAtomType(s.position, AtomTypeKind.STR);
            }
            if (symbol.lexeme.equals("Char")) {
                dump("type -> char");
                skip();

                return new AbsAtomType(s.position, AtomTypeKind.CHR);
            }
            if (symbol.lexeme.equals("Void")) {
                dump("type -> void");
                skip();

                return new AbsAtomType(s.position, AtomTypeKind.VOID);
            }

			dump("type -> identifier");
			skip();

			return new AbsTypeName(s.position, s.lexeme);
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

            ArrayList<AbsType> parameters = new ArrayList<>();
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
			return new AbsFunType(new Position(start, type.position), parameters, type);
		default:
			Report.error(symbol.position, "Syntax error on token \""
					+ symbol.lexeme + "\", expected \"variable type\"");
		}

		return null;
	}

	private ArrayList<AbsParDef> parseParameters() {
		if (symbol.token == TokenType.RPARENT) {
			skip();
			return new ArrayList<>();
		}

		dump("parameters -> parameter parameters'");

		AbsParDef paramater = parseParameter();

        ArrayList<AbsParDef> params = new ArrayList<>();
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
		case KW_NULL:
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
		case KW_NULL:
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
		case KW_NULL:
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
        case NOT:
        case EQU:
        case KW_IS:
        case KW_AS:
        case NEQ:
        case LTH:
        case GTH:
        case LEQ:
        case GEQ:
        case MUL:
        case DIV:
        case MOD:
        case ADD:
        case SUB:
        case LPARENT:
        case LBRACKET:
        case DOT:
        case ARROW:
        case QMARK:
        case EMARK:
        case KW_FUN:
        case KW_IF:
        case KW_VAR:
        case KW_WHILE:
        case KW_STRUCT:
        case KW_IMPORT:
        case KW_LET:
        case KW_NULL:
        case KW_CLASS:
        case KW_INTERFACE:
        case KW_IN:
        case KW_RETURN:
        case KW_PUBLIC:
        case KW_PRIVATE:
        case KW_CONTINUE:
        case KW_BREAK:
        case KW_SWITCH:
        case KW_CASE:
        case KW_DEFAULT:
        case KW_ENUM:
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
		case KW_NULL:
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

        case KW_AS:
            dump("compare_expression' -> as identifier");
            skip();

            expr = new AbsVarNameExpr(symbol.position, symbol.lexeme);;
            skip();

            oper = AbsBinExpr.AS;
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

		return new AbsBinExpr(new Position(e.position, expr.position), oper, e, expr);
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
		case KW_NULL:
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
        case KW_AS:
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
		case KW_NULL:
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
        case KW_AS:
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
		case KW_NULL:
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
		case KW_NULL:
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
        case KW_AS:
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
		case KW_NULL:
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
					return new AbsFunCall(symbol.position, current.lexeme, new ArrayList<>());
				}

				dump("atom_expression -> identifier ( expressions )");

				// TODO: - Optimize this
				// FIXME: - Function arguments should be parsed with their own method
                ArrayList<AbsExpr> arguments = parseTupleExpressions(true);

                ArrayList<AbsLabeledExpr> absExprs = new ArrayList<>();
				for (AbsExpr e : arguments)
					absExprs.add((AbsLabeledExpr) e);

				if (symbol.token != TokenType.RPARENT)
					Report.error(symbol.position, "Expected ')'");
				skip();

				return new AbsFunCall(new Position(current.position, absExprs.get(absExprs.size() - 1).position), current.lexeme, absExprs);
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

        ArrayList<Condition> conditions = new ArrayList<>();
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
				elseBody.position : conditions.get(conditions.size() - 1).body.position;
		return new AbsIfStmt(new Position(condition.cond.position, lastPos), conditions, elseBody);
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

        ArrayList<AbsCaseStmt> cases = new ArrayList<>();
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
				defaultBody != null ? defaultBody.position : cases.get(cases.size() - 1).position);

		return new AbsSwitchStmt(switchPos, subjectExpr, cases, defaultBody);
	}

	private AbsCaseStmt parseCase() {
		if (symbol.token != TokenType.KW_CASE)
			Report.error(symbol.position,
					"Syntax error, expected keyword \"case\"");

		Position start = symbol.position;
		skip();

        ArrayList<AbsExpr> expressions = new ArrayList<>();
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
			return new AbsListExpr(start, new ArrayList<>());
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
            ArrayList<AbsExpr> elements = new ArrayList<>();
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
			return new AbsListExpr(new Position(elements.get(0).position, elements.get(elements.size() - 1).position), elements);
		}
		else if (symbol.token == TokenType.RBRACKET) {
			dump("[] -> [expression]");
            ArrayList<AbsExpr> elements = new ArrayList<>();
			elements.add(e1);
			skip();
			return new AbsListExpr(new Position(elements.get(0).position, elements.get(elements.size() - 1).position), elements);
		}

		return null;
	}


	private AbsTupleExpr parseTupleExpression(boolean argumentTuple) {
		Position start = symbol.position;
		skip();

        ArrayList<AbsExpr> expressions = parseTupleExpressions(argumentTuple);

		if (symbol.token != TokenType.RPARENT)
			Report.error(symbol.position, "Expected ')'");

		Position tuplePos = new Position(start, symbol.position);
		skip();

		return new AbsTupleExpr(tuplePos, expressions);
	}

	private ArrayList<AbsExpr> parseTupleExpressions(boolean argumentTuple) {
		int index = 0;
        ArrayList<AbsExpr> expressions = new ArrayList<>();

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
		symbol = lexAn.nextSymbol();
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
