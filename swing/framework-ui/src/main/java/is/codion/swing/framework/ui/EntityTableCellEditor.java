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
package is.codion.swing.framework.ui;

import is.codion.common.value.Value;
import is.codion.swing.common.ui.component.value.ComponentValue;
import is.codion.swing.common.ui.control.Control;

import javax.swing.AbstractCellEditor;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.TableCellEditor;
import java.awt.Component;
import java.awt.event.MouseEvent;
import java.util.EventObject;
import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;

/**
 * A {@link TableCellEditor} implementation for {@link EntityTablePanel}.
 */
final class EntityTableCellEditor<T> extends AbstractCellEditor implements TableCellEditor {

  private final Supplier<ComponentValue<T, ? extends JComponent>> inputComponentSupplier;
  private final Value<T> cellValue = Value.value();

  private JComponent component;

  EntityTableCellEditor(Supplier<ComponentValue<T, ? extends JComponent>> inputComponentSupplier) {
    this.inputComponentSupplier = requireNonNull(inputComponentSupplier);
  }

  @Override
  public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
    if (component == null) {
      component = createEditorComponent();
    }
    cellValue.set((T) value);

    return component;
  }

  @Override
  public Object getCellEditorValue() {
    return cellValue.get();
  }

  @Override
  public boolean isCellEditable(EventObject event) {
    if (event instanceof MouseEvent) {
      return ((MouseEvent) event).getClickCount() >= 2;
    }

    return false;
  }

  private JComponent createEditorComponent() {
    ComponentValue<T, ? extends JComponent> componentValue = inputComponentSupplier.get();
    componentValue.link(cellValue);
    JComponent editorComponent = componentValue.component();
    if (editorComponent instanceof JCheckBox) {
      ((JCheckBox) editorComponent).setHorizontalAlignment(SwingConstants.CENTER);
    }
    if (editorComponent instanceof JComboBox) {
      new ComboBoxEnterPressedAction((JComboBox<?>) editorComponent, Control.control(this::stopCellEditing));
    }

    return editorComponent;
  }
}
