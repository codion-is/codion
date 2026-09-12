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
 * Copyright (c) 2021 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.component.multi;

import is.codion.common.reactive.value.ValueSet;
import is.codion.swing.common.model.component.combobox.SwingFilterComboBoxModel;
import is.codion.swing.common.ui.component.Components;
import is.codion.swing.common.ui.component.list.FilterList;
import is.codion.swing.common.ui.component.value.ComponentValue;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.key.KeyEvents;

import org.junit.jupiter.api.Test;

import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import java.awt.event.ActionEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.awt.event.KeyEvent.*;
import static java.util.Arrays.asList;
import static javax.swing.JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT;
import static javax.swing.JComponent.WHEN_FOCUSED;
import static org.junit.jupiter.api.Assertions.*;

public final class MultiInputTest {

	@Test
	void valueIsTheMembersPlusThePendingValueInOrder() {
		ComponentValue<JTextField, String> stringValue = Components.stringField().buildValue();
		ComponentValue<MultiInput<JTextField, String>, Set<String>> value = Components.multiInput()
						.component(stringValue)
						.buildValue();
		MultiInput<JTextField, String> field = value.component();
		assertSame(stringValue.component(), field.component());
		assertTrue(value.getOrThrow().isEmpty());

		stringValue.set("one");
		assertEquals(asList("one"), new ArrayList<>(value.getOrThrow()));
		assertTrue(field.members().isEmpty());

		insert(field.component());
		assertEquals(asList("one"), new ArrayList<>(field.members()));
		assertTrue(stringValue.isNull());
		stringValue.set("two");
		assertEquals(asList("one", "two"), new ArrayList<>(value.getOrThrow()));
		insert(field.component());
		stringValue.set("three");
		assertEquals(asList("one", "two", "three"), new ArrayList<>(value.getOrThrow()));
		// Adding a value already among the members changes nothing but the component, which is cleared
		stringValue.set("one");
		insert(field.component());
		assertEquals(asList("one", "two"), new ArrayList<>(field.members()));
		assertTrue(stringValue.isNull());
	}

	@Test
	void settingTheValueSetsTheMembers() {
		ComponentValue<JTextField, String> stringValue = Components.stringField().buildValue();
		ComponentValue<MultiInput<JTextField, String>, Set<String>> value = Components.multiInput()
						.component(stringValue)
						.buildValue();
		value.set(Set.of("a"));
		assertEquals(asList("a"), new ArrayList<>(value.component().members()));
		value.set(null);
		assertTrue(value.getOrThrow().isEmpty());
		assertTrue(value.component().members().isEmpty());
	}

	@Test
	void linkedToAValueSet() {
		ValueSet<String> valueSet = ValueSet.valueSet();
		ComponentValue<JTextField, String> stringValue = Components.stringField().buildValue();
		MultiInput<JTextField, String> field = Components.multiInput()
						.component(stringValue)
						.link(valueSet)
						.build();
		valueSet.add("x");
		assertEquals(asList("x"), new ArrayList<>(field.members()));
		stringValue.set("y");
		insert(field.component());
		assertEquals(Set.of("x", "y"), valueSet.get());
	}

	@Test
	void settingTheValueClearsThePendingValue() {
		ValueSet<String> valueSet = ValueSet.valueSet();
		ComponentValue<JTextField, String> stringValue = Components.stringField().buildValue();
		MultiInput<JTextField, String> field = Components.multiInput()
						.component(stringValue)
						.link(valueSet)
						.build();
		valueSet.set(Set.of("a", "b"));
		stringValue.set("c");
		assertEquals(Set.of("a", "b", "c"), valueSet.get());
		// clearing the condition, say
		valueSet.clear();
		assertTrue(field.members().isEmpty());
		assertNull(stringValue.get());
		assertTrue(valueSet.get().isEmpty());
		// the pending value no longer returns with the next change
		valueSet.set(Set.of("x"));
		assertEquals(asList("x"), new ArrayList<>(field.members()));
		assertNull(stringValue.get());
		assertEquals(Set.of("x"), valueSet.get());
	}

	@Test
	void theDialogsListRemovesTheSelected() {
		ValueSet<String> valueSet = ValueSet.valueSet();
		MultiInput<JTextField, String> field = Components.multiInput()
						.component(Components.stringField().buildValue())
						.link(valueSet)
						.build();
		valueSet.set(new LinkedHashSet<>(asList("a", "b", "c")));
		FilterList<String> list = field.createList();
		assertEquals(3, list.getModel().getSize());
		list.setSelectedIndex(1);
		action(list, WHEN_FOCUSED, KeyStroke.getKeyStroke(VK_DELETE, 0));
		assertEquals(asList("a", "c"), new ArrayList<>(field.members()));
		assertEquals(Set.of("a", "c"), valueSet.get());
		// The selection moves on to the next member
		assertEquals(1, list.getSelectedIndex());
	}

	@Test
	void enterAddsWhileTheComponentHoldsAValueAndIsLeftAloneOtherwise() {
		ComponentValue<JTextField, String> stringValue = Components.stringField().buildValue();
		MultiInput<JTextField, String> field = Components.multiInput()
						.component(stringValue)
						.build();
		assertFalse(enter(field.component()));
		stringValue.set("one");
		assertTrue(enter(field.component()));
		assertEquals(asList("one"), new ArrayList<>(field.members()));
		assertTrue(stringValue.isNull());
		assertFalse(enter(field.component()));

		MultiInput<JTextField, String> insertOnly = Components.multiInput()
						.component(Components.stringField().value("one").buildValue())
						.addOnEnter(false)
						.build();
		assertFalse(enter(insertOnly.component()));
		assertTrue(insertOnly.members().isEmpty());
	}

	@Test
	void aConsumedEnterIsLeftAlone() {
		ComponentValue<JTextField, String> stringValue = Components.stringField().buildValue();
		// the component using the Enter, a search field searching, its listener added before the field's
		stringValue.component().addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				if (e.getKeyCode() == VK_ENTER) {
					e.consume();
				}
			}
		});
		MultiInput<JTextField, String> field = Components.multiInput()
						.component(stringValue)
						.build();
		stringValue.set("a");
		assertTrue(enter(field.component()));
		assertTrue(field.members().isEmpty());
		assertEquals("a", stringValue.get());
	}

	@Test
	void enterIsLeftToAWrappedComboBox() {
		SwingFilterComboBoxModel<String> model = SwingFilterComboBoxModel.builder()
						.items(asList("one", "two"))
						.build();
		ComponentValue<JComboBox<String>, String> comboBoxValue = Components.comboBox()
						.model(model)
						.buildValue();
		MultiInput<JComboBox<String>, String> field = Components.multiInput()
						.component(comboBoxValue)
						.build();
		comboBoxValue.set("one");
		assertFalse(enter(field.component()));
		assertTrue(field.members().isEmpty());
		insert(field.component());
		assertEquals(asList("one"), new ArrayList<>(field.members()));
	}

	@Test
	void keyEventsAndListenersLandOnTheWrappedComponent() {
		AtomicBoolean fired = new AtomicBoolean();
		FocusListener focusListener = new FocusAdapter() {};
		MultiInput<JTextField, String> field = Components.multiInput()
						.component(Components.stringField().buildValue())
						.keyEvent(KeyEvents.builder()
										.keyCode(VK_F5)
										.action(Control.action(e -> fired.set(true))))
						.focusListener(focusListener)
						.build();
		KeyStroke f5 = KeyStroke.getKeyStroke(VK_F5, 0);
		assertNull(field.getInputMap(WHEN_FOCUSED).get(f5));
		action(field.component(), WHEN_FOCUSED, f5);
		assertTrue(fired.get());
		assertTrue(asList(field.component().getFocusListeners()).contains(focusListener));
	}

	private static void insert(JComponent component) {
		action(component, WHEN_ANCESTOR_OF_FOCUSED_COMPONENT, KeyStroke.getKeyStroke(VK_INSERT, 0));
	}

	private static void action(JComponent component, int condition, KeyStroke keyStroke) {
		Object key = component.getInputMap(condition).get(keyStroke);
		assertNotNull(key);
		component.getActionMap().get(key).actionPerformed(new ActionEvent(component, ActionEvent.ACTION_PERFORMED, ""));
	}

	/** Presses Enter through the component's key listeners, returning whether it was consumed */
	private static boolean enter(JComponent component) {
		KeyEvent event = new KeyEvent(component, KEY_PRESSED, System.currentTimeMillis(), 0, VK_ENTER, CHAR_UNDEFINED);
		List<KeyListener> listeners = asList(component.getKeyListeners());
		listeners.forEach(listener -> listener.keyPressed(event));

		return event.isConsumed();
	}
}
