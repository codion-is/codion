/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.framework.ui;

import org.jminor.common.value.Value;
import org.jminor.common.value.Values;
import org.jminor.framework.db.EntityConnectionProvider;
import org.jminor.framework.domain.property.ForeignKeyProperty;
import org.jminor.framework.domain.property.Property;
import org.jminor.swing.framework.model.SwingEntityComboBoxModel;

import javax.swing.AbstractCellEditor;
import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.table.TableCellEditor;
import java.awt.Component;
import java.awt.event.MouseEvent;
import java.util.EventObject;

/**
 * A {@link TableCellEditor} implementation for {@link EntityTablePanel}.
 */
final class EntityTableCellEditor extends AbstractCellEditor implements TableCellEditor {

  private final EntityConnectionProvider connectionProvider;
  private final Property property;
  private final Value cellValue = Values.value();

  private JComponent component;

  EntityTableCellEditor(final EntityConnectionProvider connectionProvider, final Property property) {
    this.connectionProvider = connectionProvider;
    this.property = property;
  }

  /** {@inheritDoc} */
  @Override
  public Component getTableCellEditorComponent(final JTable table, final Object value, final boolean isSelected,
                                               final int row, final int column) {
    if (component == null) {
      component = initializeEditorComponent();
    }
    cellValue.set(value);

    return component;
  }

  /** {@inheritDoc} */
  @Override
  public Object getCellEditorValue() {
    return cellValue.get();
  }

  /** {@inheritDoc} */
  @Override
  public boolean isCellEditable(final EventObject event) {
    if (event instanceof MouseEvent) {
      return ((MouseEvent) event).getClickCount() >= 2;
    }

    return true;
  }

  private JComponent initializeEditorComponent() {
    if (property instanceof ForeignKeyProperty) {
      return EntityUiUtil.createInputComponent(property, cellValue,
              new SwingEntityComboBoxModel(((ForeignKeyProperty) property).getForeignEntityId(),
                      connectionProvider));
    }

    return EntityUiUtil.createInputComponent(property, cellValue, null);
  }
}
