/*
 * Copyright (c) 2013 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.ui;

import is.codion.framework.domain.entity.Attribute;
import is.codion.framework.domain.entity.Entity;
import is.codion.swing.common.ui.component.table.FilteredTableCellRenderer.Builder;
import is.codion.swing.framework.model.SwingEntityTableModel;

/**
 * Provides {@link is.codion.swing.common.ui.component.table.FilteredTableCellRenderer}
 * implementations for EntityTablePanels via {@link #builder(SwingEntityTableModel, Attribute)}.
 */
public interface EntityTableCellRenderer {

  /**
   * Instantiates a new {@link Builder} with defaults based on the given attribute.
   * @param tableModel the table model providing the data to render
   * @param attribute the attribute
   * @return a new {@link Builder} instance
   */
  static Builder<SwingEntityTableModel, Entity, Attribute<?>> builder(SwingEntityTableModel tableModel, Attribute<?> attribute) {
    return new EntityTableCellRendererBuilder(tableModel, attribute);
  }
}
