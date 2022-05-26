/**
 *   This file is part of Skript.
 *
 *  Skript is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Skript is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Skript.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Copyright Peter Güttinger, SkriptLang team and contributors
 */
package ch.njol.skript.expressions;

import java.util.Map;

import org.bukkit.event.Event;
import org.eclipse.jdt.annotation.Nullable;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionList;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.lang.UnparsedLiteral;
import ch.njol.skript.lang.Variable;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.skript.util.LiteralUtils;
import ch.njol.util.Kleenean;

/**
 * TODO should 'amount of [item]' return the size of the stack?
 * 
 * @author Peter Güttinger
 */
@Name("Amount")
@Description({"The amount of something.",
		"Please note that <code>amount of %items%</code> will not return the number of items, but the number of stacks, e.g. 1 for a stack of 64 torches. To get the amount of items in a stack, see the <a href='#ExprItemAmount'>item amount</a> expression.",
		"",
		"Also, you can get the recursive size of a list, which will return the recursive size of the list with sublists included, e.g.",
		"{list::*} Structure",
		"    ├──── {list::1}: 1",
		"    ├──── {list::2}: 2",
		"    │         ├──── {list::2::1}: 3",
		"    │         │           └──── {list::2::1::1}: 4",
		"    │         └──── {list::2::2}: 5",
		"    └──── {list::3}: 6",
		"Where using %size of {list::*}% will only return 3 (the first layer of indices only), while %recursive size of {list::*}% will return 6 (the entire list)",
		"Please note that getting a list's recursive size can cause lag if the list is large, so only use this expression if you need to!"})
@Examples({"message \"There are %number of all players% players online!\""})
@Since("1.0")
public class ExprAmount extends SimpleExpression<Number> {

	static {
		Skript.registerExpression(ExprAmount.class, Number.class, ExpressionType.PROPERTY,
				"(amount|number|size) of %objects%",
				"recursive (amount|number|size) of %objects%");
	}

	@SuppressWarnings("null")
	private ExpressionList<?> exprs;

	private boolean recursive;

	@Override
	@SuppressWarnings({"null", "unchecked"})
	public boolean init(final Expression<?>[] exprs, final int matchedPattern, final Kleenean isDelayed, final ParseResult parseResult) {
		this.exprs = exprs[0] instanceof ExpressionList ? (ExpressionList<?>) exprs[0] : new ExpressionList<>(new Expression<?>[]{exprs[0]}, Object.class, false);
		this.recursive = matchedPattern == 1;
		for (Expression<?> expr : this.exprs.getExpressions()) {
			if (expr instanceof Literal<?>) {
				return false;
			}
			if (expr.isSingle()) {
				Skript.error("'" + expr.toString(null, false) + "' can only ever have one value at most, thus the 'amount of ...' expression is useless. Use '... exists' instead to find out whether the expression has a value.");
				return false;
			}
			if (recursive && !(expr instanceof Variable<?>)) {
				Skript.error("Getting the recursive size of a list only applies to variables, thus the '" + expr.toString(null, false) + "' expression is useless.");
				return false;
			}
		}
		return true;
	}

	@Override
	@SuppressWarnings("unchecked")
	protected Number[] get(final Event e) {
		if (recursive) {
			int currentSize = 0;
			for (Expression<?> expr : exprs.getExpressions()) {
				Object var = ((Variable<?>) expr).getRaw(e);
				if (var != null) { // Should already be a map
					currentSize += getRecursiveSize((Map<String, ?>) var);
				}
			}
			return new Number[]{currentSize};
		}
		return new Number[]{exprs.getArray(e).length};
	}

	@SuppressWarnings("unchecked")
	private static int getRecursiveSize(Map<String, ?> map) {
		int count = 0;
		for (Map.Entry<String, ?> entry : map.entrySet()) {
			Object value = entry.getValue();
			if (value instanceof Map)
				count += getRecursiveSize((Map<String, ?>) value);
			else
				count++;
		}
		return count;
	}

	@Override
	public boolean isSingle() {
		return true;
	}

	@Override
	public Class<? extends Number> getReturnType() {
		return Number.class;
	}

	@Override
	public String toString(final @Nullable Event e, final boolean debug) {
		return (recursive ? "recursize size of " : "amount of ") + exprs.toString(e, debug);
	}

}
