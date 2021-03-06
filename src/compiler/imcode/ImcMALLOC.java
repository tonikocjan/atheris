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

public class ImcMALLOC extends ImcExpr {

	/** Velikost pomnilnika */
	public final int size;
	
	public ImcMALLOC(int size) {
		this.size = size;
	}

	@Override
	public void dump(int indent) {
        logger.dump(indent, "MALLOC sizeInBytes: " + size + " BYTES");
	}

	@Override
	public ImcESEQ linear() {
		ImcESEQ eseq = new ImcESEQ(new ImcSEQ(), this);
		return eseq;
	}

    @Override
    public String toString() {
        return "ImcMALLOC";
    }

}
