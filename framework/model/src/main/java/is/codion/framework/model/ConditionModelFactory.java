/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.model;

import is.codion.common.model.table.ColumnConditionModel;
import is.codion.framework.domain.entity.Attribute;

import java.util.Optional;

/**
 * Specifies an object responsible for creating condition models
 */
public interface ConditionModelFactory {

  /**
   * Initializes a {@link ColumnConditionModel} for the given attribute
   * @param <T> the column value type
   * @param <A> the Attribute type
   * @param attribute the Attribute for which to create a {@link ColumnConditionModel}
   * @return a {@link ColumnConditionModel} based on the given attribute, an empty Optional if searching should not be available for this attribute
   */
  <T, A extends Attribute<T>> Optional<ColumnConditionModel<A, T>> createConditionModel(final A attribute);
}
