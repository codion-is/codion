/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.ui;

import is.codion.common.value.Value;
import is.codion.common.value.Values;
import is.codion.framework.domain.property.Property;

import javax.swing.AbstractCellEditor;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.TableCellEditor;
import java.awt.Component;
import java.awt.event.MouseEvent;
import java.util.EventObject;

import static java.util.Objects.requireNonNull;

/**
 * A {@link TableCellEditor} implementation for {@link EntityTablePanel}.
 */
class EntityTableCellEditor<T> extends AbstractCellEditor implements TableCellEditor {

  private final Property<T> property;
  private final Value<T> cellValue = Values.value();

  private JComponent component;

  EntityTableCellEditor(final Property<T> property) {
    this.property = requireNonNull(property, "property");
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

  protected final Property<T> getProperty() {
    return property;
  }

  protected final Value<T> getCellValue() {
    return cellValue;
  }

  protected JComponent initializeEditorComponent() {
    final JComponent editorComponent = EntityInputComponents.createInputComponent(property, cellValue);
    if (editorComponent instanceof JCheckBox) {
      ((JCheckBox) editorComponent).setHorizontalAlignment(SwingConstants.CENTER);
    }

    return editorComponent;
  }
}
