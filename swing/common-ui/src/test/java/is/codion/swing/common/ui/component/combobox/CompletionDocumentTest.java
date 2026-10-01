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
package is.codion.swing.common.ui.component.combobox;

import is.codion.swing.common.model.component.combobox.SwingFilterComboBoxModel;
import is.codion.swing.common.ui.component.Components;
import is.codion.swing.common.ui.component.combobox.Completion.Mode;
import is.codion.swing.common.ui.component.value.ComponentValue;

import org.junit.jupiter.api.Test;

import javax.swing.JComboBox;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertEquals;

public final class CompletionDocumentTest {

	@Test
	void highlightedOnceTheEditorIsUpdated() throws Exception {
		for (Mode mode : asList(Mode.AUTOCOMPLETE, Mode.MAXIMUM_MATCH)) {
			SwingFilterComboBoxModel<String> model = SwingFilterComboBoxModel.builder()
							.items(asList("Akureyri", "Reykjavik", "Selfoss"))
							.nullItem("-")
							.build();
			model.items().refresh();
			ComponentValue<JComboBox<String>, String> value = Components.comboBox()
							.model(model)
							.completionMode(mode)
							.buildValue();
			JTextField editor = (JTextField) value.component().getEditor().getEditorComponent();
			SwingUtilities.invokeAndWait(() -> {
				value.set("Reykjavik");
				value.clear();
			});
			SwingUtilities.invokeAndWait(() -> {});
			// the null item caption selected, the caret at its start, typing replacing it and completing anew
			assertEquals("-", editor.getText());
			assertEquals(0, editor.getCaretPosition());
			assertEquals(1, editor.getSelectionEnd());
			SwingUtilities.invokeAndWait(() -> editor.replaceSelection("s"));
			assertEquals("Selfoss", value.get());
		}
	}
}
