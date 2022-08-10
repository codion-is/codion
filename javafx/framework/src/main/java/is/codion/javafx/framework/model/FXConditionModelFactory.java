/*
 * Copyright (c) 2010 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.javafx.framework.model;

import is.codion.common.model.table.ColumnConditionModel;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.domain.entity.Attribute;
import is.codion.framework.domain.entity.ForeignKey;
import is.codion.framework.model.ConditionModelFactory;
import is.codion.framework.model.DefaultConditionModelFactory;

/**
 * A {@link ConditionModelFactory} implementation using {@link ObservableEntityList} for foreign keys based on small datasets
 */
public class FXConditionModelFactory extends DefaultConditionModelFactory {

  public FXConditionModelFactory(EntityConnectionProvider connectionProvider) {
    super(connectionProvider);
  }

  @Override
  public <T, A extends Attribute<T>> ColumnConditionModel<A, T> createConditionModel(A attribute) {
    if (attribute instanceof ForeignKey) {
      ForeignKey foreignKey = (ForeignKey) attribute;
      if (definition(foreignKey.referencedType()).isSmallDataset()) {
        return (ColumnConditionModel<A, T>) new FXForeignKeyConditionListModel(foreignKey,
                new ObservableEntityList(foreignKey.referencedType(), connectionProvider()));
      }
    }

    return super.createConditionModel(attribute);
  }
}
