/*
 * This file is part of Codion.
 *
 * Codion is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Codion is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Codion.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (c) 2009 - 2023, Björn Darri Sigurðsson.
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
    public boolean isSubset() {
      return subset;
    }
  }
}
