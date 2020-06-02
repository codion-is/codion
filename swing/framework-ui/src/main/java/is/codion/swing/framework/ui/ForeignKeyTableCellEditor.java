/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.ui;

import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.domain.entity.EntityId;
import is.codion.framework.domain.property.ForeignKeyProperty;
import is.codion.framework.model.DefaultEntityLookupModel;
import is.codion.swing.framework.model.SwingEntityComboBoxModel;

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
    final EntityId foreignEntityId = foreignKeyProperty.getForeignEntityId();

    if (connectionProvider.getEntities().getDefinition(foreignEntityId).isSmallDataset()) {
      return EntityInputComponents.createForeignKeyComboBox(foreignKeyProperty, getCellValue(),
              new SwingEntityComboBoxModel(foreignEntityId, connectionProvider));
    }

    return EntityInputComponents.createForeignKeyLookupField(foreignKeyProperty, getCellValue(),
            new DefaultEntityLookupModel(foreignEntityId, connectionProvider));
  }
}
