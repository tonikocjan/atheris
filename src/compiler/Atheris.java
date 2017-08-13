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

import compiler.frames.FrameDescriptionMap;
import compiler.imcode.*;
import compiler.seman.*;
import utils.ArgumentParser;
import utils.Constants;
import compiler.ast.Ast;
import compiler.ast.tree.AbsTree;
import compiler.frames.PrintAstVisitor;
import compiler.frames.FrameDescription;
import compiler.frames.FrmEvaluator;
import compiler.interpreter.Interpreter;
import compiler.lexan.LexAn;
import compiler.lexan.TokenType;
import compiler.lincode.CodeGenerator;
import compiler.seman.type.Type;
import compiler.synan.SynAn;
import managers.LanguageManager;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;

public class Atheris {

    private String sourceFileName;
    private static String allPhases = "(lexan|synan|ast|seman|frames|imcode|interpret)";
    private String execPhase = "interpret";
    private String dumpPhases = "interpret";

    private ImcCodeChunk compiledCode;

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
                Logger.warning(LanguageManager.localize("error_uknown_phase", phase));
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
                Logger.warning(LanguageManager.localize("error_invalid_stack_size_parameter"));
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

    public Atheris compile(String[] args) {
        Long startTime = System.currentTimeMillis();

        LanguageManager.sharedManager.loadLocalization("Localize/en.lproj/Localizable.strings");
        System.out.println(LanguageManager.localize("general_compiler_name"));

        parseArguments(args);

        if (sourceFileName == null) {
            Logger.error(LanguageManager.localize("error_source_file_not_specified"));
        }

        if (dumpPhases != null) {
            Logger.openDumpFile(sourceFileName);
        }

        Logger.fileName = sourceFileName;

        Interpreter.clean();
        ImcCodeChunk mainCodeChunk = null;

        SymbolDescriptionMap symbolDescription = new SymbolDescription();
        SymbolTableMap symbolTable = new SymbolTable(symbolDescription);
        FrameDescriptionMap frameDescription = new FrameDescription();
        ImcDescriptionMap imcDescription = new ImcDescription();

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
            PrintAstVisitor semAn = new PrintAstVisitor(dumpPhases.contains("seman"), symbolTable, symbolDescription, frameDescription);
            source.accept(new NameChecker(symbolTable, symbolDescription));
            source.accept(new TypeChecker(symbolTable, symbolDescription));
            semAn.dump(source);
//			source.accept(new InitializationChecker());
            if (execPhase.equals("seman")) break;

            // Klicni zapisi.
            PrintAstVisitor frames = new PrintAstVisitor(dumpPhases.contains("frames"), symbolTable, symbolDescription, frameDescription);
            FrmEvaluator frmEval = new FrmEvaluator(symbolTable, symbolDescription, frameDescription);
            source.accept(frmEval);
            frames.dump(source);
            if (execPhase.equals("frames")) break;

            // Generiranje vmesne kode.
            ImcCodeGen imcodegen = new ImcCodeGen(frmEval.entryPoint, symbolTable, symbolDescription, frameDescription, imcDescription);
            source.accept(imcodegen);

            // Linearizacija vmesne kode.
            mainCodeChunk = new CodeGenerator(frameDescription).linearize(imcodegen.chunks);
            if (mainCodeChunk == null) {
                mainCodeChunk = imcodegen.entryPointCode;
            }

            ImCode imcode = new ImCode(dumpPhases.contains("imcode"));
            imcode.dump(imcodegen.chunks);
            if (execPhase.equals("imcode")) break;

            // clean-up
            Type.clean();

            compilationTime(startTime);

            // Izvajanje linearizirane vmesne kode
            if (execPhase.equals("interpret")) break;

            // Neznana faza prevajanja.
            if (!execPhase.equals(""))
                Logger.warning(LanguageManager.localize("error_uknown_phase", execPhase));
        }

        this.compiledCode = mainCodeChunk;

        return this;
    }

    public Atheris execute() {
        if (compiledCode == null) Logger.error("Compile source code first!");

        System.out.println(LanguageManager.localize("general_executing_file", sourceFileName));

        Interpreter.stM(Interpreter.getFP() + Constants.Byte, 0);
        new Interpreter(compiledCode.frame, compiledCode.lincode);

        return this;
    }

    public void exit() {
        if (dumpPhases != null) Logger.closeDumpFile();
    }

    private void compilationTime(Long startTime) {
        Long deltaTime = System.currentTimeMillis() - startTime;
        System.out.println(String.format("Compiled in: %dms", deltaTime));
    }
}
