/*
 * Copyright (c) 2016 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.model;

import is.codion.common.model.combobox.FilteredComboBoxModel;
import is.codion.common.model.table.ColumnConditionModel;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.domain.entity.Attribute;
import is.codion.framework.domain.entity.ForeignKey;
import is.codion.framework.model.ConditionModelFactory;
import is.codion.framework.model.DefaultConditionModelFactory;

/**
 * A Swing {@link ConditionModelFactory} implementation
 * using ComboBoxModel for foreign key properties with small datasets
 */
public class SwingConditionModelFactory extends DefaultConditionModelFactory {

  public SwingConditionModelFactory(EntityConnectionProvider connectionProvider) {
    super(connectionProvider);
  }

  @Override
  public <T, A extends Attribute<T>> ColumnConditionModel<A, T> createConditionModel(A attribute) {
    if (attribute instanceof ForeignKey) {
      ForeignKey foreignKey = (ForeignKey) attribute;
      if (getDefinition(foreignKey.getReferencedEntityType()).isSmallDataset()) {
        return (ColumnConditionModel<A, T>) new SwingForeignKeyConditionModel(foreignKey, createComboBoxModel(foreignKey));
      }
    }

    return super.createConditionModel(attribute);
  }

  /**
   * Creates a combo box model based on the given foreign key
   * @param foreignKey the foreign key
   * @return a combo box model based on the given foreign key
   */
  protected SwingEntityComboBoxModel createComboBoxModel(ForeignKey foreignKey) {
    SwingEntityComboBoxModel comboBoxModel = new SwingEntityComboBoxModel(foreignKey.getReferencedEntityType(), getConnectionProvider());
    comboBoxModel.setIncludeNull(FilteredComboBoxModel.COMBO_BOX_NULL_CAPTION.get());

    return comboBoxModel;
  }
}
