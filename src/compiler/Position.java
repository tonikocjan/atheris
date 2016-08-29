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

/**
 * Doloca polozaj dela izvornega besedila v izvorni datoteki.
 * Position description in source code.
 * @author toni kocjan
 */
public class Position {

	/** Start line. */
	private final int begLine;
	/** Start column. */
	private final int begColumn;

	/** End line. */
	private final int endLine;
	/** EndColumn */
	private final int endColumn;

	/**
	 * Create new position.
	 * 
	 * @param begLine
	 *            Start line.
	 * @param begColumn
	 *            Start column.
	 * @param endLine
	 *            End line.
	 * @param endColumn
	 *            End column.
	 */
	public Position(int begLine, int begColumn, int endLine, int endColumn) {
		this.begLine = begLine;
		this.begColumn = begColumn;
		this.endLine = endLine;
		this.endColumn = endColumn;
	}

	/**
	 * Create new position.
	 * 
	 * @param line
	 *            Line of the character's position.
	 * @param column
	 *            Column of the character's position.
	 */
	public Position(int line, int column) {
		this(line, column, line, column);
	}

	/**
	 * Create new position based on first part's position and last part's position.
	 * 
	 * @param begPos
	 *            Start position.
	 * @param endPos
	 *            End position.
	 */
	public Position(Position begPos, Position endPos) {
		this.begLine = begPos.begLine;
		this.begColumn = begPos.begColumn;
		this.endLine = endPos.endLine;
		this.endColumn = endPos.endColumn;
	}

	@Override
	public String toString() {
		return (begLine + ":" + begColumn + "-" + endLine + ":" + endColumn);
	}

}
