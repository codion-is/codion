/*
 * Copyright (c) 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.model.table;

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
  Optional<ColumnSummaryModel> columnSummaryModel(C columnIdentifier);

  /**
   * @param summaryModelFactory the summary model factory
   * @return a new {@link TableSummaryModel} instance
   * @param <C> the column identifier type
   */
  static <C> TableSummaryModel<C> tableSummaryModel(ColumnSummaryModel.Factory<C> summaryModelFactory) {
    return new DefaultTableSummaryModel<>(summaryModelFactory);
  }
}
