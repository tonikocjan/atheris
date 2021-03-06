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

import compiler.frames.*;
import compiler.logger.Logger;
import compiler.logger.LoggerFactory;
import compiler.logger.LoggerInterface;

public class ImcCodeChunk extends ImcChunk {

    LoggerInterface logger = LoggerFactory.logger();

	public FrmFrame frame;
    public ImcStmt imcode;
    public ImcSEQ lincode;

	public ImcCodeChunk(FrmFrame frame, ImcStmt imcode) {
		this.frame = frame;
		this.imcode = imcode;
		this.lincode = null;
	}

	public void linearize() {
	    this.lincode = imcode.linear();
    }

    public FrmFrame getFrame() {
        return frame;
    }

    public ImcSEQ getLincode() {
        return lincode;
    }

    public ImcStmt getImcode() {
        return imcode;
    }

    @Override
	public void dump() {
        logger.dump(0, "CODE CHUNK: entryLabel=" + frame.entryLabel.getName());
        logger.dump(2, frame.toString());
		if (lincode == null) imcode.dump(2); else lincode.dump(2);
	}

    @Override
    public String name() {
        return frame.toString();
    }

    @Override
    public String toString() {
        return name();
    }
}
