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

import java.io.FileNotFoundException;
import java.io.PrintStream;

/**
 * Obvescanje o poteku prevajanja, izpis opozoril in obvestil o napakah.
 * <p>
 * Obvestila o poteku prevajanja so namenjena obvescanju programerja o tem, kaj
 * se dogaja med prevajanjem in jih je mozno izklopiti z ustrezno nastavitvijo
 * spremenljivke {@link compiler.Report#reporting}. Opozorila o napakah so se
 * izpisejo, kadar je mozno nadaljevati prevajanje, obvestila o napakah pa so
 * uporabljena takrat, ko nadaljevanje prevajanja ni mozno.
 * 
 * @author sliva
 */
public class Report {

	/** Doloca, ali se obvestila o poteku prevajanja izpisujejo ali ne. */
	public static boolean reporting = true;
	
	/** Ime datoteke, ki jo trenutno prevajalnik prevaja */
	public static String fileName = null;

	/**
	 * Izpise obvestilo o poteku prevajanja.
	 * 
	 * @param message
	 *            Obvestilo o poteku prevajanja.
	 */
	public static void report(String message) {
		if (reporting)
			System.err.println(":-) " + message);
	}

	/**
	 * Izpise obvestilo o poteku prevajanja, ki je vezano na del izvorne kode.
	 * 
	 * @param pos
	 *            Polozaj dela izvorne kode, na katerega se nanasa obvestilo.
	 * @param message
	 *            Obvestilo o poteku prevajanja.
	 */
	public static void report(Position pos, String message) {
		report("[" + pos.toString() + "] " + message);
	}

	/**
	 * Izpise opozorilo o napaki.
	 * 
	 * @param message
	 *            Opozorilo o napaki.
	 */
	public static void warning(String message) {
		if (fileName != null)
			message = fileName + ":" + message;
		
		System.err.println(":-o " + message);
	}

	/**
	 * Izpise opozorilo o napaki, ki je vezana na del izvorne kode.
	 * 
	 * @param pos
	 *            Polozaj dela izvorne kode, na katerega se nanasa opozorilo o
	 *            napaki.
	 * @param message
	 *            Opozorilo o napaki.
	 */
	public static void warning(Position pos, String message) {
		warning("[" + pos.toString() + "] " + message);
	}

	/**
	 * Izpise opozorilo o napaki, ki je vezana na znak izvorne kode.
	 * 
	 * @param line
	 *            Vrstica znaka, na katerega se nanasa opozorilo o napaki.
	 * @param column
	 *            Stolpec znaka, na katerega se nanasa opozorilo o napaki.
	 * @param message
	 *            Opozorilo o napaki.
	 */
	public static void warning(int line, int column, String message) {
		warning(new Position(line, column), message);
	}

	/**
	 * Izpise obvestilo o napaki in konca prevajanje.
	 * 
	 * @param message
	 *            Obvestilo o napaki.
	 */
	public static void error(String message) {
		if (fileName != null)
			message = fileName + ": " + message;
		
		System.err.println(":-( " + message);
		System.exit(1);
	}

	/**
	 * Izpise obvestilo o napaki, ki je vezana na del izvorne kode, in konca
	 * prevajanje.
	 * 
	 * @param pos
	 *            Polozaj dela izvorne kode, na katerega se nanasa obvestilo o
	 *            napaki.
	 * @param message
	 *            Obvestilo o napaki.
	 */
	public static void error(Position pos, String message) {
		error("[" + pos.toString() + "] " + message);
	}

	/**
	 * Izpise obvestilo o napaki, ki je vezana na znak izvorne kode, in konca
	 * prevajanje.
	 * 
	 * @param line
	 *            Vrstica znaka, na katerega se nanasa opozorilo o napaki.
	 * @param column
	 *            Stolpec znaka, na katerega se nanasa opozorilo o napaki.
	 * @param message
	 *            obvestilo o napaki.
	 */
	public static void error(int line, int column, String message) {
		error(new Position(line, column), message);
	}
	
	/** Datoteka z vmesnimi rezultati prevajanja. */
	private static PrintStream dumpFile = null;

	/**
	 * Odpre datoteko z vmesnimi rezultati.
	 * 
	 * @param sourceFileName
	 *            Ime datoteke z vmesnimi rezultati.
	 */
	public static void openDumpFile(String sourceFileName) {
		String dumpFileName = sourceFileName.replaceFirst("\\.prev$", "") + ".log";
		try {
			dumpFile = new PrintStream(dumpFileName);
		} catch (FileNotFoundException __) {
			Report.warning("Cannot produce dump file '" + dumpFileName + "'.");
		}
	}
	
	/**
	 * Zapre datoteko z vemsnimi rezultati.
	 */
	public static void closeDumpFile() {
		dumpFile.close();
		dumpFile = null;
	}
	
	/**
	 * Vrne datoteko z vmesnimi rezultati.
	 * 
	 * @return Datoteka z vmesnimi rezultati.
	 */
	public static PrintStream dumpFile() {
		if (dumpFile == null)
			Report.error ("Internal error: compiler.Report.dumpFile().");
		return dumpFile;
	}
	
	/**
	 * Izpise vrstico na datoteko z vmesnimi rezultati zamknjeno v desno.
	 * 
	 * @param indent
	 *            Sirina zamika.
	 * @param line
	 *            Vsebina vrstice.
	 */
	public static void dump(int indent, String line) {
		for (int i = 0; i < indent; i++) dumpFile.print(" ");
		dumpFile.println(line);
	}
}
