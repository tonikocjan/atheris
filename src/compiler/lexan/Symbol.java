package compiler.lexan;

import compiler.*;

/**
 * Leksikalni simbol.
 * 
 * @author sliva
 */
public class Symbol {
	
	/** Vrsta simbola. */
	public final Token token;

	/** Znakovna predstavitev simbola. */
	public final String lexeme;
	
	/** Polozaj simbola v izvorni datoteki. */
	public final Position position;

	/**
	 * Ustvari nov leksikalni simbol.
	 * 
	 * @param token
	 *            Vrsta simbola.
	 * @param lexeme
	 *            Znakovna predstavitev simbola.
	 * @param begLine
	 *            Vrstca zacetka simbola v izvorni datoteki.
	 * @param begColumn
	 *            Stolpec zacetka simbola v izvorni datoteki.
	 * @param endLine
	 *            Vrstica konca simbola v izvorni datoteki.
	 * @param endColumn
	 *            Stolpec konca simbola v izvorni datoteki.
	 */
	public Symbol(Token token, String lexeme, int begLine, int begColumn, int endLine, int endColumn) {
		this.token = token;
		this.lexeme = lexeme;
		this.position = new Position(begLine, begColumn, endLine, endColumn);
	}

	/**
	 * Ustvari nov leksikalni simbol.
	 * 
	 * @param token
	 *            Vrsta simbola.
	 * @param lexeme
	 *            Znakovna predstavitev simbola.
	 * @param position
	 *            Polozaj simbola v izvorni datoteki.
	 */
	public Symbol(Token token, String lexeme, Position position) {
		this.token = token;
		this.lexeme = lexeme;
		this.position = position;
	}

	@Override
	public String toString() {
		String tokenName = "";
		switch (token) {

		case EOF       : tokenName = "EOF"       ; break;
		
		case IDENTIFIER: tokenName = "IDENTIFIER"; break;

		case LOG_CONST : tokenName = "LOG_CONST" ; break;
		case INT_CONST : tokenName = "INT_CONST" ; break;
		case STR_CONST : tokenName = "STR_CONST" ; break;
		case CHAR_CONST   : tokenName = "CHAR_CONST" ; break;
		case DOUBLE_CONST : tokenName = "DOUBLE_CONST" ; break;

		case AND       : tokenName = "AND"       ; break;
		case IOR       : tokenName = "IOR"       ; break;
		case NOT       : tokenName = "NOT"       ; break;
		
		case EQU       : tokenName = "EQU"       ; break;
		case NEQ       : tokenName = "NEQ"       ; break;
		case LTH       : tokenName = "LTH"       ; break;
		case GTH       : tokenName = "GTH"       ; break;
		case LEQ       : tokenName = "LEQ"       ; break;
		case GEQ       : tokenName = "GEQ"       ; break;
		
		case MUL       : tokenName = "MUL"       ; break;
		case DIV       : tokenName = "DIV"       ; break;
		case MOD       : tokenName = "MOD"       ; break;
		case ADD       : tokenName = "ADD"       ; break;
		case SUB       : tokenName = "SUB"       ; break;
		
		case LPARENT   : tokenName = "LPARENT"   ; break;
		case RPARENT   : tokenName = "RPARENT"   ; break;
		case LBRACKET  : tokenName = "LBRACKET"  ; break;
		case RBRACKET  : tokenName = "RBRACKET"  ; break;
		case LBRACE    : tokenName = "LBRACE"    ; break;
		case RBRACE    : tokenName = "RBRACE"    ; break;
		
		case DOT	     : tokenName = "DOT"     ; break;
		case COLON     : tokenName = "COLON"     ; break;
		case SEMIC     : tokenName = "SEMIC"     ; break;
		case COMMA     : tokenName = "COMMA"     ; break;
		case ARROW     : tokenName = "->"    	 ; break;
		
		case KW_STRUCT : tokenName = "STRUCT"    ; break;
		case KW_CLASS  : tokenName = "CLASS"     ; break;
		case KW_SELF   : tokenName = "SELF"      ; break;
		case KW_NIL    : tokenName = "NIL"       ; break;
		
		case ASSIGN    : tokenName = "ASSIGN"    ; break;
		
		case BOOL	   : tokenName = "BOOLEAN"	 ; break;
		case VOID	   : tokenName = "VOID" 	 ; break;
		case INTEGER   : tokenName = "INTEGER"   ; break;
		case STRING    : tokenName = "STRING"    ; break;
		case CHAR      : tokenName = "CHAR"      ; break;
		case DOUBLE    : tokenName = "DOUBLE"    ; break;
		
		case KW_ELSE   : tokenName = "ELSE"      ; break;
		case KW_FOR    : tokenName = "FOR"       ; break;
		case KW_FUN    : tokenName = "FUNC"      ; break;
		case KW_IF     : tokenName = "IF"        ; break;
		case KW_VAR    : tokenName = "VAR"       ; break;
		case KW_LET    : tokenName = "LET"       ; break;
		case KW_WHILE  : tokenName = "WHILE"     ; break;
		
		case KW_IMPORT : tokenName = "IMPORT"    ; break;
		
		case NEWLINE   	 : tokenName = "NEWLINE"   ; break;
		case KW_IN     	 : tokenName = "IN"   	 ; break;
		case KW_RETURN 	 : tokenName = "RETURN"    ; break;
		case KW_PUBLIC 	 : tokenName = "PUBLIC"    ; break;
		case KW_PRIVATE	 : tokenName = "PRIVATE"   ; break;
		case KW_BREAK	 : tokenName = "PRIVATE"   ; break;
		case KW_CONTINUE : tokenName = "PRIVATE"   ; break;
		
		default:
			Report.error("Internal error: token=" + token + " in compiler.lexan.Symbol.toString().");
		}
		return tokenName + ":" + lexeme;
	}

}
