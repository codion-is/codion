/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.model;

import is.codion.common.model.table.ColumnConditionModel;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.domain.entity.Attribute;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.ForeignKey;
import is.codion.framework.domain.property.ColumnProperty;

import java.util.Optional;

/**
 * Specifies an object responsible for creating condition models
 */
public interface ConditionModelFactory {

  /**
   * Initializes a {@link ColumnConditionModel} for the given property
   * @param <T> the column value type
   * @param property the Property for which to create a {@link ColumnConditionModel}
   * @return a {@link ColumnConditionModel} for the given property, an empty Optional if searching
   * should not be allowed for this property
   */
  <T> Optional<ColumnConditionModel<Attribute<?>, T>> createColumnConditionModel(ColumnProperty<T> property);

  /**
   * Initializes a {@link ColumnConditionModel} for the given property
   * @param foreignKey the Property for which to create a {@link ColumnConditionModel}
   * @param connectionProvider the EntityConnectionProvider instance to use
   * @return a {@link ColumnConditionModel} for the given property, an empty Optional if if searching
   * should not be allowed for this property
   */
  Optional<ColumnConditionModel<ForeignKey, Entity>> createForeignKeyConditionModel(
          ForeignKey foreignKey, EntityConnectionProvider connectionProvider);
}
