/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.model.table;

import is.codion.common.event.Event;
import is.codion.common.event.EventDataListener;
import is.codion.common.event.EventListener;

import java.util.List;

import static java.util.Arrays.asList;
import static java.util.Objects.requireNonNull;

/**
 * A default ColumnSummaryModel implementation.
 * @param <T> the column type
 */
public class DefaultColumnSummaryModel<T extends Number> implements ColumnSummaryModel {

  private final Event<Summary> summaryChangedEvent = Event.event();
  private final Event<?> summaryValueChangedEvent = Event.event();

  private final ColumnValueProvider<T> valueProvider;
  private final List<Summary> summaries = asList(ColumnSummary.values());

  private Summary summary = ColumnSummary.NONE;
  private boolean locked = false;

  /**
   * Instantiates a new DefaultColumnSummaryModel
   * @param valueProvider the property value provider
   */
  public DefaultColumnSummaryModel(final ColumnValueProvider<T> valueProvider) {
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
    return summaries;
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
