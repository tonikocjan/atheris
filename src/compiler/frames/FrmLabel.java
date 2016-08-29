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

package compiler.frames;

/**
 * Opis labele v programu.
 * 
 * @author sliva
 */
public class FrmLabel {

	/** Ime labele.  */
	private String name;

	/**
	 * Ustvari novo labelo.
	 * 
	 * @param name Ime labele.
	 */
	private FrmLabel(String name) {
		this.name = name;
	}

	@Override
	public boolean equals(Object l) {
		return name == ((FrmLabel)l).name;
	}

	/**
	 * Vrne ime labele.
	 *
	 * @return Ime labele.
	 */
	public String name() {
		return name;
	}
	
	/** Stevec anonimnih label.  */
	private static int label_count = 0;
	
	/**
	 * toString
	 */
	public String toString() {
		return name;
	}

	/** 
	 * Vrne novo anonimno labelo.
	 *
	 * @return Nova anonimna labela.
	 */
	public static FrmLabel newLabel() {
		return new FrmLabel("L" + (label_count++));
	}

	/**
	 * Vrne novo poimenovano labelo.
	 * 
	 * @param name Ime nove poimenovane labele.
	 * @return Nova poimenovana labela.
	 */
	public static FrmLabel newLabel(String name) {
		return new FrmLabel("_" + name);
	}

}
