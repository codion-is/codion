/*
 * Copyright (c) 2010 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.model;

import is.codion.common.model.table.ColumnConditionModel;
import is.codion.framework.domain.entity.Attribute;

/**
 * Responsible for creating {@link ColumnConditionModel}.
 */
public interface ConditionModelFactory {

  /**
   * Creates a {@link ColumnConditionModel} for the given attribute
   * @param <T> the column value type
   * @param <A> the Attribute type
   * @param attribute the Attribute for which to create a {@link ColumnConditionModel}
   * @return a {@link ColumnConditionModel} based on the given attribute, null if searching should not be available for this attribute
   */
  <T, A extends Attribute<T>> ColumnConditionModel<A, T> createConditionModel(A attribute);
}
