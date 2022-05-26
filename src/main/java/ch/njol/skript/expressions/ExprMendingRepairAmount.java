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

import org.bukkit.event.Event;
import org.bukkit.event.player.PlayerItemMendEvent;
import org.eclipse.jdt.annotation.Nullable;

import ch.njol.skript.Skript;
import ch.njol.skript.bukkitutil.ItemUtils;
import ch.njol.skript.classes.Changer.ChangeMode;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.skript.log.ErrorQuality;
import ch.njol.util.Kleenean;
import ch.njol.util.coll.CollectionUtils;

@Name("Mending Repair Amount")
@Description({"The number of durability points an item is to be repaired in a mending event.",
			" Modifying the repair amount will affect how much experience is given to the player after mending."})
@Examples({"on item mend:",
		"\tset the mending repair amount to 100"})
@Since("2.5.1")
public class ExprMendingRepairAmount extends SimpleExpression<Number> {

	static {
		Skript.registerExpression(ExprMendingRepairAmount.class, Number.class, ExpressionType.SIMPLE, "[the] [mending] repair amount");
	}

	@Override
	public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, ParseResult parseResult) {
		if (!getParser().isCurrentEvent(PlayerItemMendEvent.class)) {
			Skript.error("The 'mending repair amount' is only usable in item mend events", ErrorQuality.SEMANTIC_ERROR);
			return false;
		}
		return true;
	}

	@Override
	protected Number[] get(final Event e) {
		return new Number[]{((PlayerItemMendEvent) e).getRepairAmount()};
	}

	@Nullable
	@Override
	public Class<?>[] acceptChange(ChangeMode mode) {
		switch (mode) {
			case SET:
			case ADD:
			case REMOVE:
			case RESET:
				return CollectionUtils.array(Number.class);
			default:
				return null;
		}
	}

	@Override
	public void change(Event event, @Nullable Object[] delta, ChangeMode mode) {
		PlayerItemMendEvent e = (PlayerItemMendEvent) event;
		int newLevel = delta != null ? ((Number) delta[0]).intValue() : 0;
		switch (mode) {
			case SET:
				break;
			case ADD:
				newLevel += e.getRepairAmount();
				break;
			case REMOVE:
				newLevel = e.getRepairAmount() - newLevel;
				break;
			case RESET:
				int repairAmount = e.getExperienceOrb().getExperience() * 2;
				int itemDamage = ItemUtils.getDamage(e.getItem());
				newLevel = Math.min(itemDamage, repairAmount);
				break;
			default:
				assert false;
		}
		e.setRepairAmount(newLevel);
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
		return "the mending repair amount";
	}

}
