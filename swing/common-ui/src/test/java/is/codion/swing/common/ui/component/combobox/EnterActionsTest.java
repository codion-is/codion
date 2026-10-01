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

import is.codion.common.reactive.state.State;
import is.codion.swing.common.model.component.combobox.SwingFilterComboBoxModel;
import is.codion.swing.common.ui.component.Components;
import is.codion.swing.common.ui.control.Control;

import org.junit.jupiter.api.Test;

import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JRootPane;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.awt.event.KeyEvent.VK_ENTER;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static javax.swing.JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT;
import static javax.swing.JComponent.WHEN_FOCUSED;
import static org.junit.jupiter.api.Assertions.*;

public final class EnterActionsTest {

	private static final KeyStroke ENTER = KeyStroke.getKeyStroke(VK_ENTER, 0);

	@Test
	void firstEnabledInTheOrderAdded() throws Exception {
		JComboBox<String> comboBox = comboBox();
		State firstEnabled = State.state(true);
		List<String> performed = new ArrayList<>();
		SwingUtilities.invokeAndWait(() -> {
			ComboBoxBuilder.addEnterAction(comboBox, Control.builder()
							.command(() -> performed.add("first"))
							.enabled(firstEnabled)
							.build());
			ComboBoxBuilder.addEnterAction(comboBox, Control.command(() -> performed.add("second")));
		});
		enter(comboBox);
		assertEquals(singletonList("first"), performed);
		SwingUtilities.invokeAndWait(() -> firstEnabled.set(false));
		enter(comboBox);
		assertEquals(asList("first", "second"), performed);
	}

	@Test
	void performedOnceTheEditorIsCommitted() throws Exception {
		// A plain model accepts the empty string the editor commits, which would be selected in case
		// the action cleared the combo box before the editor commit following it
		JComboBox<Integer> comboBox = new JComboBox<>(new DefaultComboBoxModel<>(new Integer[] {1, 2}));
		comboBox.setEditable(true);
		List<String> events = new ArrayList<>();
		SwingUtilities.invokeAndWait(() -> {
			comboBox.setSelectedItem(1);
			comboBox.addActionListener(e -> events.add(e.getActionCommand()));
			ComboBoxBuilder.addEnterAction(comboBox, Control.command(() -> {
				events.add("action");
				comboBox.setSelectedItem(null);
			}));
		});
		enter(comboBox);
		assertNull(comboBox.getSelectedItem());
		assertEquals(asList("comboBoxEdited", "action"), events.subList(0, 2));
	}

	@Test
	void lookAndFeelWhilePopupVisible() throws Exception {
		AtomicBoolean popupVisible = new AtomicBoolean(true);
		JComboBox<String> comboBox = new JComboBox<String>(new String[] {"a", "b"}) {
			@Override
			public boolean isPopupVisible() {
				return popupVisible.get();
			}
		};
		comboBox.setEditable(true);
		List<String> performed = new ArrayList<>();
		ActionMap lookAndFeel = lookAndFeel(comboBox, performed);
		SwingUtilities.invokeAndWait(() -> {
			ComboBoxBuilder.addEnterAction(comboBox, Control.command(() -> performed.add("action")));
			comboBox.getActionMap().setParent(lookAndFeel);
		});
		enter(comboBox);
		assertEquals(singletonList("look and feel"), performed);
		popupVisible.set(false);
		enter(comboBox);
		assertEquals(asList("look and feel", "action"), performed);
	}

	@Test
	void lookAndFeelWhenNoneEnabled() throws Exception {
		JComboBox<String> comboBox = comboBox();
		State enabled = State.state(true);
		List<String> performed = new ArrayList<>();
		SwingUtilities.invokeAndWait(() -> ComboBoxBuilder.addEnterAction(comboBox, Control.builder()
						.command(() -> performed.add("action"))
						.enabled(enabled)
						.build()));
		// looked up when needed, as after a look and feel change replacing the parent map
		comboBox.getActionMap().setParent(lookAndFeel(comboBox, performed));
		enter(comboBox);
		assertEquals(singletonList("action"), performed);
		SwingUtilities.invokeAndWait(() -> enabled.set(false));
		enter(comboBox);
		assertEquals(asList("action", "look and feel"), performed);
	}

	@Test
	void lookAndFeelChange() throws Exception {
		JComboBox<String> comboBox = comboBox();
		List<String> performed = new ArrayList<>();
		SwingUtilities.invokeAndWait(() -> {
			ComboBoxBuilder.addEnterAction(comboBox, Control.command(() -> performed.add("action")));
			// a new UI, with a new editor
			comboBox.updateUI();
		});
		enter(comboBox);
		assertEquals(singletonList("action"), performed);
	}

	@Test
	void enabledForTheKeyBinding() throws Exception {
		// A non-editable combo box consumes Enter only while it is handled, leaving it to the ancestors otherwise
		JComboBox<String> comboBox = new JComboBox<>(new String[] {"a"});
		JRootPane rootPane = new JRootPane();
		rootPane.getContentPane().add(comboBox);
		State enabled = State.state(false);
		SwingUtilities.invokeAndWait(() -> ComboBoxBuilder.addEnterAction(comboBox, Control.builder()
						.command(() -> {})
						.enabled(enabled)
						.build()));
		Action enterPressed = comboBox.getActionMap().get(comboBox.getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).get(ENTER));
		assertFalse(enterPressed.isEnabled());
		SwingUtilities.invokeAndWait(() -> enabled.set(true));
		assertTrue(enterPressed.isEnabled());
		SwingUtilities.invokeAndWait(() -> {
			enabled.set(false);
			JButton defaultButton = new JButton();
			rootPane.getContentPane().add(defaultButton);
			rootPane.setDefaultButton(defaultButton);
		});
		// the window binding Enter, the default button
		assertTrue(enterPressed.isEnabled());
	}

	@Test
	void builder() throws Exception {
		List<String> performed = new ArrayList<>();
		JComboBox<String> comboBox = Components.comboBox()
						.model(model())
						.transferFocusOnEnter(true)
						.enterAction(Control.command(() -> performed.add("action")))
						.build();
		enter(comboBox);
		assertEquals(singletonList("action"), performed);
		// the focus transfer, which would take Enter in the editor, disabled
		JComponent editor = (JComponent) comboBox.getEditor().getEditorComponent();
		JComponent plainEditor = (JComponent) comboBox().getEditor().getEditorComponent();
		assertEquals(plainEditor.getInputMap(WHEN_FOCUSED).get(ENTER), editor.getInputMap(WHEN_FOCUSED).get(ENTER));
	}

	private static ActionMap lookAndFeel(JComboBox<?> comboBox, List<String> performed) {
		ActionMap actionMap = new ActionMap();
		actionMap.setParent(comboBox.getActionMap().getParent());
		actionMap.put("enterPressed", Control.command(() -> performed.add("look and feel")));

		return actionMap;
	}

	/**
	 * Enter, as the editor's own Enter action performs it in an editable combo box, and the key binding in a
	 * non-editable one, followed by the deferred action
	 */
	private static void enter(JComboBox<?> comboBox) throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			if (comboBox.isEditable()) {
				((JTextField) comboBox.getEditor().getEditorComponent()).postActionEvent();
			}
			else {
				comboBox.getActionMap().get("enterPressed").actionPerformed(new ActionEvent(comboBox, ActionEvent.ACTION_PERFORMED, ""));
			}
		});
		SwingUtilities.invokeAndWait(() -> {});
	}

	private static JComboBox<String> comboBox() {
		return Components.comboBox()
						.model(model())
						.build();
	}

	private static SwingFilterComboBoxModel<String> model() {
		SwingFilterComboBoxModel<String> model = SwingFilterComboBoxModel.builder()
						.items(asList("a", "b"))
						.build();
		model.items().refresh();

		return model;
	}
}
