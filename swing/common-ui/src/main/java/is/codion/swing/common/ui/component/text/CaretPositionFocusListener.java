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
package is.codion.swing.common.ui.component.text;

import is.codion.swing.common.ui.component.text.TextComponentBuilder.CaretPosition;

import javax.swing.text.JTextComponent;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import static java.util.Objects.requireNonNull;

/**
 * Positions the caret when the text component gains the focus, unless by a mouse press, which leaves the caret
 * where pressed, or after a temporary loss, the window deactivated or a modal dialog shown for example,
 * which leaves it where it was.
 */
final class CaretPositionFocusListener extends MouseAdapter implements FocusListener {

	private final JTextComponent textComponent;
	private final CaretPosition caretPosition;

	private boolean mousePressed;
	private boolean temporaryLoss;

	private CaretPositionFocusListener(JTextComponent textComponent, CaretPosition caretPosition) {
		this.textComponent = textComponent;
		this.caretPosition = caretPosition;
	}

	/**
	 * @param textComponent the text component
	 * @param caretPosition the caret position on focus gained
	 */
	static void enable(JTextComponent textComponent, CaretPosition caretPosition) {
		CaretPositionFocusListener listener = new CaretPositionFocusListener(requireNonNull(textComponent), requireNonNull(caretPosition));
		textComponent.addMouseListener(listener);
		textComponent.addFocusListener(listener);
	}

	@Override
	public void mousePressed(MouseEvent e) {
		// The focus gained follows the press, which has positioned the caret
		mousePressed = !textComponent.hasFocus();
	}

	@Override
	public void focusGained(FocusEvent e) {
		if (!mousePressed && !temporaryLoss) {
			textComponent.setCaretPosition(caretPosition == CaretPosition.START ? 0 : textComponent.getDocument().getLength());
		}
		mousePressed = false;
		temporaryLoss = false;
	}

	@Override
	public void focusLost(FocusEvent e) {
		temporaryLoss = e.isTemporary();
	}
}
