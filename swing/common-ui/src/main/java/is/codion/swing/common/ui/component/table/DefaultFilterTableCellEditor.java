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
 * Copyright (c) 2019 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.component.table;

import is.codion.swing.common.ui.component.value.ComponentValue;

import org.jspecify.annotations.Nullable;

import javax.swing.AbstractAction;
import javax.swing.AbstractCellEditor;
import javax.swing.Action;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.table.TableCellRenderer;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.util.EventObject;
import java.util.function.Function;
import java.util.function.Supplier;

import static java.awt.event.InputEvent.*;
import static java.util.Objects.requireNonNull;

final class DefaultFilterTableCellEditor<T> extends AbstractCellEditor implements FilterTableCellEditor<T> {

	private final Supplier<ComponentValue<? extends JComponent, T>> inputComponent;
	private final Function<EventObject, Boolean> cellEditable;
	private final @Nullable Boolean resizeRow;

	private @Nullable ComponentValue<? extends JComponent, T> componentValue;

	int editedRow = -1;

	DefaultFilterTableCellEditor(DefaultBuilder<T> builder) {
		this.inputComponent = builder.component;
		this.cellEditable = builder.cellEditable();
		this.resizeRow = builder.resizeRow;
	}

	@Override
	public ComponentValue<? extends JComponent, T> componentValue() {
		if (componentValue == null) {
			componentValue = initializeComponentValue();
		}

		return componentValue;
	}

	@Override
	public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
		this.editedRow = row;
		componentValue().set((T) value);

		// The editor is prepared before the selection is actually changed, see table.editCellAt in BasicTableUI.adjustSelection()
		// so isSelected may be false here, but the row is about to be selected (we are editing it, after all), so we set it
		// to true for the editor configuration, in order for JCheckBox to display the selection background correctly.
		return configure(componentValue().component(), table, value, true, row, column);
	}

	private static JComponent configure(JComponent component, JTable table, Object value, boolean isSelected, int row, int column) {
		TableCellRenderer renderer = table.getCellRenderer(row, column);
		if (component instanceof JCheckBox) {
			JComponent rendererComponent = (JComponent) renderer.getTableCellRendererComponent(table, value, isSelected, true, row, column);
			component.setBackground(rendererComponent.getBackground());
			component.setBorder(rendererComponent.getBorder());
			component.setOpaque(true);
			component.setRequestFocusEnabled(false);
		}
		if (component instanceof JTextField && renderer instanceof FilterTableCellRenderer) {
			((JTextField) component).setHorizontalAlignment(((FilterTableCellRenderer<?, ?, ?>) renderer).horizontalAlignment());
		}

		return component;
	}

	@Override
	public @Nullable Object getCellEditorValue() {
		return componentValue().get();
	}

	@Override
	public boolean isCellEditable(EventObject event) {
		return cellEditable.apply(event);
	}

	@Override
	public boolean shouldSelectCell(EventObject event) {
		if (event instanceof MouseEvent) {
			MouseEvent e = (MouseEvent) event;

			return e.getID() != MouseEvent.MOUSE_DRAGGED;
		}

		return true;
	}

	void updateUI() {
		if (componentValue != null) {
			componentValue.component().updateUI();
		}
	}

	boolean resizeRow(boolean resizeRowToFitEditor) {
		return resizeRow == null ? resizeRowToFitEditor : resizeRow;
	}

	private ComponentValue<? extends JComponent, T> initializeComponentValue() {
		ComponentValue<? extends JComponent, T> value = inputComponent.get();
		JComponent editorComponent = value.component();
		if (editorComponent instanceof JTextField) {
			((JTextField) editorComponent).addActionListener(new StopEditingActionListener());
		}
		else if (editorComponent instanceof JCheckBox) {
			((JCheckBox) editorComponent).setHorizontalAlignment(SwingConstants.CENTER);
			((JCheckBox) editorComponent).addActionListener(new StopEditingActionListener());
		}
		else if (editorComponent instanceof JComboBox) {
			new ComboBoxEnterPressedAction((JComboBox<?>) editorComponent, new StopEditingActionListener());
		}

		return value;
	}

	private static final class DefaultComponentStep implements Builder.ComponentStep {

		@Override
		public <T> Builder<T> component(Supplier<ComponentValue<? extends JComponent, T>> component) {
			return new DefaultBuilder<>(requireNonNull(component));
		}
	}

	static final class DefaultBuilder<T> implements Builder<T> {

		static final ComponentStep COMPONENT = new DefaultComponentStep();

		private final Supplier<ComponentValue<? extends JComponent, T>> component;

		private @Nullable Function<EventObject, Boolean> cellEditable;
		private int clickCountToStart = 2;
		private @Nullable Boolean resizeRow;

		private DefaultBuilder(Supplier<ComponentValue<? extends JComponent, T>> component) {
			this.component = component;
		}

		@Override
		public Builder<T> cellEditable(Function<EventObject, Boolean> cellEditable) {
			this.cellEditable = requireNonNull(cellEditable);
			return this;
		}

		@Override
		public Builder<T> clickCountToStart(int clickCountToStart) {
			this.clickCountToStart = clickCountToStart;
			return this;
		}

		@Override
		public Builder<T> resizeRow(boolean resizeRow) {
			this.resizeRow = resizeRow;
			return this;
		}

		@Override
		public FilterTableCellEditor<T> build() {
			return new DefaultFilterTableCellEditor<>(this);
		}

		private Function<EventObject, Boolean> cellEditable() {
			return cellEditable == null ? new DefaultCellEditable(clickCountToStart) : cellEditable;
		}
	}

	private final class StopEditingActionListener implements ActionListener {

		@Override
		public void actionPerformed(ActionEvent e) {
			stopCellEditing();
		}
	}

	private static final class ComboBoxEnterPressedAction extends AbstractAction {

		private static final String ENTER_PRESSED = "enterPressed";

		private final JComboBox<?> comboBox;
		private final ActionListener actionListener;
		private final Action enterPressedAction;

		private ComboBoxEnterPressedAction(JComboBox<?> comboBox, ActionListener actionListener) {
			this.comboBox = comboBox;
			this.actionListener = actionListener;
			this.enterPressedAction = comboBox.getActionMap().get(ENTER_PRESSED);
			this.comboBox.getActionMap().put(ENTER_PRESSED, this);
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			if (comboBox.isPopupVisible()) {
				enterPressedAction.actionPerformed(e);
			}
			actionListener.actionPerformed(e);
		}
	}

	private static final class DefaultCellEditable implements Function<EventObject, Boolean> {

		private static final int MODIFIERS = CTRL_DOWN_MASK | SHIFT_DOWN_MASK | ALT_DOWN_MASK | META_DOWN_MASK;

		private final int clickCountToStart;

		private DefaultCellEditable(int clickCountToStart) {
			this.clickCountToStart = clickCountToStart;
		}

		@Override
		public Boolean apply(EventObject event) {
			if (event instanceof MouseEvent) {
				MouseEvent mouseEvent = (MouseEvent) event;
				if ((mouseEvent.getModifiersEx() & MODIFIERS) != 0) {
					return false;
				}

				return mouseEvent.getClickCount() >= clickCountToStart;
			}

			return true;
		}
	}
}
