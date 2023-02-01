/*
 * Copyright (c) 2009 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.model.table;

import is.codion.common.state.State;
import is.codion.common.value.Value;
import is.codion.common.value.ValueObserver;

import java.util.Collection;
import java.util.List;

import static java.util.Arrays.asList;
import static java.util.Objects.requireNonNull;

/**
 * A default ColumnSummaryModel implementation.
 * @param <T> the column type
 */
final class DefaultColumnSummaryModel<T extends Number> implements ColumnSummaryModel {

  private final Value<Summary> summaryValue = Value.value(ColumnSummary.NONE, ColumnSummary.NONE);
  private final Value<String> summaryTextValue = Value.value();
  private final State lockedState = State.state();
  private final SummaryValueProvider<T> valueProvider;
  private final List<Summary> summaries = asList(ColumnSummary.values());

  DefaultColumnSummaryModel(SummaryValueProvider<T> valueProvider) {
    this.valueProvider = requireNonNull(valueProvider);
    this.summaryValue.addValidator(summary -> {
      if (lockedState.get()) {
        throw new IllegalStateException("Summary model is locked");
      }
    });
    this.valueProvider.addValuesListener(this::updateSummary);
    this.summaryValue.addListener(this::updateSummary);
  }

  @Override
  public State lockedState() {
    return lockedState;
  }

  @Override
  public Value<Summary> summaryValue() {
    return summaryValue;
  }

  @Override
  public List<Summary> availableSummaries() {
    return summaries;
  }

  @Override
  public ValueObserver<String> summaryTextObserver() {
    return summaryTextValue.observer();
  }

  private void updateSummary() {
    summaryTextValue.set(summaryValue().get().summary(valueProvider));
  }

  static final class DefaultSummaryValues<T extends Number> implements ColumnSummaryModel.SummaryValues<T> {

    private final Collection<T> values;
    private final boolean subset;

    DefaultSummaryValues(Collection<T> values, boolean subset) {
      this.values = requireNonNull(values, "values");
      this.subset = subset;
    }

    @Override
    public Collection<T> values() {
      return values;
    }

    @Override
    public boolean isSubset() {
      return subset;
    }
  }
}
