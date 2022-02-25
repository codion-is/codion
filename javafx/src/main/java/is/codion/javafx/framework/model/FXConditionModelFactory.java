/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.javafx.framework.model;

import is.codion.common.model.table.ColumnConditionModel;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.domain.entity.Attribute;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.ForeignKey;
import is.codion.framework.model.DefaultConditionModelFactory;

/**
 * Provides foreign key condition models based on {@link ObservableEntityList} for
 * entities based on small datasets, see {@link EntityDefinition#isSmallDataset()}
 */
public class FXConditionModelFactory extends DefaultConditionModelFactory {

  public FXConditionModelFactory(final EntityConnectionProvider connectionProvider) {
    super(connectionProvider);
  }

  @Override
  public <T, A extends Attribute<T>> ColumnConditionModel<A, T> createConditionModel(final A attribute) {
    if (attribute instanceof ForeignKey) {
      ForeignKey foreignKey = (ForeignKey) attribute;
      if (getDefinition(foreignKey.getReferencedEntityType()).isSmallDataset()) {
        return (ColumnConditionModel<A, T>) new FXForeignKeyConditionListModel(foreignKey,
                new ObservableEntityList(foreignKey.getReferencedEntityType(), getConnectionProvider()));
      }
    }

    return super.createConditionModel(attribute);
  }
}
