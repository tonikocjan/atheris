package compiler.lexan;

/**
 * Token types.
 * 
 * @author toni kocjan
 */
public enum TokenEnum {

	/** Token type: konec datoteke. */							EOF,
	
	/** Token type: ime. */										IDENTIFIER,

	/** Token type: logicna konstanta. */						LOG_CONST,
	/** Token type: celo stevilo. */							INT_CONST,
	/** Token type: niz. */										STR_CONST,
	/** Token type: decimal constant.  */	 					DOUBLE_CONST,
	/** Token type: char constant.  */		 					CHAR_CONST,

	/** Token type: logicni in. */								AND,
	/** Token type: logicni ali. */								IOR,
	/** Token type: logicni ne. */								NOT,
	/** Token type: je-enako. */								EQU,
	/** Token type: ni-enako. */								NEQ,
	/** Token type: manjse-kot. */								LTH,
	/** Token type: vecje-kot. */								GTH,
	/** Token type: manjse-ali-enako. */						LEQ,
	/** Token type: vecje-ali-enako. */							GEQ,
	
	/** Token type: celostevilsko mnozenje. */					MUL,
	/** Token type: celostevilsko deljenje. */					DIV,
	/** Token type: ostanek po celostevilskem deljenju. */		MOD,
	/** Token type: celostevilsko sestevanje ali predznak. */	ADD,
	/** Token type: celostevilsko odstevanje ali predznak. */	SUB,
		
	/** Token type: levi oklepaj. */							LPARENT,
	/** Token type: desni oklepaj. */							RPARENT,
	/** Token type: levi oglati oklepaj. */						LBRACKET,
	/** Token type: desni oglati oklepaj. */					RBRACKET,
	/** Token type: levi zaviti oklepaj. */						LBRACE,
	/** Token type: desni zaviti oklepaj. */					RBRACE,
	
	/** Token type: pika. */									DOT,
	/** Token type: dvopicje. */								COLON,
	/** Token type: podpicje. */								SEMIC,
	/** Token type: vejica. */                                  COMMA,
	/** Token type: nova vrstica.  */ 							NEWLINE,
	/** Token type: puščica ->.  */ 							ARROW,
	
	/** Token type: prirejanje. */								ASSIGN,
	
	/** Token type: tip integer.  */							INTEGER,
	/** Token type: tip string.  */								STRING,
	/** Token type: tip double.  */								DOUBLE,
	/** Token type: tip bool.  */								BOOL,
	/** Token type: tip char.  */								CHAR,
	/** Token type: tip void.  */								VOID,
	
	/** Token type: kljucna beseda else.  */					KW_ELSE,
	/** Token type: kljucna beseda for.  */						KW_FOR,
	/** Token type: kljucna beseda fun.  */						KW_FUN,
	/** Token type: kljucna beseda if.  */						KW_IF,
	/** Token type: kljucna beseda var.  */						KW_VAR,
	/** Token type: kljucna beseda while.  */					KW_WHILE,
	/** Token type: keyword struct.  */							KW_STRUCT,
	/** Token type: keyword import.  */ 						KW_IMPORT,
	/** Token type: keyword let.  */ 							KW_LET,
	/** Token type: keyword nil.  */ 							KW_NIL,
	/** Token type: keyword self.  */ 							KW_SELF,
	/** Token type: keyword class.  */ 							KW_CLASS,
	/** Token type: keyword in.  */ 							KW_IN,
	/** Token type: keyword return.  */ 						KW_RETURN,
	/** Token type: keyword public.  */ 						KW_PUBLIC,
	/** Token type: keyword private.  */ 						KW_PRIVATE,
	/** Token type: keyword continue.  */ 						KW_CONTINUE,
	/** Token type: keyword break.  */ 							KW_BREAK,
	/** Token type: keyword switch.  */ 						KW_SWITCH,
	/** Token type: keyword case.  */ 							KW_CASE,
	/** Token type: keyword default.  */ 						KW_DEFAULT,
	/** Token type: keyword enum.  */ 							KW_ENUM,
}
