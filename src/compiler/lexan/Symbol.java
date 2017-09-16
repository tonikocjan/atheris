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

package compiler.lexan;

import compiler.*;
import compiler.logger.LoggerFactory;
import compiler.logger.LoggerInterface;

public class Symbol {

    private static LoggerInterface logger = LoggerFactory.logger();

    private TokenType tokenType;
	private String lexeme;
	private Position position;

    public void setLexeme(String lexeme) {
        this.lexeme = lexeme;
    }

    public void setPosition(Position position) {
        this.position = position;
    }

    public void setTokenType(TokenType tokenType) {
        this.tokenType = tokenType;
    }

    public Position getPosition() {
        return position;
    }

    public String getLexeme() {
        return lexeme;
    }

    public TokenType getTokenType() {
        return tokenType;
    }

    @Override
    public String toString() {
        String tokenName = "";
        switch (tokenType) {

            case EOF          : tokenName = "EOF"       ; break;

            case IDENTIFIER   : tokenName = "IDENTIFIER"; break;

            case LOG_CONST    : tokenName = "LOG_CONST" ; break;
            case INT_CONST    : tokenName = "INT_CONST" ; break;
            case STR_CONST    : tokenName = "STR_CONST" ; break;
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
            case KW_NULL   : tokenName = "NIL"       ; break;

            case ASSIGN    : tokenName = "ASSIGN"    ; break;

            case KW_ELSE   : tokenName = "ELSE"      ; break;
            case KW_FOR    : tokenName = "FOR"       ; break;
            case KW_FUN    : tokenName = "FUNC"      ; break;
            case KW_IF     : tokenName = "IF"        ; break;
            case KW_VAR    : tokenName = "VAR"       ; break;
            case KW_LET    : tokenName = "LET"       ; break;
            case KW_WHILE  : tokenName = "WHILE"     ; break;

            case KW_IMPORT   : tokenName = "IMPORT"    ; break;

            case NEWLINE   	 : tokenName = "NEWLINE"   ; break;
            case KW_IN     	 : tokenName = "IN"   	 ; break;
            case KW_RETURN 	 : tokenName = "RETURN"    ; break;
            case KW_PUBLIC 	 : tokenName = "PUBLIC"    ; break;
            case KW_PRIVATE	 : tokenName = "PRIVATE"   ; break;
            case KW_BREAK	 : tokenName = "BREAK"   ; break;
            case KW_CONTINUE : tokenName = "CONTINUE"   ; break;
            case KW_SWITCH   : tokenName = "SWITCH"   ; break;
            case KW_CASE     : tokenName = "CASE"   ; break;
            case KW_DEFAULT  : tokenName = "DEFAULT"   ; break;
            case KW_ENUM     : tokenName = "ENUM"   ; break;
            case KW_AS       : tokenName = "AS"; break;
            case KW_INIT     : tokenName = "INIT"; break;
            case KW_EXTENSION   : tokenName = "EXTENSION"; break;
            case KW_IS          : tokenName = "IS"; break;
            case KW_OVERRIDE    : tokenName = "OVERRIDE"; break;

            default:
                logger.error("Internal error: tokenType=" + tokenType + " in compiler.lexan.Symbol.toString().");
        }
        return tokenName + ":" + lexeme;
    }

    public boolean isIdentifier() {
        return tokenType == TokenType.IDENTIFIER;
    }

    public static class Builder {

        private TokenType tokenType;
        private String lexeme;
        private int startRow, endRow, startCol, endCol;
        private Position position;

        public Symbol build() {
            if (tokenType == null) {
                return null;
            }

            Position pos;

            if (position == null) {
                pos = new Position(startRow, startCol, endRow, endCol);
            }
            else {
                pos = position;
            }

            Symbol symbol = new Symbol();
            symbol.setLexeme(lexeme);
            symbol.setTokenType(tokenType);
            symbol.setPosition(pos);

            return symbol;
        }

        public Builder setTokenType(TokenType type) {
            this.tokenType = type;
            return this;
        }

        public Builder setLexeme(String lexeme) {
            this.lexeme = lexeme;
            return this;
        }

        public Builder setStartRow(int row) {
            this.startRow = row;
            return this;
        }

        public Builder setEndRow(int row) {
            this.endRow = row;
            return this;
        }

        public Builder setStartCol(int col) {
            this.startCol = col;
            return this;
        }

        public Builder setEndCol(int col) {
            this.endCol = col;
            return this;
        }

        public Builder setPosition(Position position) {
            this.position = position;
            return this;
        }
    }

    public static Builder EOF() {
        return new Builder().setLexeme("$").setTokenType(TokenType.EOF);
    }
}
