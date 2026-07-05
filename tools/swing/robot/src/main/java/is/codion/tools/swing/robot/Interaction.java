/*
 * This file is part of Codion.
 *
 * Codion is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Codion is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Codion.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (c) 2026, Björn Darri Sigurðsson.
 */
package is.codion.tools.swing.robot;

import org.jspecify.annotations.Nullable;

/**
 * The result of a verified keyboard interaction: what was requested, and what the JVM's key event
 * stream shows actually happened. Observed via a {@link java.awt.KeyEventPostProcessor}, so
 * {@link Delivery} reflects whether a component actually consumed the event.
 */
public final class Interaction {

	/**
	 * The observed outcome of an interaction.
	 */
	public enum Delivery {
		/**
		 * The event was observed and consumed by a component, in other words it did something.
		 */
		CONSUMED,
		/**
		 * The event was observed but not consumed, in other words it fell through and most likely did nothing.
		 */
		FELL_THROUGH,
		/**
		 * No matching event was observed, in other words the keystroke did not go through.
		 */
		MISSED
	}

	private final String keyStroke;
	private final Delivery delivery;
	private final @Nullable String component;
	private final @Nullable String action;

	/**
	 * @param keyStroke the requested keystroke, or the typed text
	 * @param delivery whether the interaction was observed and consumed
	 * @param component the component that received the event, if observed
	 * @param action the action the keystroke resolves to in the receiving component's input maps, null if none
	 */
	public Interaction(String keyStroke, Delivery delivery, @Nullable String component, @Nullable String action) {
		this.keyStroke = keyStroke;
		this.delivery = delivery;
		this.component = component;
		this.action = action;
	}

	/**
	 * @return the requested keystroke, or the typed text
	 */
	public String keyStroke() {
		return keyStroke;
	}

	/**
	 * @return whether the interaction was observed and consumed
	 */
	public Delivery delivery() {
		return delivery;
	}

	/**
	 * @return the component that received the event, if observed
	 */
	public @Nullable String component() {
		return component;
	}

	/**
	 * @return the action the keystroke resolves to in the receiving component's input maps, null if none
	 */
	public @Nullable String action() {
		return action;
	}
}
