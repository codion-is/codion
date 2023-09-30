/*
 * Copyright (c) 2015 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.model.table;

import is.codion.common.model.table.ColumnSummaryModel.SummaryValueProvider;
import is.codion.common.model.table.ColumnSummaryModel.SummaryValues;

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
    public <T extends Number> String summary(SummaryValueProvider<T> valueProvider) {
      return "";
    }
  }, SUM {
    @Override
    public String toString() {
      return MESSAGES.getString("sum");
    }

    @Override
    public <T extends Number> String summary(SummaryValueProvider<T> valueProvider) {
      SummaryValues<T> summaryValues = valueProvider.values();
      if (!summaryValues.values().isEmpty()) {
        return addSubsetIndicator(valueProvider.format(summaryValues.values().stream()
                .filter(Objects::nonNull)
                .mapToDouble(Number::doubleValue).sum()), summaryValues.subset());
      }

      return "";
    }
  }, AVERAGE {
    @Override
    public String toString() {
      return MESSAGES.getString("average");
    }

    @Override
    public <T extends Number> String summary(SummaryValueProvider<T> valueProvider) {
      SummaryValues<T> summaryValues = valueProvider.values();
      if (!summaryValues.values().isEmpty()) {
        OptionalDouble average = summaryValues.values().stream()
                .mapToDouble(value -> value == null ? 0d : value.doubleValue())
                .average();
        if (average.isPresent()) {
          return addSubsetIndicator(valueProvider.format(average.getAsDouble()), summaryValues.subset());
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
    public <T extends Number> String summary(SummaryValueProvider<T> valueProvider) {
      SummaryValues<T> summaryValues = valueProvider.values();
      if (!summaryValues.values().isEmpty()) {
        OptionalDouble min = summaryValues.values().stream()
                .filter(Objects::nonNull)
                .mapToDouble(Number::doubleValue)
                .min();
        if (min.isPresent()) {
          return addSubsetIndicator(valueProvider.format(min.getAsDouble()), summaryValues.subset());
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
    public <T extends Number> String summary(SummaryValueProvider<T> valueProvider) {
      SummaryValues<T> summaryValues = valueProvider.values();
      if (!summaryValues.values().isEmpty()) {
        OptionalDouble max = summaryValues.values().stream()
                .filter(Objects::nonNull)
                .mapToDouble(Number::doubleValue)
                .max();
        if (max.isPresent()) {
          return addSubsetIndicator(valueProvider.format(max.getAsDouble()), summaryValues.subset());
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
    public <T extends Number> String summary(SummaryValueProvider<T> valueProvider) {
      SummaryValues<T> summaryValues = valueProvider.values();
      if (!summaryValues.values().isEmpty()) {
        OptionalDouble min = summaryValues.values().stream()
                .filter(Objects::nonNull)
                .mapToDouble(Number::doubleValue)
                .min();
        OptionalDouble max = summaryValues.values().stream()
                .filter(Objects::nonNull)
                .mapToDouble(Number::doubleValue)
                .max();
        if (min.isPresent() && max.isPresent()) {
          return addSubsetIndicator(valueProvider.format(min.getAsDouble()) + "/" + valueProvider.format(max.getAsDouble()), summaryValues.subset());
        }
      }

      return "";
    }
  };

  private static final ResourceBundle MESSAGES = ResourceBundle.getBundle(ColumnSummary.class.getName());

  protected String addSubsetIndicator(String text, boolean subset) {
    return text.isEmpty() ? text : text + (subset ? "*" : "");
  }
}
