/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model.table;

import org.jminor.common.event.Event;
import org.jminor.common.event.EventDataListener;
import org.jminor.common.event.EventListener;
import org.jminor.common.event.Events;

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

  /** {@inheritDoc} */
  @Override
  public boolean isLocked() {
    return locked;
  }

  /** {@inheritDoc} */
  @Override
  public void setLocked(final boolean value) {
    this.locked = value;
  }

  /** {@inheritDoc} */
  @Override
  public final void setSummary(final Summary summary) {
    if (isLocked()) {
      throw new IllegalStateException("Summary model is locked");
    }
    requireNonNull(summary, "summary");
    if (!this.summary.equals(summary)) {
      this.summary = summary;
      summaryChangedEvent.fire(this.summary);
    }
  }

  /** {@inheritDoc} */
  @Override
  public final Summary getSummary() {
    return summary;
  }

  /** {@inheritDoc} */
  @Override
  public final List<Summary> getAvailableSummaries() {
    if (valueProvider.isNumerical()) {
      return asList(ColumnSummary.NONE, ColumnSummary.SUM, ColumnSummary.AVERAGE, ColumnSummary.MINIMUM,
              ColumnSummary.MAXIMUM, ColumnSummary.MINIMUM_MAXIMUM);
    }

    return emptyList();
  }

  /** {@inheritDoc} */
  @Override
  public final String getSummaryText() {
    return summary.getSummary(valueProvider);
  }

  /** {@inheritDoc} */
  @Override
  public final void addSummaryValueListener(final EventListener listener) {
    summaryValueChangedEvent.addListener(listener);
  }

  /** {@inheritDoc} */
  @Override
  public final void removeSummaryValueListener(final EventListener listener) {
    summaryValueChangedEvent.removeListener(listener);
  }

  /** {@inheritDoc} */
  @Override
  public final void addSummaryListener(final EventDataListener<Summary> listener) {
    summaryChangedEvent.addDataListener(listener);
  }

  /** {@inheritDoc} */
  @Override
  public final void removeSummaryListener(final EventDataListener listener) {
    summaryChangedEvent.removeDataListener(listener);
  }
}
