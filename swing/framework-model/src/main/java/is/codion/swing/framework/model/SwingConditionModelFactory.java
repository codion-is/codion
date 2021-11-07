/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.model;

import is.codion.common.model.combobox.FilteredComboBoxModel;
import is.codion.common.model.table.ColumnConditionModel;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.domain.entity.Attribute;
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
  public <T, A extends Attribute<T>> Optional<ColumnConditionModel<A, T>> createConditionModel(final A attribute) {
    if (attribute instanceof ForeignKey) {
      final ForeignKey foreignKey = (ForeignKey) attribute;
      if (getDefinition(foreignKey.getReferencedEntityType()).isSmallDataset()) {
        final SwingEntityComboBoxModel comboBoxModel = new SwingEntityComboBoxModel(foreignKey.getReferencedEntityType(), getConnectionProvider());
        comboBoxModel.setNullString(FilteredComboBoxModel.COMBO_BOX_NULL_VALUE_ITEM.get());

        return Optional.of((ColumnConditionModel<A, T>) new SwingForeignKeyConditionModel(foreignKey, comboBoxModel));
      }
    }

    return super.createConditionModel(attribute);
  }
}
