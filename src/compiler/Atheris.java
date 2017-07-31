package compiler;

import Utils.Constants;
import compiler.abstr.Ast;
import compiler.abstr.tree.AbsTree;
import compiler.frames.Frames;
import compiler.frames.FrmDesc;
import compiler.frames.FrmEvaluator;
import compiler.imcode.ImCode;
import compiler.imcode.ImcCodeChunk;
import compiler.imcode.ImcCodeGen;
import compiler.imcode.ImcDesc;
import compiler.interpreter.Interpreter;
import compiler.lexan.LexAn;
import compiler.lexan.TokenType;
import compiler.lincode.CodeGenerator;
import compiler.seman.NameChecker;
import compiler.seman.SymbDesc;
import compiler.seman.SymbTable;
import compiler.seman.TypeChecker;
import compiler.seman.type.Type;
import compiler.synan.SynAn;
import managers.LanguageManager;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;

/**
 * Created by Toni Kocjan on 31/07/2017.
 * Triglav Partner BE
 */
public class Atheris {

    /** Ime izvorne datoteke. */
    private String sourceFileName;

    /** Seznam vseh faz prevajalnika. */
    private static String allPhases = "(lexan|synan|ast|seman|frames|imcode|interpret)";

    /** Doloca zadnjo fazo prevajanja, ki se bo se izvedla. */
    private String execPhase = "interpret";

    /** Doloca faze, v katerih se bodo izpisali vmesni rezultati. */
    private String dumpPhases = "interpret";

    public void compile(String[] args) {
        LanguageManager.sharedManager.loadLocalization("Localize/en.lproj/Localizable.strings");

        System.out.println(LanguageManager.localize("general_compiler_name"));

        // TODO: - This should be separate method (separate class?)
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
                if (args[argc].startsWith("--testing=")) {
                    String bool = args[argc].substring("--testing=".length());
                    if (bool.equals(Constants.trueKeyword)) {
                        File outputFile = new File(sourceFileName + ".out");
                        try {
                            Interpreter.interpreterOutput = new PrintStream(outputFile);
                        } catch (FileNotFoundException e) {
                            e.printStackTrace();
                        }
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

        if (sourceFileName == null) {
            Report.error(LanguageManager.localize("error_source_file_not_specified"));
        }

        if (dumpPhases != null) {
            Report.openDumpFile(sourceFileName);
        }

        Report.fileName = sourceFileName;

        Interpreter.clean();

        // Izvajanje faz prevajanja.
        while (true) {
            // Leksikalna analiza.
            LexAn lexAn = new LexAn(sourceFileName, dumpPhases.contains("lexan"));
            if (execPhase.equals("lexan")) {
                while (lexAn.nextSymbol().token != TokenType.EOF) {}
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
            Frames semAn = new Frames(dumpPhases.contains("seman"));
            source.accept(new NameChecker());
            source.accept(new TypeChecker());
            semAn.dump(source);
//			source.accept(new InitializationChecker());
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
            Type.clean();

            System.out.println(LanguageManager.localize("general_executing_file", sourceFileName));
            System.out.flush();

//			Interpreter.printMemory();

            // Izvajanje linearizirane vmesne kode
            Interpreter.stM(Interpreter.getFP() + 4, 0);
            if (mainFrame != null)
                new Interpreter(mainFrame.frame, mainFrame.imcode.linear());
            else
                new Interpreter(frmEval.entryPoint, imcodegen.entryPointCode.lincode);

            if (execPhase.equals("interpret")) break;

            // Neznana faza prevajanja.
            if (!execPhase.equals(""))
                Report.warning(LanguageManager.localize("error_uknown_phase", execPhase));
        }

        // Zapiranje datoteke z vmesnimi rezultati.
        if (dumpPhases != null) Report.closeDumpFile();
    }
}
