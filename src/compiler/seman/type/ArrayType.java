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

package compiler.seman.type;

/**
 * Opis tabelaricnega tipa.
 * 
 * @author sliva
 */
public class ArrayType extends PointerType {

	/** Tip elementa. */
	public final Type type;

	/** Velikost tabele. */
	public final int count;

	/**
	 * Ustvari nov opis tabelaricnega tipa.
	 * 
	 * @param type
	 *            Tip elementa tabele.
	 * @param count
	 *            Velikost tabele.
	 */
	public ArrayType(Type type, int count) {
		this.type = type;
		this.count = count;
	}

	@Override
	public boolean sameStructureAs(Type type) {
		if (type.actualType().isArrayType()) {
			ArrayType listType = (ArrayType) (type.actualType());
			return (listType.type.sameStructureAs(this.type));
		}
		return false;
	}

	@Override
	public String toString() {
		return "LIST(" + type.toString() + ")";
	}

	@Override
	public int size() {
		return count * type.size();
	}

	@Override
	public boolean canCastTo(Type t) {
		return false;
	}

	@Override
	public boolean containsMember(String name) {
		return false;
	}

	@Override
	public String friendlyName() {
		return type.toString();
	}
}
