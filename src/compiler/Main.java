package compiler;

import managers.LanguageManager;
import compiler.lincode.CodeGenerator;
import compiler.lexan.*;
import compiler.synan.*;
import compiler.abstr.*;
import compiler.abstr.tree.*;
import compiler.seman.*;
import compiler.frames.*;
import compiler.imcode.*;
import compiler.interpreter.Interpreter;

/**
 * Osnovni razred prevajalnika, ki vodi izvajanje celotnega procesa prevajanja.
 * 
 * @author Toni Kocjan
 */
public class Main {

	/** Ime izvorne datoteke. */
	private static String sourceFileName;

	/** Seznam vseh faz prevajalnika. */
	private static String allPhases = "(lexan|synan|ast|seman|frames|imcode|interpret)";

	/** Doloca zadnjo fazo prevajanja, ki se bo se izvedla. */
	private static String execPhase = "interpret";

	/** Doloca faze, v katerih se bodo izpisali vmesni rezultati. */
	private static String dumpPhases = "interpret";

	/**
	 * Metoda, ki izvede celotni proces prevajanja.
	 * 
	 * @param args
	 *            Parametri ukazne vrstice.
	 */
	public static void main(String[] args) {
		/**
		 * Load localization.
		 */
		LanguageManager.sharedManager.loadLocalization("Localize/en.lproj/Localizable.strings");
		
		/**
		 * Start compiling.
		 */
		System.out.println(LanguageManager.localize("general_compiler_name"));
		
		// Pregled ukazne vrstice.
		for (int argc = 0; argc < args.length; argc++) {
			if (args[argc].startsWith("--")) {
				// Stikalo v ukazni vrstici.
				if (args[argc].startsWith("--phase=")) {
					String phase = args[argc].substring("--phase=".length());
					if (phase.matches(allPhases)) {
						execPhase = phase;
						dumpPhases = phase;
					}
					else
						Report.warning(LanguageManager.localize("error_uknown_phase", phase));
					continue;
				}
				if (args[argc].startsWith("--debug=")) {
					String debug = args[argc].substring("--debug=".length());
					Interpreter.debug = debug.equals("true"); 
					continue;
				}
				if (args[argc].startsWith("--stack_size=")) {
					String size = args[argc].substring("--stack_size=".length());
					try {
						Interpreter.STACK_SIZE = Integer.parseInt(size);
					}
					catch(Exception e) {
						Report.warning(LanguageManager.localize("error_invalid_stack_size_parameter"));
					}
					continue;
				}
				// Neznano stikalo.
				Report.warning(LanguageManager.localize("error_unknown_switch"));
			} else {
				// Ime izvorne datoteke.
				if (sourceFileName == null)
					sourceFileName = args[argc];
				else
					Report.warning("Source file name '" + sourceFileName + "' ignored.");
			}
		}
		if (sourceFileName == null)
			Report.error(LanguageManager.localize("error_source_file_not_specified"));

		// Odpiranje datoteke z vmesnimi rezultati.
		if (dumpPhases != null) Report.openDumpFile(sourceFileName);
		
		Report.fileName = sourceFileName;

		// Izvajanje faz prevajanja.
		while (true) {
			// Leksikalna analiza.
			LexAn lexAn = new LexAn(sourceFileName, dumpPhases.contains("lexan"));
			if (execPhase.equals("lexan")) {
				while (lexAn.lexAn().token != Token.EOF) {
				}
				break;
			}
			
			// Sintaksna analiza.
			SynAn synAn = new SynAn(lexAn, dumpPhases.contains("synan"));
			AbsTree source = synAn.parse();
			if (execPhase.equals("synan")) break;
			
			// Abstraktna sintaksa.
			Ast ast = new Ast(dumpPhases.contains("ast"));
			ast.dump(source);
			if (execPhase.equals("ast")) break;
			
			// Semanticna analiza.
			SemAn semAn = new SemAn(dumpPhases.contains("seman"));
			NameChecker nc = new NameChecker();
			source.accept(nc);
			source.accept(new TypeChecker());
			semAn.dump(source);
			source.accept(new InitializationChecker());
			if (execPhase.equals("seman")) break;
			
			// Klicni zapisi.
			Frames frames = new Frames(dumpPhases.contains("frames"));
			FrmEvaluator frmEval = new FrmEvaluator();
			source.accept(frmEval);
			frames.dump(source);
			if (execPhase.equals("frames")) break;
			
			// Vmesna koda.
			ImCode imcode = new ImCode(dumpPhases.contains("imcode"));
			ImcCodeGen imcodegen = new ImcCodeGen(frmEval.entryPoint);
			source.accept(imcodegen);
			imcode.dump(imcodegen.chunks);
			if (execPhase.equals("imcode")) break;
			
			// Linearizacija vmesne kode
			ImcCodeChunk mainFrame = CodeGenerator.linearize(imcodegen.chunks);
			imcode = new ImCode(dumpPhases.contains("interpret"));
			imcode.dump(imcodegen.chunks);
			
			// clean-up
			SymbTable.clean();
			SymbDesc.clean();
			FrmDesc.clean();
			ImcDesc.clean();

			System.out.printf("");
			System.out.println(LanguageManager.localize("general_executing_file", sourceFileName));
			
			// Izvajanje linearizirane vmesne kode
			Interpreter.stM(Interpreter.getFP() + 4, 0);
			if (mainFrame != null)
				new Interpreter(mainFrame.frame, mainFrame.imcode.linear());
			else
				new Interpreter(frmEval.entryPoint, imcodegen.entryPointCode.imcode.linear());
			
			if (execPhase.equals("interpret")) break;
			
			// Neznana faza prevajanja.
			if (!execPhase.equals(""))
				Report.warning(LanguageManager.localize("error_uknown_phase", execPhase));
		}

		// Zapiranje datoteke z vmesnimi rezultati.
		if (dumpPhases != null) Report.closeDumpFile();

		System.exit(0);
	}
}
