/*
 * Copyright (c) 2013 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.ui;

import is.codion.framework.domain.entity.Attribute;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.property.Property;
import is.codion.swing.common.ui.component.table.FilteredTableCellRenderer;
import is.codion.swing.framework.model.SwingEntityTableModel;

/**
 * Provides TableCellRenderer implementations for EntityTablePanels via {@link #builder(SwingEntityTableModel, Property)}.
 */
public interface EntityTableCellRenderer extends FilteredTableCellRenderer {

  /**
   * Instantiates a new {@link EntityTableCellRenderer.Builder} with defaults based on the given property.
   * @param tableModel the table model providing the data to render
   * @param property the property
   * @return a new {@link EntityTableCellRenderer.Builder} instance
   */
  static Builder<SwingEntityTableModel, Entity, Attribute<?>> builder(SwingEntityTableModel tableModel, Property<?> property) {
    return new EntityTableCellRendererBuilder(tableModel, property);
  }
}
