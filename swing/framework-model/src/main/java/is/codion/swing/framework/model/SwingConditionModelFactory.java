/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.model;

import is.codion.common.model.combobox.FilteredComboBoxModel;
import is.codion.common.model.table.ColumnConditionModel;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.ForeignKey;
import is.codion.framework.model.ConditionModelFactory;
import is.codion.framework.model.DefaultConditionModelFactory;

/**
 * A Swing {@link ConditionModelFactory} implementation
 * using ComboBoxModel for foreign key properties with small datasets
 */
public class SwingConditionModelFactory extends DefaultConditionModelFactory {

  @Override
  public ColumnConditionModel<ForeignKey, Entity> createForeignKeyConditionModel(
          final ForeignKey foreignKey, final EntityConnectionProvider connectionProvider) {
    if (connectionProvider.getEntities().getDefinition(foreignKey.getReferencedEntityType()).isSmallDataset()) {
      final SwingEntityComboBoxModel comboBoxModel = new SwingEntityComboBoxModel(foreignKey.getReferencedEntityType(), connectionProvider);
      comboBoxModel.setNullString(FilteredComboBoxModel.COMBO_BOX_NULL_VALUE_ITEM.get());

      return new SwingForeignKeyConditionModel(foreignKey, comboBoxModel);
    }

    return super.createForeignKeyConditionModel(foreignKey, connectionProvider);
  }
}
