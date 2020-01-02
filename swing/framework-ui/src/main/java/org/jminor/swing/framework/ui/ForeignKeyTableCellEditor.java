/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.framework.ui;

import org.jminor.framework.db.EntityConnectionProvider;
import org.jminor.framework.domain.property.ForeignKeyProperty;
import org.jminor.framework.model.DefaultEntityLookupModel;
import org.jminor.swing.framework.model.SwingEntityComboBoxModel;

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

    if (connectionProvider.getDomain().getDefinition(foreignEntityId).isSmallDataset()) {
      return EntityInputComponents.createForeignKeyComboBox(foreignKeyProperty, getCellValue(),
              new SwingEntityComboBoxModel(foreignEntityId, connectionProvider));
    }

    return EntityInputComponents.createForeignKeyLookupField(foreignKeyProperty, getCellValue(),
            new DefaultEntityLookupModel(foreignEntityId, connectionProvider));
  }
}
