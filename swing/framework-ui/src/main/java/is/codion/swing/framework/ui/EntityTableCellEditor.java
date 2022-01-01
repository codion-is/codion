/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.ui;

import is.codion.common.value.Value;
import is.codion.swing.common.ui.component.ComponentValue;

import javax.swing.AbstractCellEditor;
import javax.swing.JCheckBox;
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

  EntityTableCellEditor(final Supplier<ComponentValue<T, ? extends JComponent>> inputComponentSupplier) {
    this.inputComponentSupplier = inputComponentSupplier;
  }

  @Override
  public final Component getTableCellEditorComponent(final JTable table, final Object value, final boolean isSelected,
                                                     final int row, final int column) {
    if (component == null) {
      component = initializeEditorComponent();
    }
    cellValue.set((T) value);

    return component;
  }

  @Override
  public final Object getCellEditorValue() {
    return cellValue.get();
  }

  @Override
  public final boolean isCellEditable(final EventObject event) {
    if (event instanceof MouseEvent) {
      return ((MouseEvent) event).getClickCount() >= 2;
    }

    return false;
  }

  private JComponent initializeEditorComponent() {
    final ComponentValue<T, ? extends JComponent> componentValue = inputComponentSupplier.get();
    componentValue.link(cellValue);
    final JComponent editorComponent = componentValue.getComponent();
    if (editorComponent instanceof JCheckBox) {
      ((JCheckBox) editorComponent).setHorizontalAlignment(SwingConstants.CENTER);
    }

    return editorComponent;
  }
}
