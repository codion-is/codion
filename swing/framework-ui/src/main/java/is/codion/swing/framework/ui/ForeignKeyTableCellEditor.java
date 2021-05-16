/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.ui;

import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.ForeignKey;
import is.codion.framework.domain.property.ForeignKeyProperty;
import is.codion.framework.model.DefaultEntitySearchModel;
import is.codion.swing.framework.model.SwingEntityComboBoxModel;

import javax.swing.JComponent;
import javax.swing.table.TableCellEditor;

/**
 * A {@link TableCellEditor} implementation for {@link ForeignKeyProperty} columns in a {@link EntityTablePanel}.
 */
final class ForeignKeyTableCellEditor extends EntityTableCellEditor<Entity> {

  private final EntityConnectionProvider connectionProvider;

  ForeignKeyTableCellEditor(final EntityConnectionProvider connectionProvider, final EntityDefinition entityDefinition,
                            final ForeignKey foreignKey) {
    super(entityDefinition, foreignKey);
    this.connectionProvider = connectionProvider;
  }

  //TODO handle Enter key correctly
  @Override
  protected JComponent initializeEditorComponent() {
    final ForeignKey foreignKey = (ForeignKey) getAttribute();
    final EntityType<?> foreignEntityType = foreignKey.getReferencedEntityType();

    if (connectionProvider.getEntities().getDefinition(foreignEntityType).isSmallDataset()) {
      final SwingEntityComboBoxModel comboBoxModel = new SwingEntityComboBoxModel(foreignEntityType, connectionProvider);
      comboBoxModel.refresh();

      return getInputComponents().createForeignKeyComboBox(foreignKey, getCellValue(), comboBoxModel);
    }

    return getInputComponents().createForeignKeySearchField(foreignKey, getCellValue(),
            new DefaultEntitySearchModel(foreignEntityType, connectionProvider));
  }
}
