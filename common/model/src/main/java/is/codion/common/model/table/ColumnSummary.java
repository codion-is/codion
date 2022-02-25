/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.model.table;

import java.util.Collection;
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
    public <T extends Number> String getSummary(ColumnSummaryModel.ColumnValueProvider<T> valueProvider) {
      return "";
    }
  }, SUM {
    @Override
    public String toString() {
      return MESSAGES.getString("sum");
    }

    @Override
    public <T extends Number> String getSummary(ColumnSummaryModel.ColumnValueProvider<T> valueProvider) {
      Collection<T> values = valueProvider.getValues();
      if (!values.isEmpty()) {
        return addSubsetIndicator(valueProvider.format(values.stream()
                .filter(Objects::nonNull)
                .mapToDouble(Number::doubleValue).sum()), valueProvider);
      }

      return "";
    }
  }, AVERAGE {
    @Override
    public String toString() {
      return MESSAGES.getString("average");
    }

    @Override
    public <T extends Number> String getSummary(ColumnSummaryModel.ColumnValueProvider<T> valueProvider) {
      Collection<T> values = valueProvider.getValues();
      if (!values.isEmpty()) {
        OptionalDouble average = values.stream()
                .mapToDouble(value -> value == null ? 0d : value.doubleValue())
                .average();
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
    public <T extends Number> String getSummary(ColumnSummaryModel.ColumnValueProvider<T> valueProvider) {
      Collection<T> values = valueProvider.getValues();
      if (!values.isEmpty()) {
        OptionalDouble min = values.stream()
                .filter(Objects::nonNull)
                .mapToDouble(Number::doubleValue)
                .min();
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
    public <T extends Number> String getSummary(ColumnSummaryModel.ColumnValueProvider<T> valueProvider) {
      Collection<T> values = valueProvider.getValues();
      if (!values.isEmpty()) {
        OptionalDouble max = values.stream()
                .filter(Objects::nonNull)
                .mapToDouble(Number::doubleValue)
                .max();
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
    public <T extends Number> String getSummary(ColumnSummaryModel.ColumnValueProvider<T> valueProvider) {
      Collection<T> values = valueProvider.getValues();
      if (!values.isEmpty()) {
        OptionalDouble min = values.stream()
                .filter(Objects::nonNull)
                .mapToDouble(Number::doubleValue)
                .min();
        OptionalDouble max = values.stream()
                .filter(Objects::nonNull)
                .mapToDouble(Number::doubleValue)
                .max();
        if (min.isPresent() && max.isPresent()) {
          return addSubsetIndicator(valueProvider.format(min.getAsDouble()) + "/" + valueProvider.format(max.getAsDouble()), valueProvider);
        }
      }

      return "";
    }
  };

  private static final ResourceBundle MESSAGES = ResourceBundle.getBundle(ColumnSummary.class.getName());

  protected String addSubsetIndicator(String text, ColumnSummaryModel.ColumnValueProvider<?> valueProvider) {
    return text.isEmpty() ? text : text + (valueProvider.isValueSubset() ? "*" : "");
  }
}
