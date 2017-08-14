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

/**
 * Token types.
 * 
 * @author toni kocjan
 */
public enum TokenType {

	/** Token memberType: konec datoteke. */							EOF,
	
	/** Token memberType: ime. */										IDENTIFIER,

	/** Token memberType: logicna konstanta. */						LOG_CONST,
	/** Token memberType: celo stevilo. */							INT_CONST,
	/** Token memberType: niz. */										STR_CONST,
	/** Token memberType: decimal constant.  */	 					DOUBLE_CONST,
	/** Token memberType: char constant.  */		 					CHAR_CONST,

	/** Token memberType: logicni in. */								AND,
	/** Token memberType: logicni ali. */								IOR,
	/** Token memberType: logicni ne. */								NOT,
	/** Token memberType: je-enako. */								EQU,
	/** Token memberType: ni-enako. */								NEQ,
	/** Token memberType: manjse-kot. */								LTH,
	/** Token memberType: vecje-kot. */								GTH,
	/** Token memberType: manjse-ali-enako. */						LEQ,
	/** Token memberType: vecje-ali-enako. */							GEQ,
	
	/** Token memberType: celostevilsko mnozenje. */					MUL,
	/** Token memberType: celostevilsko deljenje. */					DIV,
	/** Token memberType: ostanek po celostevilskem deljenju. */		MOD,
	/** Token memberType: celostevilsko sestevanje ali predznak. */	ADD,
	/** Token memberType: celostevilsko odstevanje ali predznak. */	SUB,
		
	/** Token memberType: levi oklepaj. */							LPARENT,
	/** Token memberType: desni oklepaj. */							RPARENT,
	/** Token memberType: levi oglati oklepaj. */						LBRACKET,
	/** Token memberType: desni oglati oklepaj. */					RBRACKET,
	/** Token memberType: levi zaviti oklepaj. */						LBRACE,
	/** Token memberType: desni zaviti oklepaj. */					RBRACE,
	
	/** Token memberType: pika. */									DOT,
	/** Token memberType: dvopicje. */								COLON,
	/** Token memberType: podpicje. */								SEMIC,
	/** Token memberType: vejica. */                                  COMMA,
	/** Token memberType: nova vrstica.  */ 							NEWLINE,
	/** Token memberType: puščica ->.  */ 							ARROW,
	
	/** Token memberType: prirejanje. */								ASSIGN,

	/** Token memberType: question mark.  */ 							QMARK,
	/** Token memberType: esclamation mark.  */ 						EMARK,

//	/** Token memberType: tip integer.  */							INTEGER,
//	/** Token memberType: tip string.  */								STRING,
//	/** Token memberType: tip double.  */								DOUBLE,
//	/** Token memberType: tip bool.  */								BOOL,
//	/** Token memberType: tip char.  */								CHAR,
//	/** Token memberType: tip void.  */								VOID,
	
	/** Token memberType: kljucna beseda else.  */					KW_ELSE,
	/** Token memberType: kljucna beseda for.  */						KW_FOR,
	/** Token memberType: kljucna beseda functionDefinition.  */						KW_FUN,
	/** Token memberType: kljucna beseda if.  */						KW_IF,
	/** Token memberType: kljucna beseda variableDefinition.  */						KW_VAR,
	/** Token memberType: kljucna beseda while.  */					KW_WHILE,
	/** Token memberType: keyword struct.  */							KW_STRUCT,
	/** Token memberType: keyword import.  */ 						KW_IMPORT,
	/** Token memberType: keyword let.  */ 							KW_LET,
	/** Token memberType: keyword nil.  */ 							KW_NULL,
	/** Token memberType: keyword class.  */ 							KW_CLASS,
	/** Token memberType: keyword in.  */ 							KW_IN,
	/** Token memberType: keyword return.  */ 						KW_RETURN,
	/** Token memberType: keyword public.  */ 						KW_PUBLIC,
	/** Token memberType: keyword private.  */ 						KW_PRIVATE,
	/** Token memberType: keyword continue.  */ 						KW_CONTINUE,
	/** Token memberType: keyword break.  */ 							KW_BREAK,
	/** Token memberType: keyword switch.  */ 						KW_SWITCH,
	/** Token memberType: keyword case.  */ 							KW_CASE,
	/** Token memberType: keyword default.  */ 						KW_DEFAULT,
	/** Token memberType: keyword enum.  */ 							KW_ENUM,
    /** Token memberType: keyword init.  */ 							KW_INIT,
    /** Token memberType: keyword is.  */ 							KW_IS,
    /** Token memberType: keyword override.  */ 						KW_OVERRIDE,
    /** Token memberType: keyword override.  */ 						KW_AS,
    /** Token memberType: keyword extension.  */ 						KW_EXTENSION,
    /** Token memberType: keyword final.  */ 						    KW_FINAL,
    /** Token memberType: keyword static.  */ 						KW_STATIC,
    /** Token memberType: keyword interface.  */ 						KW_INTERFACE,
}
