/*
 * Copyright (c) 2009 - 2024, Björn Darri Sigurðsson. All Rights Reserved.
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

  private final Value<Summary> summary = Value.value(ColumnSummary.NONE, ColumnSummary.NONE);
  private final Value<String> summaryText = Value.value();
  private final State locked = State.state();
  private final SummaryValueProvider<T> valueProvider;
  private final List<Summary> summaries = asList(ColumnSummary.values());

  DefaultColumnSummaryModel(SummaryValueProvider<T> valueProvider) {
    this.valueProvider = requireNonNull(valueProvider);
    this.summary.addValidator(summary -> {
      if (locked.get()) {
        throw new IllegalStateException("Summary model is locked");
      }
    });
    this.valueProvider.addListener(this::updateSummary);
    this.summary.addListener(this::updateSummary);
  }

  @Override
  public State locked() {
    return locked;
  }

  @Override
  public Value<Summary> summary() {
    return summary;
  }

  @Override
  public List<Summary> summaries() {
    return summaries;
  }

  @Override
  public ValueObserver<String> summaryText() {
    return summaryText.observer();
  }

  private void updateSummary() {
    summaryText.set(summary().get().summary(valueProvider));
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
    public boolean subset() {
      return subset;
    }
  }
}
