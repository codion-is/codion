/*
 * Copyright (c) 2019 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.ui;

import is.codion.common.value.Value;
import is.codion.swing.common.ui.component.ComponentValue;
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

/**
 * A {@link TableCellEditor} implementation for {@link EntityTablePanel}.
 */
class EntityTableCellEditor<T> extends AbstractCellEditor implements TableCellEditor {

  private final Supplier<ComponentValue<T, ? extends JComponent>> inputComponentSupplier;
  private final Value<T> cellValue = Value.value();

  private JComponent component;

  EntityTableCellEditor(Supplier<ComponentValue<T, ? extends JComponent>> inputComponentSupplier) {
    this.inputComponentSupplier = inputComponentSupplier;
  }

  @Override
  public final Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected,
                                                     int row, int column) {
    if (component == null) {
      component = createEditorComponent();
    }
    cellValue.set((T) value);

    return component;
  }

  @Override
  public final Object getCellEditorValue() {
    return cellValue.get();
  }

  @Override
  public final boolean isCellEditable(EventObject event) {
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
