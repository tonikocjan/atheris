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
 * Fragment kode.
 * 
 * @author sliva
 */
public class ImcCodeChunk extends ImcChunk {

	/** Klicni zapis funkcije.  */
	public FrmFrame frame;

	/** Vmesna koda funkcije.  */
	public ImcStmt imcode;

	/** Linearna vmesna koda.  */
	public ImcSEQ lincode;

	/**
	 * Ustvari nov fragment kode.
	 * 
	 * @param frame Klicni zapis funkcije.
	 * @param imcode Vmesna koda funckije.
	 */
	public ImcCodeChunk(FrmFrame frame, ImcStmt imcode) {
		this.frame = frame;
		this.imcode = imcode;
		this.lincode = null;
	}

	@Override
	public void dump() {
		Logger.dump(0, "CODE CHUNK: entryLabel=" + frame.entryLabel.getName());
		Logger.dump(2, frame.toString());
		if (lincode == null) imcode.dump(2); else lincode.dump(2);
	}

    @Override
    public String name() {
        return frame.toString();
    }
}
