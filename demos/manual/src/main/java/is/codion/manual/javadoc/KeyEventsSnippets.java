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
package is.codion.manual.javadoc;

import is.codion.swing.common.ui.key.KeyEvents;

import javax.swing.Action;
import javax.swing.JTextField;

import static is.codion.swing.common.ui.key.KeyEvents.MENU_SHORTCUT_MASK;
import static java.awt.event.KeyEvent.VK_DOWN;
import static javax.swing.JComponent.WHEN_FOCUSED;

/**
 * The {@link KeyEvents} javadoc snippets, each the region of the same name.
 */
final class KeyEventsSnippets {

	void usage(Action findNext) {
		JTextField textField = new JTextField(); // @start region=usage

		KeyEvents.builder()
						.keyCode(VK_DOWN)
						.onKeyRelease(false)
						.modifiers(MENU_SHORTCUT_MASK)
						.condition(WHEN_FOCUSED)
						.action(findNext)
						.enable(textField); // @end
	}
}
