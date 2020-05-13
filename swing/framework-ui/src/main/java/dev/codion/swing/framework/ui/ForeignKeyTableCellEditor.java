/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package dev.codion.swing.framework.ui;

import dev.codion.framework.db.EntityConnectionProvider;
import dev.codion.framework.domain.property.ForeignKeyProperty;
import dev.codion.framework.model.DefaultEntityLookupModel;
import dev.codion.swing.framework.model.SwingEntityComboBoxModel;

import javax.swing.JComponent;
import javax.swing.table.TableCellEditor;

/**
 * A {@link TableCellEditor} implementation for {@link ForeignKeyProperty} columns in a {@link EntityTablePanel}.
 */
final class ForeignKeyTableCellEditor extends EntityTableCellEditor {

  private final EntityConnectionProvider connectionProvider;

  ForeignKeyTableCellEditor(final EntityConnectionProvider connectionProvider, final ForeignKeyProperty property) {
    super(property);
    this.connectionProvider = connectionProvider;
  }

  //TODO handle Enter key correctly
  @Override
  protected JComponent initializeEditorComponent() {
    final ForeignKeyProperty foreignKeyProperty = (ForeignKeyProperty) getProperty();
    final String foreignEntityId = foreignKeyProperty.getForeignEntityId();

    if (connectionProvider.getEntities().getDefinition(foreignEntityId).isSmallDataset()) {
      return EntityInputComponents.createForeignKeyComboBox(foreignKeyProperty, getCellValue(),
              new SwingEntityComboBoxModel(foreignEntityId, connectionProvider));
    }

    return EntityInputComponents.createForeignKeyLookupField(foreignKeyProperty, getCellValue(),
            new DefaultEntityLookupModel(foreignEntityId, connectionProvider));
  }
}
