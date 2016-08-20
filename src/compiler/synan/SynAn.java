package compiler.synan;

import java.util.Vector;

import compiler.Position;
import compiler.Report;
import compiler.abstr.tree.*;
import compiler.lexan.*;

/**
 * Sintaksni analizator.
 * 
 * @author sliva
 * @implementation Toni Kocjan
 */
public class SynAn {

	/** Leksikalni analizator. */
	private LexAn lexAn;

	/** Ali se izpisujejo vmesni rezultati. */
	private boolean dump;

	/** Current & previous symbol */
	private Symbol symbol = null;
	private Symbol previous = null;

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
		
		if (symbol.token == Token.NEWLINE)
			skip();

		dump("source -> statements");
		AbsTree abstrTree = parseStatements();

		if (symbol.token != Token.EOF && symbol.token != Token.NEWLINE)
			Report.error(symbol.position, "Syntax error on token \""
					+ previous.lexeme + "\"");

		return abstrTree;
	}

	private AbsStmts parseStatements() {
		dump("statements -> statement statements'");
		
		Vector<AbsStmt> absStmts = new Vector<>();
		if (symbol.token == Token.RBRACE)
			return new AbsStmts(symbol.position, absStmts);
		
		AbsStmt statement = parseStatement();
		absStmts.add(statement);
		absStmts.addAll(parseStatements_(statement));
		
		return new AbsStmts(new Position(absStmts.firstElement().position,
				absStmts.lastElement().position), absStmts);
	}

	private Vector<AbsStmt> parseStatements_(AbsStmt prevStmt) {
		switch (symbol.token) {
		case EOF:
			dump("statements' -> $");

			return new Vector<>();
		case RBRACE:
			dump("definitions' -> e");
//			skip();

			return new Vector<>();
		case SEMIC:
			skip();
			if (symbol.token == Token.NEWLINE)
				skip();
			
			if (symbol.token == Token.EOF || symbol.token == Token.RBRACE)
				return new Vector<AbsStmt>();
		case IDENTIFIER:
			AbsStmt statement = parseStatement();
			Vector<AbsStmt> absStmts = parseStatements_(statement);
			absStmts.add(0, statement);
			return absStmts;
		case NEWLINE:
			skip();
			
			if (symbol.token == Token.EOF || symbol.token == Token.RBRACE)
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
			AbsVarName varName = new AbsVarName(var.position, var.name);
			AbsExpr e = parseExpression();
			
			absStmts = parseStatements_(null);
			
			absStmts.add(0, new AbsBinExpr(new Position(var.position, e.position), 
					AbsBinExpr.ASSIGN, varName, e));
			
			return absStmts;
		default:
			Report.error(symbol.position, "Syntax error on token \""
					+ previous.lexeme
					+ "\", expected \";\" or \"NEWLINE\" after this token");
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
		case KW_FUN:
		case KW_IMPORT:
			dump("statement -> definition");
			return parseDefinition();
			
		/**
		 * Parse control transfer statement.
		 */
		case KW_CONTINUE:
			dump("expression -> continue");
			skip();
			return new AbsControlTransferStmt(symbol.position, 
					ControlTransfer.Continue);
		case KW_BREAK:
			dump("expression -> break");
			skip();
			return new AbsControlTransferStmt(symbol.position, 
					ControlTransfer.Break);
		
		/**
		 * Parse expression.
		 */
		default:
			dump("statement -> expression");
			return parseExpression();
		}
	}
//
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
		switch (symbol.token) {
		case KW_FUN:
			dump("definition -> function_definition");
			definition = parseFunDefinition();
			break;
		case KW_VAR:
		case KW_LET:
		case KW_PUBLIC:
		case KW_PRIVATE:
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
		default:
			if (symbol.token != Token.EOF)
				Report.error(symbol.position, "Syntax error on token \""
						+ symbol.lexeme + "\", delete this token");
			else
				Report.error(previous.position, "Syntax error on token \""
						+ previous.lexeme + "\", delete this token");
		}

		return definition;
	}

	private AbsFunDef parseFunDefinition() {
		Position startPos = symbol.position;
		if (symbol.token == Token.KW_FUN) {
			Symbol id = skip(new Symbol(Token.IDENTIFIER, "identifier", null));

			skip(new Symbol(Token.LPARENT, "(", null));
			skip();
			dump("function_definition -> func identifier ( parameters ) function_definition'");

			Vector<AbsPar> params = parseParameters();

			return parseFunDefinition_(startPos, id, params);
		}
		Report.error(previous.position, "Syntax error on token \""
				+ previous.lexeme + "\", expected keyword \"fun\"");

		return null;
	}

	private AbsFunDef parseFunDefinition_(Position startPos, Symbol id,
			Vector<AbsPar> params) {
		AbsType type = null;

		if (symbol.token == Token.LBRACE) {
			dump("function_definition' -> { statements } ");
			type = new AbsAtomType(symbol.position, AtomType.VOID);
		} else {
			dump("function_definition' -> type { statements } ");
			type = parseType();
		}

		if (symbol.token != Token.LBRACE)
			Report.error(symbol.position, "Syntax error on token \""
					+ previous.lexeme + "\", expected \"{\" after this token");

		skip(new Symbol(Token.NEWLINE, "NEWLINE", null));
		skip();
		
		AbsStmts expr = parseStatements();
		if (symbol.token != Token.RBRACE)
			Report.error(symbol.position, "Syntax error on token \""
					+ previous.lexeme + "\", expected \"}\" after this token");
		skip();

		return new AbsFunDef(new Position(startPos, expr.position), id.lexeme,
				params, type, expr);
	}

	private AbsDef parseVarDefinition() {
		Position startPos = symbol.position;
		Visibility v = Visibility.Public;
		boolean isConstant = false;
		Symbol id = null;
		
		if (symbol.token == Token.KW_PUBLIC)
			skip();
		else if (symbol.token == Token.KW_PRIVATE) {
			v = Visibility.Private;
			skip();
		}
		
		if (symbol.token == Token.KW_VAR)
			id = skip(new Symbol(Token.IDENTIFIER, "identifier", null));
		else if (symbol.token == Token.KW_LET) {
			isConstant = true;
			id = skip(new Symbol(Token.IDENTIFIER, "identifier", null));
		}
		
		skip();
		
		AbsType type = null;
		
		if (symbol.token == Token.ASSIGN) {
			dump("var_definition -> var identifier = expr");
			return new AbsVarDef(startPos, id.lexeme, type, isConstant);
		}
		else if (symbol.token != Token.COLON) 
			Report.error(previous.position, "Syntax error on token \""
					+ previous.lexeme + "\", expected \":\"");
		
		skip();

		dump("var_definition -> var identifier : type");

		type = parseType();
		return new AbsVarDef(new Position(startPos, type.position),
				id.lexeme, type, isConstant, v);
	}

	private AbsImportDef parseImportDefinition() {
		Position pos = symbol.position;
		skip(new Symbol(Token.IDENTIFIER, "IDENTIFIER", null));
		String file = symbol.lexeme;
		skip();
		if (symbol.token == Token.DOT) {
			skip();
			return parseImportDefinition_(new AbsImportDef(pos, file));
		} else
			return new AbsImportDef(pos, file);
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
	
	private AbsClassDef parseClassDefinition() {
		Position start = symbol.position;
		String name = skip(new Symbol(Token.IDENTIFIER, "IDENTIFIER", null)).lexeme;
		
		skip(new Symbol(Token.LBRACE, "{", null));
		skip(new Symbol(Token.NEWLINE, "\n", null));
		skip();
		
		Vector<AbsStmt> statements = parseClassDefinitions();
		if (symbol.token != Token.RBRACE)
			Report.error(symbol.position, "Syntax error on token \""
					+ symbol.lexeme + "\", expected \"}\"");
		skip();
		
		Position end = statements.lastElement().position;
		return new AbsClassDef(name, new Position(start, end), statements);
	}
	
	private Vector<AbsStmt> parseClassDefinitions() {
		Vector<AbsStmt> statements = new Vector<>();
		while (true) {
			AbsDef definition;
			
			switch (symbol.token) {
			case KW_PUBLIC:
			case KW_PRIVATE:
			case KW_VAR:
			case KW_LET:
				definition = parseDefinition();
				statements.add(0, definition);
				
				if (symbol.token == Token.ASSIGN) {
					skip();
					dump("var_definition -> = expression");
					
					AbsVarName varName = new AbsVarName(definition.position, 
							((AbsVarDef)definition).name);
					AbsExpr e = parseExpression();

					statements.add(new AbsBinExpr(new Position(definition.position, e.position), 
							AbsBinExpr.ASSIGN, varName, e));
				}
				break;
			case KW_FUN:
				definition = parseDefinition();
				break;
			case RBRACE:
				return statements;
			default:
				Report.error(symbol.position, "Syntax error todooooo");
			}
			
			if (symbol.token == Token.SEMIC)
				skip();
			if (symbol.token == Token.NEWLINE)
				skip();
		}
	}

	private AbsType parseType() {
		Symbol s = symbol;

		switch (symbol.token) {
		case IDENTIFIER:
			dump("type -> identifier");
			skip();

			return new AbsTypeName(s.position, s.lexeme);
		case BOOL:
			dump("type -> logical");
			skip();

			return new AbsAtomType(s.position, AtomType.LOG);
		case INTEGER:
			dump("type -> integer");
			skip();

			return new AbsAtomType(s.position, AtomType.INT);
		case STRING:
			dump("type -> string");
			skip();

			return new AbsAtomType(s.position, AtomType.STR);
		case CHAR:
			dump("type -> char");
			skip();

			return new AbsAtomType(s.position, AtomType.CHR);
		case DOUBLE:
			dump("type -> double");
			skip();

			return new AbsAtomType(s.position, AtomType.DOB);
		case VOID:
			dump("type -> void");
			skip();

			return new AbsAtomType(s.position, AtomType.VOID);
		case LBRACKET:
			skip();
			dump("type -> [ type ]");
			AbsType type = parseType();

			if (symbol.token != Token.RBRACKET)
				Report.error(symbol.position, "Syntax error, insert \"]\"");

			skip();
			return new AbsListType(new Position(s.position, type.position), 0,
					type);
		case LPARENT:
			Position start = symbol.position;
			dump("type -> (parameters) -> type");
			skip();
			
			Vector<AbsType> parameters = new Vector<>();
			if (symbol.token != Token.RPARENT)
				while (true) {
					parameters.add(parseType());
					if (symbol.token != Token.COMMA)
						break;
					skip();
				}
			if (symbol.token != Token.RPARENT)
				Report.error(symbol.position,
						"Syntax error, insert \")\" to complete function declaration");
			skip(new Symbol(Token.ARROW, "->", null));
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

	private Vector<AbsPar> parseParameters() {
		if (symbol.token == Token.RPARENT) {
			skip();
			return new Vector<>();
		}

		dump("parameters -> parameter parameters'");

		AbsPar paramater = parseParameter();
		Vector<AbsPar> params = new Vector<>();
		params.add(paramater);
		params.addAll(parseParameters_());

		return params;
	}

	private Vector<AbsPar> parseParameters_() {
		if (symbol.token == Token.COMMA) {
			dump("parameters' -> parameters");
			skip();

			AbsPar parameter = parseParameter();
			Vector<AbsPar> params = new Vector<>();
			params.add(parameter);
			params.addAll(parseParameters_());
			return params;
		} else if (symbol.token != Token.RPARENT)
			Report.error(symbol.position,
					"Syntax error, insert \")\" to complete function declaration");

		dump("parameters' -> e");
		skip();

		return new Vector<>();
	}

	private AbsPar parseParameter() {
		if (symbol.token == Token.IDENTIFIER) {
			Symbol id = symbol;

			skip(new Symbol(Token.COLON, ":", null));
			skip();

			dump("parameter -> identifier : type");

			AbsType type = parseType();
			return new AbsPar(new Position(id.position, type.position),
					id.lexeme, type);
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

		return parseMulExpression_(new AbsBinExpr(new Position(e.position,
				expr.position), oper, e, expr));
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
			dump("postfix_expression -> atom_expression postfix_expression'");

			return parsePostfixExpression_(parseAtomExpression());
		default:
			Report.error(symbol.position, "Syntax error on token \""
					+ symbol.lexeme + "\", delete this token");
		}

		return null;
	}

	private AbsExpr parsePostfixExpression_(AbsExpr e) {
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
			if (symbol.token != Token.RBRACKET)
				Report.error(previous.position,
						"Syntax error, insert \"]\" to complete expression");
			skip();
			return parsePostfixExpression_(new AbsBinExpr(new Position(
					e.position, expr.position), AbsBinExpr.ARR, e, expr));
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

			return new AbsAtomConst(current.position, AtomType.LOG,
					current.lexeme);
		case INT_CONST:
			dump("atom_expression -> int_const");
			skip();

			return new AbsAtomConst(current.position, AtomType.INT,
					current.lexeme);
		case STR_CONST:
			dump("atom_expression -> str_const");
			skip();

			return new AbsAtomConst(current.position, AtomType.STR,
					current.lexeme);
		case CHAR_CONST:
			dump("atom_expression -> char_const");
			skip();

			return new AbsAtomConst(current.position, AtomType.CHR,
					current.lexeme);
		case DOUBLE_CONST:
			dump("atom_expression -> double_const");
			skip();

			return new AbsAtomConst(current.position, AtomType.DOB,
					current.lexeme);
		case KW_NIL:
			dump("atom_expression -> nil");
			skip();

			return new AbsAtomConst(current.position, AtomType.NIL,
					current.lexeme);
		case KW_IF:
			dump("atom_expression -> if_expression");
			return parseIf();
		case KW_WHILE:
			dump("atom_expression -> while expression { expression }");
			return parseWhileLoop();
		case KW_FOR:
			dump("atom_expression -> for indentifier in expression { expression }");
			return parseForLoop();
		case IDENTIFIER:
			skip();
			if (symbol.token == Token.LPARENT) {
				skip();
				
				if (symbol.token == Token.RPARENT) {
					dump("atom_expression -> identifier ( )");
					skip();
					return new AbsFunCall(symbol.position, current.lexeme, new Vector<>());
				}

				dump("atom_expression -> identifier ( expressions )");

				Vector<AbsExpr> absExprs = parseExpressions();
				return new AbsFunCall(new Position(current.position,
						absExprs.lastElement().position), current.lexeme,
						absExprs);
			} else {
				dump("atom_expression -> identifier");
				return new AbsVarName(current.position, current.lexeme);
			}
		case LBRACKET:
			return parseBracket();
		case KW_RETURN:
			Position pos = symbol.position;
			skip();
			if (symbol.token == Token.SEMIC) {
				dump("atom_expression -> return");
				return new AbsReturnExpr(pos, null);
			}
			dump("atom_expression -> return expression");
			AbsExpr e = parseExpression();
			return new AbsReturnExpr(new Position(pos, e.position), e);
		default:
			Report.error("Syntax error on token \"" + symbol.lexeme + "\", delete this token");
		}
		return null;
	}

	private AbsExpr parseForLoop() {
		if (symbol.token == Token.KW_FOR) {
			Position start = symbol.position;
			Symbol count = skip(new Symbol(Token.IDENTIFIER, "identifier", null));
			skip();
			
			if (symbol.token != Token.KW_IN)
				Report.error(previous.position, "Syntax error on token \""
						+ previous.lexeme
						+ "\", expected keyword \"in\" after this token");
			skip();

			AbsExpr e = parseExpression();
			if (symbol.token != Token.LBRACE)
				Report.error(previous.position, "Syntax error on token \""
						+ previous.lexeme
						+ "\", expected \"{\" after this token");
			skip(new Symbol(Token.NEWLINE, "NEWLINE", null));
			skip();
			
			AbsStmts s = parseStatements();

			if (symbol.token != Token.RBRACE)
				Report.error(previous.position, "Syntax error on token \""
						+ previous.lexeme
						+ "\", expected \"}\" after this token");
			skip();

			return new AbsFor(new Position(start, s.position), new AbsVarName(
					count.position, count.lexeme), e, s);
		}
		Report.error(symbol.position, "Syntax error, expected keyword \"for\"");

		return null;
	}

	private AbsExpr parseWhileLoop() {
		if (symbol.token == Token.KW_WHILE) {
			Position start = symbol.position;
			skip();
			AbsExpr e1 = parseExpression();
			if (symbol.token == Token.LBRACE) {
				skip(new Symbol(Token.NEWLINE, "NEWLINE", null));
				skip();
				AbsStmts s = parseStatements();

				if (symbol.token != Token.RBRACE)
					Report.error(symbol.position, "Syntax error on token \""
							+ previous.lexeme
							+ "\", expected '}' after this token");
				skip();

				return new AbsWhile(new Position(start, s.position), e1, s);
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
		if (symbol.token != Token.LBRACE)
			Report.error(symbol.position, "Syntax error on token \""
					+ previous.lexeme + "\", expected '{' after this token");
		skip(new Symbol(Token.NEWLINE, "NEWLINE", null));
		skip();
		AbsStmts s = parseStatements();
		if (symbol.token != Token.RBRACE)
			Report.error(symbol.position, "Syntax error on token \""
					+ previous.lexeme + "\", expected '}' after this token");
		skip();
		return new Condition(condition, s);
	}
	
	private AbsExpr parseIf() {
		if (symbol.token == Token.KW_IF) {
			Position start = symbol.position;
			return parseIf_(start, parseIfCondition());
		}
		Report.error(previous.position,
				"Syntax error, expected keyword \"while\"");
		return null;
	}

	private AbsExpr parseIf_(Position start, Condition condition) {
		if (symbol.token == Token.NEWLINE) 
			skip();
		
		Vector<Condition> conditions = new Vector<>();
		conditions.add(condition);
		AbsStmts elseBody = null;
		
		while (true) {
			if (symbol.token == Token.KW_ELSE) {
				skip();
				
				if (symbol.token == Token.KW_IF) {
					dump("if_expression' -> else if { statements }");
					conditions.add(parseIfCondition());
					
					if (symbol.token == Token.NEWLINE) 
						skip();
					continue;
				}

				if (symbol.token != Token.LBRACE)
					Report.error(symbol.position, "Syntax error on token \""
							+ previous.lexeme + "\", expected '{' after this token");
				
				dump("if_expression' -> else { statements }");

				skip(new Symbol(Token.NEWLINE, "NEWLINE", null));
				skip();

				elseBody = parseStatements();
				skip();
			}
			break;
		}

		Position lastPos = elseBody != null ? 
				elseBody.position : conditions.lastElement().body.position;
		return new AbsIfExpr(new Position(condition.cond.position, lastPos), 
				conditions, elseBody);
	}
	
	private AbsExpr parseBracket() {
		dump("atom_expression -> []");

		Position start = symbol.position;
		skip();
		
		if (symbol.token == Token.RBRACKET) {
			skip();
			return new AbsListExpr(start, new Vector<AbsExpr>());
		}
		
		AbsExpr e1 = parseExpression();
		
		if (symbol.token == Token.KW_FOR) {
			dump("[] -> [ expression for identifier in expression ]");
			Vector<AbsStmt> stmt = new Vector<>();
			stmt.add(e1);
			AbsStmts s = new AbsStmts(new Position(start, e1.position), stmt);
			
			if (symbol.token != Token.KW_FOR)
				Report.error(previous.position, "Syntax error on token \""
						+ previous.lexeme
						+ "\", expected keyword \"for\" after this token");
			
			Symbol count = skip(new Symbol(Token.IDENTIFIER, "identifier", null));
			AbsVarName var = new AbsVarName(count.position, count.lexeme);
			skip();
			
			if (symbol.token != Token.KW_IN)
				Report.error(previous.position, "Syntax error on token \""
						+ previous.lexeme
						+ "\", expected keyword \"in\" after this token");
			skip();
			
			AbsExpr e2 = parseExpression();
			
			if (symbol.token != Token.RBRACKET)
				Report.error(previous.position, "Syntax error on token \""
						+ previous.lexeme
						+ "\", expected \"]\" after this token");
			skip();
			
			return new AbsFor(new Position(start, e2.position), var, e2, s);
		}
		
		else if (symbol.token == Token.COMMA) {
			dump("[] -> [expression, expressions']");
			Vector<AbsExpr> elements = new Vector<>();
			elements.add(e1);
			while (symbol.token == Token.COMMA) {
				skip();
				elements.add(parseExpression());
			}
			if (symbol.token != Token.RBRACKET)
				Report.error(previous.position, "Syntax error on token \""
						+ previous.lexeme
						+ "\", expected \"]\" after this token");
			skip();
			return new AbsListExpr(new Position(elements.firstElement().position, 
					elements.lastElement().position), elements);
		}
		else if (symbol.token == Token.RBRACKET) {
			dump("[] -> [expression]");
			Vector<AbsExpr> elements = new Vector<>();
			elements.add(e1);
			skip();
			return new AbsListExpr(new Position(elements.firstElement().position, 
					elements.lastElement().position), elements);
		}
		
		return null;
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
