/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.framework.ui;

import org.jminor.framework.domain.property.Property;
import org.jminor.swing.common.ui.input.InputProvider;

import javax.swing.AbstractCellEditor;
import javax.swing.JTable;
import javax.swing.table.TableCellEditor;
import java.awt.Component;

import static java.util.Collections.emptyList;

/**
 * A {@link TableCellEditor} implementation for {@link EntityTablePanel}.
 */
final class EntityTableCellEditor extends AbstractCellEditor implements TableCellEditor {

  private final EntityTablePanel tablePanel;

  private InputProvider inputProvider;

  EntityTableCellEditor(final EntityTablePanel tablePanel) {
    this.tablePanel = tablePanel;
  }

  /** {@inheritDoc} */
  @Override
  public Component getTableCellEditorComponent(final JTable table, final Object value, final boolean isSelected,
                                               final int row, final int column) {
    if (inputProvider == null) {
      inputProvider = initializeEditorComponent(column);
    }
    inputProvider.setValue(value);

    return inputProvider.getInputComponent();
  }

  /** {@inheritDoc} */
  @Override
  public Object getCellEditorValue() {
    return inputProvider.getValue();
  }

  private InputProvider initializeEditorComponent(final int column) {
    final Property property = (Property) tablePanel.getEntityTableModel()
            .getColumnModel().getColumn(column).getIdentifier();

    return tablePanel.getInputProvider(property, emptyList());
  }
}
