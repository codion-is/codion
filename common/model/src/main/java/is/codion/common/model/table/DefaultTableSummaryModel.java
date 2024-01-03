/*
 * Copyright (c) 2023 - 2024, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.model.table;

import is.codion.common.model.table.ColumnSummaryModel.SummaryValueProvider;

import java.text.Format;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

final class DefaultTableSummaryModel<C> implements TableSummaryModel<C> {

  private final SummaryValueProvider.Factory<C> summaryModelFactory;
  private final Map<C, ColumnSummaryModel> columnSummaryModels = new HashMap<>();

  DefaultTableSummaryModel(SummaryValueProvider.Factory<C> summaryModelFactory) {
    this.summaryModelFactory = requireNonNull(summaryModelFactory);
  }

  @Override
  public Optional<ColumnSummaryModel> summaryModel(C columnIdentifier) {
    return Optional.ofNullable(columnSummaryModels.computeIfAbsent(columnIdentifier, k ->
            createSummaryModel(k, NumberFormat.getInstance()).orElse(null)));
  }

  private Optional<ColumnSummaryModel> createSummaryModel(C columnIdentifier, Format format) {
    return summaryModelFactory.createSummaryValueProvider(columnIdentifier, format)
            .map(ColumnSummaryModel::columnSummaryModel);
  }
}
