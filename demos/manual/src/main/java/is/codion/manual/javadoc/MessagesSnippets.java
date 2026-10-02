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

import is.codion.common.i18n.Messages;

import javax.swing.JButton;

/**
 * The {@link Messages} javadoc snippets, each the region of the same name.
 */
final class MessagesSnippets {

	void usage() {
		// Get localized messages // @start region=usage
		String cancelText = Messages.cancel();
		String okText = Messages.ok();

		// Get mnemonics for keyboard navigation
		char cancelMnemonic = Messages.cancelMnemonic();
		char clearMnemonic = Messages.clearMnemonic();

		// Use in UI components
		JButton cancelButton = new JButton(Messages.cancel());
		cancelButton.setMnemonic(Messages.cancelMnemonic()); // @end
	}
}
