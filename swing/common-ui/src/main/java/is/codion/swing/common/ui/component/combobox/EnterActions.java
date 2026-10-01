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

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JRootPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

import static java.awt.event.KeyEvent.VK_ENTER;
import static java.util.Objects.requireNonNull;

/**
 * The Enter actions of a combo box, in place of the look and feel's {@code enterPressed} action, which the editor
 * of an editable combo box calls on Enter, as does the key binding of a non-editable one.
 * While the popup is visible, Enter is the look and feel's. Otherwise the first enabled action, in the order added,
 * is performed, and with none enabled Enter is the look and feel's, the default button.
 */
final class EnterActions extends AbstractAction {

	private static final String ENTER_PRESSED = "enterPressed";
	private static final KeyStroke ENTER = KeyStroke.getKeyStroke(VK_ENTER, 0);

	private final JComboBox<?> comboBox;
	private final List<Action> actions = new ArrayList<>();

	private EnterActions(JComboBox<?> comboBox) {
		this.comboBox = comboBox;
	}

	static void add(JComboBox<?> comboBox, Action action) {
		requireNonNull(comboBox);
		requireNonNull(action);
		Action enterPressed = comboBox.getActionMap().get(ENTER_PRESSED);
		EnterActions enterActions;
		if (enterPressed instanceof EnterActions) {
			enterActions = (EnterActions) enterPressed;
		}
		else {
			enterActions = new EnterActions(comboBox);
			comboBox.getActionMap().put(ENTER_PRESSED, enterActions);
		}
		if (!enterActions.actions.contains(action)) {
			enterActions.actions.add(action);
		}
	}

	@Override
	public void actionPerformed(ActionEvent event) {
		if (comboBox.isPopupVisible()) {
			lookAndFeel(event);
		}
		else {
			// Once the editor of an editable combo box has been committed, by JComboBox.actionPerformed(),
			// which follows this one and would otherwise overwrite any change an action makes to the combo box
			SwingUtilities.invokeLater(() -> performFirstEnabled(event));
		}
	}

	/**
	 * Enabled, so that a key binding consumes Enter, while the popup is visible, an action is enabled or the window
	 * binds Enter, as the look and feel's own, leaving Enter to the ancestors otherwise.
	 * @return true if this action handles Enter
	 */
	@Override
	public boolean isEnabled() {
		return comboBox.isPopupVisible() || actionEnabled() || windowBindsEnter();
	}

	private void performFirstEnabled(ActionEvent event) {
		for (Action action : new ArrayList<>(actions)) {
			if (action.isEnabled()) {
				action.actionPerformed(event);
				return;
			}
		}
		lookAndFeel(event);
	}

	// Looked up when needed, a look and feel change replacing the action map it is in
	private void lookAndFeel(ActionEvent event) {
		ActionMap lookAndFeelActions = comboBox.getActionMap().getParent();
		Action action = lookAndFeelActions == null ? null : lookAndFeelActions.get(ENTER_PRESSED);
		if (action != null) {
			action.actionPerformed(event);
		}
	}

	private boolean actionEnabled() {
		for (Action action : actions) {
			if (action.isEnabled()) {
				return true;
			}
		}

		return false;
	}

	private boolean windowBindsEnter() {
		JRootPane rootPane = SwingUtilities.getRootPane(comboBox);

		return rootPane == null || rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).get(ENTER) != null;
	}
}
