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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.bukkit.event.Event;
import org.eclipse.jdt.annotation.Nullable;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;

@Name("Shuffled List")
@Description("Shuffles given list randomly. This is done by replacing indices by random numbers in resulting list.")
@Examples({"set {_list::*} to shuffled {_list::*}"})
@Since("2.2-dev32")
public class ExprShuffledList extends SimpleExpression<Object> {

	static{
		Skript.registerExpression(ExprShuffledList.class, Object.class, ExpressionType.COMBINED, "shuffled %objects%");
	}

	@SuppressWarnings("null")
	private Expression<?> list;

	@Override
	@SuppressWarnings({"null", "unchecked"})
	public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, ParseResult parseResult) {
		list = exprs[0].getConvertedExpression(Object.class);
		return list != null;
	}

	@Override
	@Nullable
	protected Object[] get(Event e) {
		Object[] origin = list.getAll(e);
		List<Object> shuffled = Arrays.asList(origin.clone()); // Not yet shuffled...
		
		try {
			Collections.shuffle(shuffled);
		} catch (IllegalArgumentException ex) { // In case elements are not comparable
			Skript.error("Tried to sort a list, but some objects are not comparable!");
		}
		return shuffled.toArray();
	}

	@Override
	public Class<? extends Object> getReturnType() {
		return Object.class;
	}

	@Override
	public boolean isSingle() {
		return false;
	}

	@Override
	public String toString(@Nullable Event e, boolean debug) {
		return "shuffled " + list.toString(e, debug);
	}

}
