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
package is.codion.swing.framework.ui;

import is.codion.swing.common.ui.control.ControlKey;
import is.codion.swing.common.ui.control.ControlMap;
import is.codion.swing.common.ui.key.KeyEvents;

import javax.swing.KeyStroke;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Binds controls to their key strokes in one place, rejecting two control keys with the same key stroke,
 * where one would silently replace the other.
 */
final class ControlKeyBindings {

	private final Map<KeyStroke, ControlKey<?>> bound = new HashMap<>();

	/**
	 * Binds the control associated with the given key, in case it has both a control and a key stroke.
	 * @param controlMap the control map
	 * @param controlKey the control key
	 * @param binding enables the key event
	 * @throws IllegalStateException in case another control key has been bound to the same key stroke
	 */
	void bind(ControlMap controlMap, ControlKey<?> controlKey, Consumer<KeyEvents.Builder> binding) {
		controlMap.keyEvent(controlKey).ifPresent(keyEvent -> {
			KeyStroke keyStroke = controlMap.keyStroke(controlKey).getOrThrow();
			ControlKey<?> boundKey = bound.putIfAbsent(keyStroke, controlKey);
			if (boundKey != null) {
				throw new IllegalStateException("Key stroke '" + keyStroke + "' assigned to both " + boundKey + " and " + controlKey);
			}
			binding.accept(keyEvent);
		});
	}
}
