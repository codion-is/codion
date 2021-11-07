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

import java.util.Optional;

/**
 * A Swing {@link ConditionModelFactory} implementation
 * using ComboBoxModel for foreign key properties with small datasets
 */
public class SwingConditionModelFactory extends DefaultConditionModelFactory {

  public SwingConditionModelFactory(final EntityConnectionProvider connectionProvider) {
    super(connectionProvider);
  }

  @Override
  public Optional<ColumnConditionModel<ForeignKey, Entity>> createForeignKeyConditionModel(final ForeignKey foreignKey) {
    final EntityConnectionProvider connectionProvider = getConnectionProvider();
    if (connectionProvider.getEntities().getDefinition(foreignKey.getReferencedEntityType()).isSmallDataset()) {
      final SwingEntityComboBoxModel comboBoxModel = new SwingEntityComboBoxModel(foreignKey.getReferencedEntityType(), connectionProvider);
      comboBoxModel.setNullString(FilteredComboBoxModel.COMBO_BOX_NULL_VALUE_ITEM.get());

      return Optional.of(new SwingForeignKeyConditionModel(foreignKey, comboBoxModel));
    }

    return super.createForeignKeyConditionModel(foreignKey);
  }
}
