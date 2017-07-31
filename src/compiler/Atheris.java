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

package compiler;

import utils.ArgumentParser;
import utils.Constants;
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

public class Atheris {

    /** Ime izvorne datoteke. */
    private String sourceFileName;

    /** Seznam vseh faz prevajalnika. */
    private static String allPhases = "(lexan|synan|ast|seman|frames|imcode|interpret)";

    /** Doloca zadnjo fazo prevajanja, ki se bo se izvedla. */
    private String execPhase = "interpret";

    /** Doloca faze, v katerih se bodo izpisali vmesni rezultati. */
    private String dumpPhases = "interpret";

    /**
     * Parse arguments.
     * @param args
     */
    private void parseArguments(String[] args) {
        ArgumentParser parser = new ArgumentParser(args);

        sourceFileName = parser.valueFor("sourceFileName");

        if (parser.containsArgument("phase")) {
            String phase = parser.valueFor("phase");

            if (phase.matches(allPhases)) {
                execPhase = phase;
                dumpPhases = phase;
            }
            else {
                Report.warning(LanguageManager.localize("error_uknown_phase", phase));
            }
        }

        if (parser.containsArgument("debug")) {
            Interpreter.debug = parser.valueFor("debug").equals(Constants.trueKeyword);
        }

        if (parser.containsArgument("stack_size")) {
            String size = parser.valueFor("stack_size");

            try {
                Interpreter.STACK_SIZE = Integer.parseInt(size);
            }
            catch(Exception e) {
                Report.warning(LanguageManager.localize("error_invalid_stack_size_parameter"));
            }
        }

        if (parser.containsArgument("testing")) {
            if (parser.valueFor("testing").equals(Constants.trueKeyword)) {
                File outputFile = new File(sourceFileName + ".out");
                try {
                    Interpreter.interpreterOutput = new PrintStream(outputFile);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void compile(String[] args) {
        LanguageManager.sharedManager.loadLocalization("Localize/en.lproj/Localizable.strings");
        System.out.println(LanguageManager.localize("general_compiler_name"));

        parseArguments(args);

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
