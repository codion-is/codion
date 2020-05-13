/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package dev.codion.common.model.table;

import dev.codion.common.event.Event;
import dev.codion.common.event.EventDataListener;
import dev.codion.common.event.EventListener;
import dev.codion.common.event.Events;

import java.util.List;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Objects.requireNonNull;

/**
 * A default ColumnSummaryModel implementation.
 */
public class DefaultColumnSummaryModel implements ColumnSummaryModel {

  private final Event<Summary> summaryChangedEvent = Events.event();
  private final Event summaryValueChangedEvent = Events.event();

  private final ColumnValueProvider valueProvider;

  private Summary summary = ColumnSummary.NONE;
  private boolean locked = false;

  /**
   * Instantiates a new DefaultColumnSummaryModel
   * @param valueProvider the property value provider
   */
  public DefaultColumnSummaryModel(final ColumnValueProvider valueProvider) {
    this.valueProvider = valueProvider;
    this.valueProvider.addValuesChangedListener(summaryValueChangedEvent);
    this.summaryChangedEvent.addListener(summaryValueChangedEvent);
  }

  @Override
  public boolean isLocked() {
    return locked;
  }

  @Override
  public void setLocked(final boolean locked) {
    this.locked = locked;
  }

  @Override
  public final void setSummary(final Summary summary) {
    if (isLocked()) {
      throw new IllegalStateException("Summary model is locked");
    }
    requireNonNull(summary, "summary");
    if (!this.summary.equals(summary)) {
      this.summary = summary;
      summaryChangedEvent.onEvent(this.summary);
    }
  }

  @Override
  public final Summary getSummary() {
    return summary;
  }

  @Override
  public final List<Summary> getAvailableSummaries() {
    if (valueProvider.isNumerical()) {
      return asList(ColumnSummary.NONE, ColumnSummary.SUM, ColumnSummary.AVERAGE, ColumnSummary.MINIMUM,
              ColumnSummary.MAXIMUM, ColumnSummary.MINIMUM_MAXIMUM);
    }

    return emptyList();
  }

  @Override
  public final String getSummaryText() {
    return summary.getSummary(valueProvider);
  }

  @Override
  public final void addSummaryValueListener(final EventListener listener) {
    summaryValueChangedEvent.addListener(listener);
  }

  @Override
  public final void removeSummaryValueListener(final EventListener listener) {
    summaryValueChangedEvent.removeListener(listener);
  }

  @Override
  public final void addSummaryListener(final EventDataListener<Summary> listener) {
    summaryChangedEvent.addDataListener(listener);
  }

  @Override
  public final void removeSummaryListener(final EventDataListener<Summary> listener) {
    summaryChangedEvent.removeDataListener(listener);
  }
}
