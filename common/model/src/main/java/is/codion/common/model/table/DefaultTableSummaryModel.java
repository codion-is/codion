/*
 * Copyright (c) 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.model.table;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

final class DefaultTableSummaryModel<C> implements TableSummaryModel<C> {

  private final ColumnSummaryModel.Factory<C> summaryModelFactory;
  private final Map<C, ColumnSummaryModel> columnSummaryModels = new HashMap<>();

  DefaultTableSummaryModel(ColumnSummaryModel.Factory<C> summaryModelFactory) {
    this.summaryModelFactory = requireNonNull(summaryModelFactory);
  }

  @Override
  public Optional<ColumnSummaryModel> summaryModel(C columnIdentifier) {
    return Optional.ofNullable(columnSummaryModels.computeIfAbsent(requireNonNull(columnIdentifier),
            identifier -> summaryModelFactory.createSummaryModel(columnIdentifier)));
  }
}
