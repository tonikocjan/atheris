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

package compiler.imcode;

import compiler.*;
import compiler.frames.*;

/**
 * Fragment podatkov.
 * 
 * @author sliva
 */
public class ImcDataChunk extends ImcChunk {

	/** Naslov spremenljivke v pomnilniku.  */
	public FrmLabel label;

	/** Velikost spremenljivke v pomnilniku.  */
	public int size;
	
	/**  Inicializacija spremenljivke v labeli */
	public Object data = null;

	/**
	 * Ustvari novfragment podatkov.
	 * 
	 * @param label Labela podatka.
	 * @param size Velikost podatka.
	 */
	public ImcDataChunk(FrmLabel label, int size) {
		this.label = label;
		this.size = size;
	}

	@Override
	public void dump() {
		String tmp =  data == null ? "" : " .BYTE: " + data;
		Logger.dump(0, "DATA CHUNK: entryLabel=" + label.getName() + " sizeInBytes=" + size + tmp);
	}

    @Override
    public String name() {
        return label.getName();
    }
}
