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
 * Copyright (c) 2010 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.component;

import is.codion.common.reactive.value.Value;
import is.codion.swing.common.ui.component.text.TextInput;
import is.codion.swing.common.ui.component.value.ComponentValue;

import org.junit.jupiter.api.Test;

import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.text.JTextComponent;
import javax.swing.undo.UndoManager;
import java.util.ArrayList;
import java.util.List;

import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.*;

public class StringValuesTest {

	@Test
	void valueLink() {
		Value<String> textValue = Value.builder()
						.nullable("start")
						.validator(text -> {
							if (text != null && text.equals("nono")) {
								throw new IllegalArgumentException();
							}
						})
						.build();
		ComponentValue<JTextField, String> textFieldValue = Components.stringField()
						.link(textValue)
						.buildValue();

		assertEquals("start", textFieldValue.get());

		textFieldValue.set("testing");
		assertEquals("testing", textValue.get());

		assertThrows(IllegalArgumentException.class, () -> textFieldValue.set("nono"));
		assertEquals("testing", textFieldValue.get());

		textValue.set("hello");
		assertEquals("hello", textFieldValue.get());

		assertThrows(IllegalArgumentException.class, () -> textValue.set("nono"));
	}

	@Test
	void nullInitialValue() {
		Value<String> stringPropertyValue = Value.nullable();
		JTextField textField = Components.stringField()
						.link(stringPropertyValue)
						.build();
		assertNull(stringPropertyValue.get());
		assertEquals("", textField.getText());
		stringPropertyValue.set("hello");
		assertEquals("hello", textField.getText());
		textField.setText("42");
		assertEquals("42", stringPropertyValue.get());
		textField.setText("");
		assertNull(stringPropertyValue.get());

		stringPropertyValue.set("test");
		JTextField textField2 = Components.stringField()
						.link(stringPropertyValue)
						.build();
		assertEquals("test", textField2.getText());
	}

	@Test
	void nonNullInitialValue() {
		Value<String> value = Value.nullable("name");
		JTextField textField = Components.stringField()
						.link(value)
						.build();
		assertEquals("name", textField.getText());
		textField.setText("darri");
		assertFalse(value.getOrThrow().isEmpty());
		assertEquals("darri", value.get());
		textField.setText("");
		assertNull(value.get());
		value.set("Björn");
		assertEquals("Björn", textField.getText());
	}

	@Test
	void textValue() {
		ComponentValue<JTextField, String> value = Components.stringField()
						.buildValue();
		JTextField textField = value.component();

		assertNull(value.get());
		textField.setText("hello there");
		assertEquals("hello there", value.get());
		textField.setText("");
		assertNull(value.get());

		value.set("hi");
		assertEquals("hi", textField.getText());
	}

	@Test
	void characterValue() {
		ComponentValue<JTextField, Character> value = Components.textField()
						.type(Character.class)
						.buildValue();
		assertNull(value.get());
		value.component().setText("2");
		assertEquals('2', value.get());
		value.clear();
		assertTrue(value.component().getText().isEmpty());
	}

	@Test
	void editNotifiedOnce() throws Exception {
		// a replacement is a removal followed by an insertion, the removal leaving an intermediate text
		ComponentValue<JTextField, String> fieldValue = Components.stringField().buildValue();
		ComponentValue<JTextArea, String> areaValue = Components.textArea().buildValue();
		ComponentValue<TextInput, String> inputValue = Components.textInput().buildValue();
		assertNotified(fieldValue, fieldValue.component());
		assertNotified(areaValue, areaValue.component());
		assertNotified(inputValue, inputValue.component().textField());
	}

	@Test
	void replaceLinkedRejectingEmpty() throws Exception {
		Value<String> value = Value.builder()
						.nullable("abc")
						.validator(text -> {
							if (text == null || text.isEmpty()) {
								throw new IllegalArgumentException("required");
							}
						})
						.build();
		ComponentValue<JTextField, String> fieldValue = Components.stringField()
						.link(value)
						.buildValue();
		JTextField field = fieldValue.component();
		SwingUtilities.invokeAndWait(() -> {
			// typing over a selection
			field.selectAll();
			field.replaceSelection("x");
		});
		assertEquals("x", field.getText());
		assertEquals("x", value.get());
		// setting the component value
		fieldValue.set("xyz");
		assertEquals("xyz", field.getText());
		assertEquals("xyz", value.get());
	}

	@Test
	void undoNotified() throws Exception {
		// an edit made outside the document filter, as undoing one is, notified as the document notifies it
		ComponentValue<JTextField, String> fieldValue = Components.stringField().buildValue();
		JTextField field = fieldValue.component();
		UndoManager undoManager = new UndoManager();
		field.getDocument().addUndoableEditListener(undoManager);
		SwingUtilities.invokeAndWait(() -> field.replaceSelection("abc"));
		List<String> notified = new ArrayList<>();
		fieldValue.addConsumer(notified::add);
		SwingUtilities.invokeAndWait(undoManager::undo);
		assertEquals("", field.getText());
		assertNull(fieldValue.get());
		assertEquals(singletonList(null), notified);
	}

	private static void assertNotified(ComponentValue<?, String> value, JTextComponent textComponent) throws Exception {
		value.set("abc");
		List<String> notified = new ArrayList<>();
		value.addConsumer(notified::add);
		SwingUtilities.invokeAndWait(() -> {
			textComponent.selectAll();
			textComponent.replaceSelection("x");
		});
		assertEquals(singletonList("x"), notified);
		notified.clear();
		value.set("abc");
		assertEquals(singletonList("abc"), notified);
	}
}
