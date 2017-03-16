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

package compiler.abstr.tree;

import compiler.abstr.tree.expr.AbsExpr;

/**
 * Simple structure holding condition and body which is executed when condition is positive.
 */
public class Condition {
	
	/** Condition. */
	public final AbsExpr cond;
	
	/** Positive branch */
	public final AbsStmts body;
	
	public Condition(AbsExpr cond, AbsStmts body) {
		this.cond = cond;
		this.body = body;
	}
	
}