/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model.table;

import java.util.Collection;
import java.util.Locale;
import java.util.Objects;
import java.util.OptionalDouble;
import java.util.ResourceBundle;

/**
 * The summary types available to the default summary model
 */
public enum ColumnSummary implements ColumnSummaryModel.Summary {
  NONE {
    @Override
    public String toString() {
      return MESSAGES.getString("none");
    }

    @Override
    public String getSummary(final ColumnSummaryModel.ColumnValueProvider valueProvider) {
      return "";
    }
  }, SUM {
    @Override
    public String toString() {
      return MESSAGES.getString("sum");
    }

    @Override
    public String getSummary(final ColumnSummaryModel.ColumnValueProvider valueProvider) {
      final Collection values = valueProvider.getValues();
      if (!values.isEmpty()) {
        return addSubsetIndicator(valueProvider.format(values.stream().filter(Objects::nonNull)
                .mapToDouble(value -> ((Number) value).doubleValue()).sum()), valueProvider);
      }

      return "";
    }
  }, AVERAGE {
    @Override
    public String toString() {
      return MESSAGES.getString("average");
    }

    @Override
    public String getSummary(final ColumnSummaryModel.ColumnValueProvider valueProvider) {
      final Collection values = valueProvider.getValues();
      if (!values.isEmpty()) {
        final OptionalDouble average = values.stream().mapToDouble(value -> value == null ? 0d : ((Number) value).doubleValue()).average();
        if (average.isPresent()) {
          return addSubsetIndicator(valueProvider.format(average.getAsDouble()), valueProvider);
        }
      }

      return "";
    }
  }, MINIMUM {
    @Override
    public String toString() {
      return MESSAGES.getString("minimum");
    }

    @Override
    public String getSummary(final ColumnSummaryModel.ColumnValueProvider valueProvider) {
      final Collection values = valueProvider.getValues();
      if (!values.isEmpty()) {
        final OptionalDouble min = values.stream().filter(Objects::nonNull).mapToDouble(value -> ((Number) value).doubleValue()).min();
        if (min.isPresent()) {
          return addSubsetIndicator(valueProvider.format(min.getAsDouble()), valueProvider);
        }
      }

      return "";
    }
  }, MAXIMUM {
    @Override
    public String toString() {
      return MESSAGES.getString("maximum");
    }

    @Override
    public String getSummary(final ColumnSummaryModel.ColumnValueProvider valueProvider) {
      final Collection values = valueProvider.getValues();
      if (!values.isEmpty()) {
        final OptionalDouble max = values.stream().filter(Objects::nonNull).mapToDouble(value -> ((Number) value).doubleValue()).max();
        if (max.isPresent()) {
          return addSubsetIndicator(valueProvider.format(max.getAsDouble()), valueProvider);
        }
      }

      return "";
    }
  }, MINIMUM_MAXIMUM {
    @Override
    public String toString() {
      return MESSAGES.getString("minimum_and_maximum");
    }

    @Override
    public String getSummary(final ColumnSummaryModel.ColumnValueProvider valueProvider) {
      final Collection values = valueProvider.getValues();
      if (!values.isEmpty()) {
        final OptionalDouble min = values.stream().filter(Objects::nonNull).mapToDouble(value -> ((Number) value).doubleValue()).min();
        final OptionalDouble max = values.stream().filter(Objects::nonNull).mapToDouble(value -> ((Number) value).doubleValue()).max();
        if (min.isPresent() && max.isPresent()) {
          return addSubsetIndicator(valueProvider.format(min.getAsDouble()) + "/" + valueProvider.format(max.getAsDouble()), valueProvider);
        }
      }

      return "";
    }
  };

  private static final ResourceBundle MESSAGES = ResourceBundle.getBundle(ColumnSummary.class.getName(), Locale.getDefault());

  protected String addSubsetIndicator(final String text, final ColumnSummaryModel.ColumnValueProvider valueProvider) {
    return text.isEmpty() ? text : text + (valueProvider.isValueSubset() ? "*" : "");
  }
}
