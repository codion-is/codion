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
 * Copyright (c) 2019 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.component.table;

import is.codion.swing.common.ui.component.value.ComponentValue;

import javax.swing.AbstractAction;
import javax.swing.AbstractCellEditor;
import javax.swing.Action;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.util.EventObject;
import java.util.function.Supplier;

import static is.codion.swing.common.ui.control.Control.commandControl;
import static java.util.Objects.requireNonNull;

final class DefaultFilterTableCellEditor<T> extends AbstractCellEditor implements FilterTableCellEditor<T> {

	private final Supplier<ComponentValue<T, ? extends JComponent>> inputComponent;

	private ComponentValue<T, ? extends JComponent> componentValue;

	DefaultFilterTableCellEditor(Supplier<ComponentValue<T, ? extends JComponent>> inputComponent) {
		this.inputComponent = requireNonNull(inputComponent);
	}

	@Override
	public ComponentValue<T, ? extends JComponent> componentValue() {
		if (componentValue == null) {
			componentValue = initializeComponentValue();
		}

		return componentValue;
	}

	@Override
	public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
		componentValue().set((T) value);

		return componentValue().component();
	}

	@Override
	public Object getCellEditorValue() {
		return componentValue().get();
	}

	@Override
	public boolean isCellEditable(EventObject event) {
		if (event instanceof MouseEvent) {
			return ((MouseEvent) event).getClickCount() >= 2;
		}

		return false;
	}

	private ComponentValue<T, ? extends JComponent> initializeComponentValue() {
		ComponentValue<T, ? extends JComponent> value = inputComponent.get();
		JComponent editorComponent = value.component();
		if (editorComponent instanceof JCheckBox) {
			((JCheckBox) editorComponent).setHorizontalAlignment(SwingConstants.CENTER);
		}
		if (editorComponent instanceof JComboBox) {
			new ComboBoxEnterPressedAction((JComboBox<?>) editorComponent, commandControl(this::stopCellEditing));
		}

		return value;
	}

	private static final class ComboBoxEnterPressedAction extends AbstractAction {

		private static final String ENTER_PRESSED = "enterPressed";

		private final JComboBox<?> comboBox;
		private final Action action;
		private final Action enterPressedAction;

		private ComboBoxEnterPressedAction(JComboBox<?> comboBox, Action action) {
			this.comboBox = comboBox;
			this.action = action;
			this.enterPressedAction = comboBox.getActionMap().get(ENTER_PRESSED);
			this.comboBox.getActionMap().put(ENTER_PRESSED, this);
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			if (comboBox.isPopupVisible()) {
				enterPressedAction.actionPerformed(e);
			}
			else if (action.isEnabled()) {
				action.actionPerformed(e);
			}
		}
	}
}
