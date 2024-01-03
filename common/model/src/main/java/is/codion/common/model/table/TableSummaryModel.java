/*
 * Copyright (c) 2023 - 2024, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.model.table;

import is.codion.common.model.table.ColumnSummaryModel.SummaryValueProvider;

import java.util.Optional;

/**
 * @param <C> the column identifier type
 */
public interface TableSummaryModel<C> {

  /**
   * Returns the {@link ColumnSummaryModel} associated with {@code columnIdentifier}
   * @param columnIdentifier the column identifier
   * @return the ColumnSummaryModel for the column identified by the given identifier, an empty Optional if none is available
   */
  Optional<ColumnSummaryModel> summaryModel(C columnIdentifier);

  /**
   * @param summaryModelFactory the summary model factory
   * @param <C> the column identifier type
   * @return a new {@link TableSummaryModel} instance
   */
  static <C> TableSummaryModel<C> tableSummaryModel(SummaryValueProvider.Factory<C> summaryModelFactory) {
    return new DefaultTableSummaryModel<>(summaryModelFactory);
  }
}
