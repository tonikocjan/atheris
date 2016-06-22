package compiler.lexan;

/**
 * Definicije vrst besed.
 * 
 * @author sliva
 */
public enum Token {

	/** Vrsta simbola: konec datoteke. */							EOF,
	
	/** Vrsta simbola: ime. */										IDENTIFIER,

	/** Vrsta simbola: logicna konstanta. */						LOG_CONST,
	/** Vrsta simbola: celo stevilo. */								INT_CONST,
	/** Vrsta simbola: niz. */										STR_CONST,
	/** Vrsta simbola: ključna beseda import.  */ 					DOUBLE_CONST,
	/** Vrsta simbola: ključna beseda import.  */ 					CHAR_CONST,

	/** Vrsta simbola: logicni in. */								AND,
	/** Vrsta simbola: logicni ali. */								IOR,
	/** Vrsta simbola: logicni ne. */								NOT,
	
	/** Vrsta simbola: je-enako. */									EQU,
	/** Vrsta simbola: ni-enako. */									NEQ,
	/** Vrsta simbola: manjse-kot. */								LTH,
	/** Vrsta simbola: vecje-kot. */								GTH,
	/** Vrsta simbola: manjse-ali-enako. */							LEQ,
	/** Vrsta simbola: vecje-ali-enako. */							GEQ,
	
	/** Vrsta simbola: celostevilsko mnozenje. */					MUL,
	/** Vrsta simbola: celostevilsko deljenje. */					DIV,
	/** Vrsta simbola: ostanek po celostevilskem deljenju. */		MOD,
	/** Vrsta simbola: celostevilsko sestevanje ali predznak. */	ADD,
	/** Vrsta simbola: celostevilsko odstevanje ali predznak. */	SUB,
		
	/** Vrsta simbola: levi oklepaj. */								LPARENT,
	/** Vrsta simbola: desni oklepaj. */							RPARENT,
	/** Vrsta simbola: levi oglati oklepaj. */						LBRACKET,
	/** Vrsta simbola: desni oglati oklepaj. */						RBRACKET,
	/** Vrsta simbola: levi zaviti oklepaj. */						LBRACE,
	/** Vrsta simbola: desni zaviti oklepaj. */						RBRACE,
	
	/** Vrsta simbola: pika. */										DOT,
	/** Vrsta simbola: dvopicje. */									COLON,
	/** Vrsta simbola: podpicje. */									SEMIC,
	/** Vrsta simbola: vejica. */                                   COMMA,
	/** Vrsta simbola: ključna beseda import.  */ 					NEWLINE,
	
	/** Vrsta simbola: prirejanje. */								ASSIGN,
	
	/** Vrsta simbola: tip integer.  */								INTEGER,
	/** Vrsta simbola: tip string.  */								STRING,
	/** Vrsta simbola: tip double.  */								DOUBLE,
	/** Vrsta simbola: tip bool.  */								BOOL,
	/** Vrsta simbola: tip char.  */								CHAR,
	/** Vrsta simbola: tip char.  */								VOID,
	
	/** Vrsta simbola: kljucna beseda else.  */						KW_ELSE,
	/** Vrsta simbola: kljucna beseda for.  */						KW_FOR,
	/** Vrsta simbola: kljucna beseda fun.  */						KW_FUN,
	/** Vrsta simbola: kljucna beseda if.  */						KW_IF,
	/** Vrsta simbola: kljucna beseda var.  */						KW_VAR,
	/** Vrsta simbola: kljucna beseda while.  */					KW_WHILE,
	/** Vrsta simbola: ključna beseda struct.  */					KW_STRUCT,
	/** Vrsta simbola: ključna beseda import.  */ 					KW_IMPORT,
	/** Vrsta simbola: ključna beseda import.  */ 					KW_LET,
	/** Vrsta simbola: ključna beseda import.  */ 					KW_NIL,
	/** Vrsta simbola: ključna beseda import.  */ 					KW_SELF,
	/** Vrsta simbola: ključna beseda import.  */ 					KW_CLASS,
	/** Vrsta simbola: ključna beseda import.  */ 					KW_IN,

}
