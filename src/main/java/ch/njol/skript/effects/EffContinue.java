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
package ch.njol.skript.effects;

import java.util.List;

import org.bukkit.event.Event;
import org.eclipse.jdt.annotation.Nullable;

import ch.njol.skript.ScriptLoader;
import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.Loop;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.TriggerItem;
import ch.njol.util.Kleenean;

@Name("Continue")
@Description("Skips the value currently being looped, moving on to the next value if it exists.")
@Examples({"loop all players:",
		"\tif loop-value does not have permission \"moderator\":",
		"\t\tcontinue # filter out non moderators",
		"\tbroadcast \"%loop-player% is a moderator!\" # Only moderators get broadcast"})
@Since("2.2-dev37")
public class EffContinue extends Effect {

	static {
		Skript.registerEffect(EffContinue.class, "continue [loop]");
	}

	@SuppressWarnings("null")
	private Loop loop;

	@Override
	protected void execute(Event e) {
		throw new UnsupportedOperationException();
	}

	@Nullable
	@Override
	protected TriggerItem walk(Event e) {
		TriggerItem.walk(loop, e);
		return null;
	}

	@Override
	public String toString(@Nullable Event e, boolean debug) {
		return "continue";
	}

	@Override
	public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
		List<Loop> loops = ScriptLoader.currentLoops;
		if (loops.isEmpty()) {
			Skript.error("Continue may only be used in loops");
			return false;
		}
		Loop loop = loops.get(loops.size() - 1); // The most recent loop
		assert loop != null;
		this.loop = loop;
		return true;
	}

}
