/*
 * Copyright (c) 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.model.table;

import is.codion.common.event.EventListener;

import java.util.Collection;
import java.util.Map;

/**
 * @param <C> the column identifier type
 * @see #tableConditionModel(Collection)
 */
public interface TableConditionModel<C> {

  /**
   * @return an unmodifiable map containing the condition models available in this table condition model, mapped to their respective column identifiers
   */
  Map<C, ColumnConditionModel<C, ?>> conditionModels();

  /**
   * The filter model associated with {@code columnIdentifier}
   * @param <T> the column value type
   * @param columnIdentifier the column identifier for which to retrieve the {@link ColumnConditionModel}
   * @return the {@link ColumnConditionModel} for the {@code columnIdentifier}
   * @throws IllegalArgumentException in case no condition model exists for the given columnIdentifier
   */
  <T> ColumnConditionModel<? extends C, T> conditionModel(C columnIdentifier);

  /**
   * Clears the search state of all the filter models, disables them and
   * resets the operator to {@link is.codion.common.Operator#EQUAL}
   */
  void clear();

  /**
   * @return true if any of the underlying filter models are enabled
   */
  boolean isEnabled();

  /**
   * @param columnIdentifier the column identifier
   * @return true if the filter model behind column with {@code columnIdentifier} is enabled
   */
  boolean isEnabled(C columnIdentifier);

  /**
   * @param listener a listener notified each time the filter changes
   */
  void addChangeListener(EventListener listener);

  /**
   * @param listener the listener to remove
   */
  void removeChangeListener(EventListener listener);

  /**
   * Instantiates a new {@link TableConditionModel}
   * @param conditionModel the condition model
   * @return a new {@link TableConditionModel}
   * @param <C> the column identifier type
   */
  static <C> TableConditionModel<C> tableConditionModel(Collection<ColumnConditionModel<C, ?>> conditionModel) {
    return new DefaultTableConditionModel<>(conditionModel);
  }
}
