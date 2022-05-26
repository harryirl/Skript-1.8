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

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

import org.bukkit.event.Event;
import org.eclipse.jdt.annotation.NonNull;

import com.google.common.collect.Iterators;
import ch.njol.skript.Skript;
import ch.njol.skript.classes.ClassInfo;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Condition;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.skript.registrations.Converters;
import ch.njol.skript.util.LiteralUtils;
import ch.njol.skript.util.Utils;
import ch.njol.util.Kleenean;
import ch.njol.util.coll.iterator.ArrayIterator;

@Name("Filter")
@Description("Filters a list based on a condition. " +
		"For example, if you ran 'broadcast \"something\" and \"something else\" where [string input is \"something\"]', " +
		"only \"something\" would be broadcast as it is the only string that matched the condition.")
@Examples("send \"congrats on being staff!\" to all players where [player input has permission \"staff\"]")
@Since("2.2-dev36")
@SuppressWarnings({"null", "unchecked"})
public class ExprFilter extends SimpleExpression<Object> {

	private static ExprFilter parsing;

	static {
		Skript.registerExpression(ExprFilter.class, Object.class, ExpressionType.COMBINED,
				"%objects% (where|that match) \\[<.+>\\]");
	}

	private Object current;
	private List<ExprInput<?>> children = new ArrayList<>();
	private Condition condition;
	private String rawCond;
	private Expression<Object> objects;

	public static ExprFilter getParsing() {
		return parsing;
	}

	@Override
	public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, ParseResult parseResult) {
		try {
			parsing = this;
			objects = LiteralUtils.defendExpression(exprs[0]);
			rawCond = parseResult.regexes.get(0).group();
			condition = Condition.parse(rawCond, "Can't understand this condition: " + rawCond);
		} finally {
			parsing = null;
		}
		return condition != null && LiteralUtils.canInitSafely(objects);
	}

	@NonNull
	@Override
	public Iterator<?> iterator(Event e) {
		try {
			return Iterators.filter(new ArrayIterator<>(this.objects.getArray(e)), object -> {
				current = object;
				return condition.check(e);
			});
		} finally {
			current = null;
		}
	}

	@Override
	protected Object[] get(Event e) {
		try {
			return Converters.convertStrictly(Iterators.toArray(iterator(e), Object.class), getReturnType());
		} catch (ClassCastException e1) {
			return null;
		}
	}

	public Object getCurrent() {
		return current;
	}

	private void addChild(ExprInput<?> child) {
		children.add(child);
	}

	private void removeChild(ExprInput<?> child) {
		children.remove(child);
	}

	@Override
	public Class<?> getReturnType() {
		return objects.getReturnType();
	}

	@Override
	public boolean isSingle() {
		return objects.isSingle();
	}

	@Override
	public String toString(Event e, boolean debug) {
		return String.format("%s where [%s]", objects.toString(e, debug), rawCond);
	}

	@Override
	public boolean isLoopOf(String s) {
		for (ExprInput<?> child : children) { // if they used player input, let's assume loop-player is valid
			if (child.getClassInfo() == null || child.getClassInfo().getUserInputPatterns() == null)
				continue;

			for (Pattern pattern : child.getClassInfo().getUserInputPatterns()) {
				if (pattern.matcher(s).matches())
					return true;
			}
		}
		return objects.isLoopOf(s); // nothing matched, so we'll rely on the object expression's logic
	}

	@Name("Filter Input")
	@Description("Represents the input in a filter expression. " +
			"For example, if you ran 'broadcast \"something\" and \"something else\" where [input is \"something\"]" +
			"the condition would be checked twice, using \"something\" and \"something else\" as the inputs.")
	@Examples("send \"congrats on being staff!\" to all players where [input has permission \"staff\"]")
	@Since("2.2-dev36")
	public static class ExprInput<T> extends SimpleExpression<T> {

		static {
			Skript.registerExpression(ExprInput.class, Object.class, ExpressionType.COMBINED,
					"input",
					"%*classinfo% input"
			);
		}

		private ExprInput<?> source;
		private Class<T> superType;
		private ExprFilter parent;
		private ClassInfo<?> inputType;

	public ExprInput() {
			this(null, (Class<? extends T>) Object.class);
		}

		public ExprInput(ExprInput<?> source, Class<? extends T>... types) {
			this.source = source;
			if (source != null) {
				this.parent = source.parent;
				this.inputType = source.inputType;
				parent.removeChild(source);
				parent.addChild(this);
			}

			this.superType = (Class<T>) Utils.getSuperType(types);
		}

		@Override
		public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, ParseResult parseResult) {
			parent = ExprFilter.getParsing();

			if (parent == null)
				return false;

			parent.addChild(this);
			inputType = matchedPattern == 0 ? null : ((Literal<ClassInfo<?>>) exprs[0]).getSingle();
			return true;
		}

		@Override
		protected T[] get(Event e) {
			Object current = parent.getCurrent();
			if (inputType != null && !inputType.getC().isInstance(current)) {
				return null;
			}

			try {
				return Converters.convertStrictly(new Object[]{current}, superType);
			} catch (ClassCastException e1) {
				return (T[]) Array.newInstance(superType, 0);
			}
		}

		public void setParent(ExprFilter parent) {
			this.parent = parent;
		}

		@Override
		public <R> Expression<? extends R> getConvertedExpression(Class<R>... to) {
			return new ExprInput<>(this, to);
		}

		@Override
		public Expression<?> getSource() {
			return source == null ? this : source;
		}

		@Override
		public Class<? extends T> getReturnType() {
			return superType;
		}

		private ClassInfo<?> getClassInfo() {
			return inputType;
		}

		@Override
		public boolean isSingle() {
			return true;
		}

		@Override
		public String toString(Event e, boolean debug) {
			return inputType == null ? "input" : inputType.getCodeName() + " input";
		}

	}

}
