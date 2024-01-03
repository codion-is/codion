/*
 * Copyright (c) 2023 - 2024, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.model.table;

import java.util.Collection;
import java.util.Map;

import static java.util.Objects.requireNonNull;

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
   * The condition model associated with {@code columnIdentifier}
   * @param <T> the column value type
   * @param columnIdentifier the column identifier for which to retrieve the {@link ColumnConditionModel}
   * @return the {@link ColumnConditionModel} for the {@code columnIdentifier}
   * @throws IllegalArgumentException in case no condition model exists for the given columnIdentifier
   */
  <T> ColumnConditionModel<? extends C, T> conditionModel(C columnIdentifier);

  /**
   * Clears the search state of all the condition models, disables them and
   * resets the operator to {@link is.codion.common.Operator#EQUAL}
   */
  void clear();

  /**
   * @return true if any of the underlying condition models are enabled
   */
  boolean enabled();

  /**
   * @param columnIdentifier the column identifier
   * @return true if the condition model behind column with {@code columnIdentifier} is enabled
   */
  boolean enabled(C columnIdentifier);

  /**
   * @param listener a listener notified each time the condition changes
   */
  void addChangeListener(Runnable listener);

  /**
   * @param listener the listener to remove
   */
  void removeChangeListener(Runnable listener);

  /**
   * Instantiates a new {@link TableConditionModel}
   * @param conditionModels the column condition models
   * @param <C> the column identifier type
   * @return a new {@link TableConditionModel}
   */
  static <C> TableConditionModel<C> tableConditionModel(Collection<ColumnConditionModel<C, ?>> conditionModels) {
    return new DefaultTableConditionModel<>(requireNonNull(conditionModels));
  }
}
