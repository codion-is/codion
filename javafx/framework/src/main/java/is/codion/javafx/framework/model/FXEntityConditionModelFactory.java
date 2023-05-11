/*
 * Copyright (c) 2010 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.javafx.framework.model;

import is.codion.common.model.table.ColumnConditionModel;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.domain.entity.Attribute;
import is.codion.framework.domain.entity.ForeignKey;
import is.codion.framework.model.EntityConditionModelFactory;

/**
 * A {@link ColumnConditionModel.Factory} implementation using {@link EntityObservableList} for foreign keys based on small datasets
 */
public class FXEntityConditionModelFactory extends EntityConditionModelFactory {

  public FXEntityConditionModelFactory(EntityConnectionProvider connectionProvider) {
    super(connectionProvider);
  }

  @Override
  public ColumnConditionModel<? extends Attribute<?>, ?> createConditionModel(Attribute<?> attribute) {
    if (attribute instanceof ForeignKey) {
      ForeignKey foreignKey = (ForeignKey) attribute;
      if (definition(foreignKey.referencedType()).isSmallDataset()) {
        return EntityListConditionModel.entityListConditionModel(foreignKey,
                new EntityObservableList(foreignKey.referencedType(), connectionProvider()));
      }
    }

    return super.createConditionModel(attribute);
  }
}
