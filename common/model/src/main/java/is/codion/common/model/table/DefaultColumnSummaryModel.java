/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.model.table;

import is.codion.common.state.State;
import is.codion.common.value.Value;
import is.codion.common.value.ValueObserver;

import java.util.List;

import static java.util.Arrays.asList;
import static java.util.Objects.requireNonNull;

/**
 * A default ColumnSummaryModel implementation.
 * @param <T> the column type
 */
public final class DefaultColumnSummaryModel<T extends Number> implements ColumnSummaryModel {

  private final Value<Summary> summaryValue = Value.value(ColumnSummary.NONE);
  private final Value<String> summaryTextValue = Value.value();
  private final State lockedState = State.state();
  private final ColumnValueProvider<T> valueProvider;
  private final List<Summary> summaries = asList(ColumnSummary.values());

  /**
   * Instantiates a new DefaultColumnSummaryModel
   * @param valueProvider the property value provider
   */
  public DefaultColumnSummaryModel(final ColumnValueProvider<T> valueProvider) {
    this.valueProvider = requireNonNull(valueProvider);
    this.summaryValue.addValidator(summary -> {
      if (lockedState.get()) {
        throw new IllegalStateException("Summary model is locked");
      }
    });
    this.valueProvider.addValuesChangedListener(this::updateSummary);
    this.summaryValue.addListener(this::updateSummary);
  }

  @Override
  public State getLockedState() {
    return lockedState;
  }

  @Override
  public Value<Summary> getSummaryValue() {
    return summaryValue;
  }

  @Override
  public List<Summary> getAvailableSummaries() {
    return summaries;
  }

  @Override
  public ValueObserver<String> getSummaryTextObserver() {
    return summaryTextValue.getObserver();
  }

  private void updateSummary() {
    summaryTextValue.set(getSummaryValue().get().getSummary(valueProvider));
  }
}
