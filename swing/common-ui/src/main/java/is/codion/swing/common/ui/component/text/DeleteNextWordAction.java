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
 * Copyright (c) 2022 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.component.text;

import javax.swing.AbstractAction;
import javax.swing.JPasswordField;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import javax.swing.text.Utilities;
import java.awt.event.ActionEvent;

final class DeleteNextWordAction extends AbstractAction {

	@Override
	public void actionPerformed(ActionEvent actionEvent) {
		JTextComponent textComponent = (JTextComponent) actionEvent.getSource();
		Document document = textComponent.getDocument();
		int caretPosition = textComponent.getCaretPosition();
		try {
			int removeToPosition = textComponent instanceof JPasswordField ?
							document.getLength() ://special handling for passwords, just remove everything after cursor
							Utilities.getWordEnd(textComponent, caretPosition) - caretPosition;
			document.remove(caretPosition, removeToPosition);
		}
		catch (BadLocationException e) {
			throw new RuntimeException(e);
		}
	}
}
