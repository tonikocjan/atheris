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

import java.util.*;

import compiler.logger.LoggerFactory;
import compiler.logger.LoggerInterface;

public class ImCode {

    private static LoggerInterface logger = LoggerFactory.logger();

	private boolean dump;

	public ImCode(boolean dump) {
		this.dump = dump;
	}

	public void dump(List<ImcChunk> chunks) {
		if (! dump) return;
		if (logger.dumpFile() == null) return;
		for (int chunk = 0; chunk < chunks.size(); chunk++)
			chunks.get(chunk).dump();
	}
}
