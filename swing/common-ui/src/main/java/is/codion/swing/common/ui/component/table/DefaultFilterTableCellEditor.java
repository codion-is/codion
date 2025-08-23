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
 * Copyright (c) 2019 - 2025, Björn Darri Sigurðsson.
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
import java.awt.event.MouseEvent;
import java.util.EventObject;
import java.util.function.Function;
import java.util.function.Supplier;

import static is.codion.swing.common.ui.control.Control.command;
import static java.util.Objects.requireNonNull;

final class DefaultFilterTableCellEditor<T> extends AbstractCellEditor implements FilterTableCellEditor<T> {

	private final Supplier<ComponentValue<? extends JComponent, T>> inputComponent;
	private final Function<EventObject, Boolean> cellEditable;

	private @Nullable ComponentValue<? extends JComponent, T> componentValue;

	int editedRow = -1;

	DefaultFilterTableCellEditor(DefaultBuilder<T> builder) {
		this.inputComponent = builder.component;
		this.cellEditable = builder.cellEditable;
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

		return configure(componentValue().component(), table, value, isSelected, row, column);
	}

	private static JComponent configure(JComponent component, JTable table, Object value, boolean isSelected, int row, int column) {
		TableCellRenderer renderer = table.getCellRenderer(row, column);
		if (component instanceof JCheckBox) {
			component.setBackground(renderer
							.getTableCellRendererComponent(table, value, isSelected, true, row, column)
							.getBackground());
		}
		if (component instanceof JTextField && renderer instanceof FilterTableCellRenderer) {
			((JTextField) component).setHorizontalAlignment(((FilterTableCellRenderer<?>) renderer).horizontalAlignment());
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

	void updateUI() {
		if (componentValue != null) {
			componentValue.component().updateUI();
		}
	}

	private ComponentValue<? extends JComponent, T> initializeComponentValue() {
		ComponentValue<? extends JComponent, T> value = inputComponent.get();
		JComponent editorComponent = value.component();
		if (editorComponent instanceof JCheckBox) {
			((JCheckBox) editorComponent).setHorizontalAlignment(SwingConstants.CENTER);
		}
		if (editorComponent instanceof JComboBox) {
			JComboBox<?> comboBox = (JComboBox<?>) editorComponent;
			comboBox.putClientProperty("JComboBox.isTableCellEditor", true);
			new ComboBoxEnterPressedAction(comboBox, command(this::stopCellEditing));
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

		private Function<EventObject, Boolean> cellEditable = new DefaultCellEditable();

		private DefaultBuilder(Supplier<ComponentValue<? extends JComponent, T>> component) {
			this.component = component;
		}

		@Override
		public Builder<T> cellEditable(Function<EventObject, Boolean> cellEditable) {
			this.cellEditable = requireNonNull(cellEditable);
			return this;
		}

		@Override
		public FilterTableCellEditor<T> build() {
			return new DefaultFilterTableCellEditor<>(this);
		}
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

	private static final class DefaultCellEditable implements Function<EventObject, Boolean> {

		@Override
		public Boolean apply(EventObject event) {
			if (event instanceof MouseEvent) {
				return ((MouseEvent) event).getClickCount() == 2;
			}

			return true;
		}
	}
}
