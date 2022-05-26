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

import java.util.Iterator;
import java.util.stream.IntStream;

import org.bukkit.event.Event;
import org.eclipse.jdt.annotation.Nullable;

import com.google.common.collect.Iterators;
import ch.njol.skript.Skript;
import ch.njol.skript.config.Node;
import ch.njol.skript.doc.NoDoc;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.skript.lang.util.SimpleLiteral;
import ch.njol.skript.log.SkriptLogger;
import ch.njol.util.Kleenean;

@NoDoc
public class ExprTimes extends SimpleExpression<Number> {

	static {
		Skript.registerExpression(ExprTimes.class, Number.class, ExpressionType.SIMPLE,
				"%number% time[s]", "once", "twice");
	}

	@SuppressWarnings("null")
	private Expression<Number> end;

	@SuppressWarnings("unchecked")
	@Override
	public boolean init(final Expression<?>[] exprs, final int matchedPattern, final Kleenean isDelayed, final ParseResult parseResult) {
		end = matchedPattern == 0 ? (Expression<Number>) exprs[0] : new SimpleLiteral<>(matchedPattern, false);
		
		if (end instanceof Literal) {
			int amount = ((Literal<Number>) end).getSingle().intValue();
			if (amount == 0 && isInLoop()) {
				Skript.warning("Looping zero times makes the code inside of the loop useless");
			} else if (amount == 1 & isInLoop()) {
				Skript.warning("Since you're looping exactly one time, you could simply remove the loop instead");
			} else if (amount < 0) {
				if (isInLoop())
					Skript.error("Looping a negative amount of times is impossible");
				else
					Skript.error("The times expression only supports positive numbers");
				return false;
			}
		}
		return true;
	}
	
	private boolean isInLoop() {
		Node node = SkriptLogger.getNode();
		if (node == null) {
			return false;
		}
		String key = node.getKey();
		if (key == null) {
			return false;
		}
		return key.startsWith("loop ");
	}

	@Nullable
	@Override
	protected Number[] get(final Event e) {
		Iterator<? extends Integer> iter = iterator(e);
		if (iter == null) {
			return null;
		}
		return Iterators.toArray(iter, Integer.class);
	}

	@Nullable
	@Override
	public Iterator<? extends Integer> iterator(final Event e) {
		Number end = this.end.getSingle(e);
		if (end == null) {
			return null;
		}
		return IntStream.range(1, end.intValue() + 1).iterator();
	}

	@Override
	public boolean isSingle() {
		return false;
	}

	@Override
	public Class<? extends Number> getReturnType() {
		return Number.class;
	}

	@Override
	public String toString(final @Nullable Event e, final boolean debug) {
		return end.toString(e, debug) + " times";
	}
}
