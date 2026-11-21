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
 * Copyright (c) 2022 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.component.text;

import javax.swing.text.JTextComponent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;

final class SelectAllFocusListener extends FocusAdapter {

	/**
	 * FlatClientProperties.SELECT_ALL_ON_FOCUS_POLICY, FlatLaf's policy, which by default selects all
	 * each time a formatted text field gains the focus other than by a mouse press, after any focus listener has run.
	 */
	private static final String FLATLAF_SELECT_ALL_ON_FOCUS_POLICY = "JTextField.selectAllOnFocusPolicy";
	private static final String NEVER = "never";

	private final JTextComponent textComponent;

	SelectAllFocusListener(JTextComponent textComponent) {
		this.textComponent = textComponent;
	}

	/**
	 * Keeps the look and feel from selecting all on focus gained, leaving it to the builder, FlatLaf being the one
	 * known to, the client property inert elsewhere.
	 * @param textComponent the text component
	 */
	static void disableLookAndFeelSelectAll(JTextComponent textComponent) {
		textComponent.putClientProperty(FLATLAF_SELECT_ALL_ON_FOCUS_POLICY, NEVER);
	}

	@Override
	public void focusGained(FocusEvent e) {
		textComponent.selectAll();
	}

	@Override
	public void focusLost(FocusEvent e) {
		textComponent.select(0, 0);
	}
}
