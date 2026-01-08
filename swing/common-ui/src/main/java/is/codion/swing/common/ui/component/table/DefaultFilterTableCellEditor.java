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

import is.codion.swing.common.model.component.combobox.FilterComboBoxModel;
import is.codion.swing.common.ui.component.text.TemporalFieldPanel;
import is.codion.swing.common.ui.component.text.TextFieldPanel;
import is.codion.swing.common.ui.component.value.ComponentValue;

import org.jspecify.annotations.Nullable;

import javax.swing.AbstractCellEditor;
import javax.swing.Action;
import javax.swing.CellEditor;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.table.TableCellRenderer;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.EventObject;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import static is.codion.swing.common.ui.component.Components.*;
import static is.codion.swing.common.ui.component.table.DefaultFilterTableCellEditor.DefaultBuilder.COMPONENT_STEP;
import static java.awt.event.InputEvent.*;
import static java.awt.event.KeyEvent.VK_ENTER;
import static java.awt.event.MouseEvent.MOUSE_DRAGGED;
import static java.util.Arrays.asList;
import static java.util.Objects.requireNonNull;

final class DefaultFilterTableCellEditor<C extends JComponent, T> extends AbstractCellEditor implements FilterTableCellEditor<C, T> {

	static final Factory<Object, Object> DEFAULT_FACTORY = new DefaultFactory<>();

	private final Supplier<ComponentValue<C, T>> inputComponent;
	private final Function<EventObject, Boolean> cellEditable;
	private final Function<EventObject, Boolean> shouldSelectCell;
	private final @Nullable Function<@Nullable T, Boolean> stopCellEditing;
	private final @Nullable Boolean resizeRow;
	private final Consumer<FilterTableCellEditor<C, T>> configuration;

	private @Nullable ComponentValue<C, T> componentValue;

	int editedRow = -1;

	DefaultFilterTableCellEditor(DefaultBuilder<C, T> builder) {
		this.inputComponent = builder.component;
		this.cellEditable = builder.cellEditable();
		this.shouldSelectCell = builder.shouldSelectCell == null ? new DefaultShouldSelectCell() : builder.shouldSelectCell;
		this.stopCellEditing = builder.stopCellEditing;
		this.resizeRow = builder.resizeRow;
		this.configuration = builder.configuration == null ? new DefaultCellEditorConfiguration<>() : builder.configuration;
	}

	@Override
	public ComponentValue<C, T> componentValue() {
		synchronized (inputComponent) {
			if (componentValue == null) {
				componentValue = inputComponent.get();
				configuration.accept(this);
			}

			return componentValue;
		}
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
			((JCheckBox) component).setHorizontalAlignment(SwingConstants.CENTER);
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
		return shouldSelectCell.apply(event);
	}

	@Override
	public boolean stopCellEditing() {
		if (stopCellEditing == null || componentValue == null) {
			return super.stopCellEditing();
		}
		if (stopCellEditing.apply(componentValue.get())) {
			return super.stopCellEditing();
		}

		return false;
	}

	void updateUI() {
		if (componentValue != null) {
			componentValue.component().updateUI();
		}
	}

	boolean resizeRow(boolean resizeRowToFitEditor) {
		return resizeRow == null ? resizeRowToFitEditor : resizeRow;
	}

	private final class DefaultShouldSelectCell implements Function<EventObject, Boolean> {

		@Override
		public Boolean apply(EventObject event) {
			if (componentValue().component() instanceof JComboBox<?> && event instanceof MouseEvent) {
				return ((MouseEvent) event).getID() != MOUSE_DRAGGED;
			}

			return true;
		}
	}

	private static final class DefaultCellEditorConfiguration<C extends JComponent, T> implements Consumer<FilterTableCellEditor<C, T>> {

		@Override
		public void accept(FilterTableCellEditor<C, T> cellEditor) {
			C editorComponent = cellEditor.componentValue().component();
			if (editorComponent instanceof JCheckBox) {
				((JCheckBox) editorComponent).addActionListener(e -> cellEditor.stopCellEditing());
			}
			else if (editorComponent instanceof JComboBox) {
				new ComboBoxStopEditingOnEnter(cellEditor, (JComboBox<?>) editorComponent);
			}
			else if (editorComponent instanceof TextFieldPanel) {
				((TextFieldPanel) editorComponent).textField().addKeyListener(new StopEditingOnEnter(cellEditor));
			}
			else if (editorComponent instanceof TemporalFieldPanel<?>) {
				((TemporalFieldPanel<?>) editorComponent).temporalField().addKeyListener(new StopEditingOnEnter(cellEditor));
			}
			else if (editorComponent instanceof JSpinner) {
				((JSpinner) editorComponent).getEditor().addKeyListener(new StopEditingOnEnter(cellEditor));
			}
			else {
				editorComponent.addKeyListener(new StopEditingOnEnter(cellEditor));
			}
		}
	}

	private static final class DefaultComponentStep implements Builder.ComponentStep {

		@Override
		public <C extends JComponent, T> Builder<C, T> component(Supplier<ComponentValue<C, T>> component) {
			return new DefaultBuilder<>(requireNonNull(component));
		}
	}

	static final class DefaultBuilder<C extends JComponent, T> implements Builder<C, T> {

		static final ComponentStep COMPONENT_STEP = new DefaultComponentStep();

		private final Supplier<ComponentValue<C, T>> component;

		private @Nullable Function<EventObject, Boolean> cellEditable;
		private @Nullable Function<EventObject, Boolean> shouldSelectCell;
		private @Nullable Function<@Nullable T, Boolean> stopCellEditing;
		private int clickCountToStart = CLICK_COUNT_TO_START.getOrThrow();
		private @Nullable Boolean resizeRow;
		private @Nullable Consumer<FilterTableCellEditor<C, T>> configuration;

		private DefaultBuilder(Supplier<ComponentValue<C, T>> component) {
			this.component = component;
		}

		@Override
		public Builder<C, T> cellEditable(Function<EventObject, Boolean> cellEditable) {
			this.cellEditable = requireNonNull(cellEditable);
			return this;
		}

		@Override
		public Builder<C, T> shouldSelectCell(Function<EventObject, Boolean> shouldSelectCell) {
			this.shouldSelectCell = requireNonNull(shouldSelectCell);
			return this;
		}

		@Override
		public Builder<C, T> stopCellEditing(Function<@Nullable T, Boolean> stopCellEditing) {
			this.stopCellEditing = requireNonNull(stopCellEditing);
			return this;
		}

		@Override
		public Builder<C, T> clickCountToStart(int clickCountToStart) {
			this.clickCountToStart = clickCountToStart;
			return this;
		}

		@Override
		public Builder<C, T> resizeRow(boolean resizeRow) {
			this.resizeRow = resizeRow;
			return this;
		}

		@Override
		public Builder<C, T> configure(Consumer<FilterTableCellEditor<C, T>> cellEditor) {
			this.configuration = requireNonNull(cellEditor);
			return this;
		}

		@Override
		public FilterTableCellEditor<C, T> build() {
			return new DefaultFilterTableCellEditor<>(this);
		}

		private Function<EventObject, Boolean> cellEditable() {
			return cellEditable == null ? new DefaultCellEditable(clickCountToStart) : cellEditable;
		}
	}

	private static class StopEditingOnEnter extends KeyAdapter {

		private final CellEditor editor;

		private StopEditingOnEnter(CellEditor editor) {
			this.editor = editor;
		}

		@Override
		public final void keyPressed(KeyEvent e) {
			if (e.getKeyCode() == VK_ENTER) {
				onEnter();
				e.consume();
			}
			else {
				super.keyPressed(e);
			}
		}

		protected void onEnter() {
			editor.stopCellEditing();
		}
	}

	private static final class ComboBoxStopEditingOnEnter extends StopEditingOnEnter {

		private static final String ENTER_PRESSED = "enterPressed";

		private final JComboBox<?> comboBox;

		private ComboBoxStopEditingOnEnter(CellEditor editor, JComboBox<?> comboBox) {
			super(editor);
			this.comboBox = comboBox;
			comboBox.addKeyListener(this);
			if (comboBox.isEditable()) {
				comboBox.getEditor().getEditorComponent().addKeyListener(this);
			}
		}

		@Override
		protected void onEnter() {
			if (comboBox.isPopupVisible()) {
				Action action = comboBox.getActionMap().get(ENTER_PRESSED);
				if (action != null) {
					action.actionPerformed(new ActionEvent(comboBox, 0, ""));
				}
			}
			super.onEnter();
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

	private static final class DefaultFactory<R, C> implements Factory<R, C> {

		@Override
		public Optional<FilterTableCellEditor<?, ?>> create(C identifier, FilterTable<R, C> table) {
			Class<?> columnClass = table.model().columns().columnClass(identifier);
			FilterTableCellEditor<?, ?> cellEditor = null;
			if (columnClass.equals(LocalTime.class)) {
				cellEditor = COMPONENT_STEP.component(localTimeField()::buildValue).build();
			}
			else if (columnClass.equals(LocalDate.class)) {
				cellEditor = COMPONENT_STEP.component(localDateField()::buildValue).build();
			}
			else if (columnClass.equals(LocalDateTime.class)) {
				cellEditor = COMPONENT_STEP.component(localDateTimeField()::buildValue).build();
			}
			else if (columnClass.equals(OffsetDateTime.class)) {
				cellEditor = COMPONENT_STEP.component(offsetDateTimeField()::buildValue).build();
			}
			else if (columnClass.equals(String.class)) {
				cellEditor = COMPONENT_STEP.component(stringField()::buildValue).build();
			}
			else if (columnClass.equals(Character.class)) {
				cellEditor = COMPONENT_STEP.component(characterField()::buildValue).build();
			}
			else if (columnClass.equals(Short.class)) {
				cellEditor = COMPONENT_STEP.component(shortField()::buildValue).build();
			}
			else if (columnClass.equals(Integer.class)) {
				cellEditor = COMPONENT_STEP.component(integerField()::buildValue).build();
			}
			else if (columnClass.equals(Long.class)) {
				cellEditor = COMPONENT_STEP.component(longField()::buildValue).build();
			}
			else if (columnClass.equals(BigInteger.class)) {
				cellEditor = COMPONENT_STEP.component(bigIntegerField()::buildValue).build();
			}
			else if (columnClass.equals(Double.class)) {
				cellEditor = COMPONENT_STEP.component(doubleField()::buildValue).build();
			}
			else if (columnClass.equals(BigDecimal.class)) {
				cellEditor = COMPONENT_STEP.component(bigDecimalField()::buildValue).build();
			}
			else if (columnClass.equals(Boolean.class)) {
				cellEditor = COMPONENT_STEP.component(checkBox()::buildValue).build();
			}
			else if (columnClass.isEnum()) {
				cellEditor = COMPONENT_STEP.component(comboBox().model(FilterComboBoxModel.builder()
								.items(asList(columnClass.getEnumConstants()))
								.build())::buildValue).build();
			}

			return Optional.ofNullable(cellEditor);
		}
	}
}
