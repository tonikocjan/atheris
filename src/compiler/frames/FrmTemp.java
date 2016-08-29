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
 * Opis zacasne spremenljivke v programu.
 * 
 * @author sliva
 */
public class FrmTemp {

	/** Stevec zacasnih spremenljivk.  */
	private static int count = 0;

	/** Ime te zacasne spremenljivke.  */
	private int num;

	/**
	 * Ustvari novo zacasno spremenljivko.
	 */
	public FrmTemp() {
		num = count++;
	}

	/**
	 * Vrne ime zacasne spremenljivke.
	 * 
	 * @return Ime zacasne spremenljivke.
	 */
	public String name() {
		return "T" + num;
	}

	@Override
	public boolean equals(Object t) {
		return num == ((FrmTemp)t).num;
	}

}
