/*
 * Copyright (c) 2004 - 2015, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.common.model.table;

import org.jminor.common.i18n.Messages;

import java.util.Collection;

/**
 * The summary types available to the default summary model
 */
public enum ColumnSummary implements ColumnSummaryModel.Summary {
  NONE {
    @Override
    public String toString() {
      return Messages.get(Messages.NONE);
    }

    @Override
    public String getSummary(final ColumnSummaryModel.ColumnValueProvider valueProvider) {
      return "";
    }
  }, SUM {
    @Override
    public String toString() {
      return Messages.get(Messages.SUM);
    }

    @Override
    public String getSummary(final ColumnSummaryModel.ColumnValueProvider valueProvider) {
      String txt = "";
      final Collection values = valueProvider.getValues();
      if (!values.isEmpty()) {
        if (valueProvider.isInteger()) {
          long sum = 0;
          for (final Object obj : values) {
            sum += (Integer) obj;
          }
          txt = valueProvider.format(sum);
        }
        else if (valueProvider.isDouble()) {
          double sum = 0;
          for (final Object obj : values) {
            sum += (Double) obj;
          }
          txt = valueProvider.format(sum);
        }
      }

      return addSubsetIndicator(txt, valueProvider);
    }
  }, AVERAGE {
    @Override
    public String toString() {
      return Messages.get(Messages.AVERAGE);
    }

    @Override
    public String getSummary(final ColumnSummaryModel.ColumnValueProvider valueProvider) {
      String txt = "";
      final Collection values = valueProvider.getValues();
      if (!values.isEmpty()) {
        if (valueProvider.isInteger()) {
          double sum = 0;
          int count = 0;
          for (final Object obj : values) {
            sum += (Integer)obj;
            count++;
          }
          txt = valueProvider.format(sum / count);
        }
        else if (valueProvider.isDouble()) {
          double sum = 0;
          int count = 0;
          for (final Object obj : values) {
            sum += (Double)obj;
            count++;
          }
          txt = valueProvider.format(sum / count);
        }
      }

      return addSubsetIndicator(txt, valueProvider);
    }
  }, MINIMUM {
    @Override
    public String toString() {
      return Messages.get(Messages.MINIMUM);
    }

    @Override
    public String getSummary(final ColumnSummaryModel.ColumnValueProvider valueProvider) {
      String txt = "";
      final Collection values = valueProvider.getValues();
      if (!values.isEmpty()) {
        if (valueProvider.isInteger()) {
          int min = Integer.MAX_VALUE;
          for (final Object obj : values) {
            min = Math.min(min, (Integer) obj);
          }
          txt = valueProvider.format(min);
        }
        else if (valueProvider.isDouble()) {
          double min = Double.MAX_VALUE;
          for (final Object obj : values) {
            min = Math.min(min, (Double) obj);
          }
          txt = valueProvider.format(min);
        }
      }
      return addSubsetIndicator(txt, valueProvider);
    }
  }, MAXIMUM {
    @Override
    public String toString() {
      return Messages.get(Messages.MAXIMUM);
    }

    @Override
    public String getSummary(final ColumnSummaryModel.ColumnValueProvider valueProvider) {
      String txt = "";
      final Collection values = valueProvider.getValues();
      if (!values.isEmpty()) {
        if (valueProvider.isInteger()) {
          int max = 0;
          for (final Object obj : values) {
            max = Math.max(max, (Integer) obj);
          }
          txt = valueProvider.format(max);
        }
        else if (valueProvider.isDouble()) {
          double max = 0;
          for (final Object obj : values) {
            max = Math.max(max, (Double) obj);
          }
          txt = valueProvider.format(max);
        }
      }

      return addSubsetIndicator(txt, valueProvider);
    }
  }, MINIMUM_MAXIMUM {
    @Override
    public String toString() {
      return Messages.get(Messages.MINIMUM_AND_MAXIMUM);
    }

    @Override
    public String getSummary(final ColumnSummaryModel.ColumnValueProvider valueProvider) {
      String txt = "";
      final Collection values = valueProvider.getValues();
      if (!values.isEmpty()) {
        if (valueProvider.isInteger()) {
          int min = Integer.MAX_VALUE;
          int max = Integer.MIN_VALUE;
          for (final Object obj : values) {
            max = Math.max(max, (Integer) obj);
            min = Math.min(min, (Integer) obj);
          }
          txt = valueProvider.format(min) + "/" + valueProvider.format(max);
        }
        else if (valueProvider.isDouble()) {
          double min = Double.MAX_VALUE;
          double max = Double.MIN_VALUE;
          for (final Object obj : values) {
            max = Math.max(max, (Double) obj);
            min = Math.min(min, (Double) obj);
          }
          txt = valueProvider.format(min) + "/" + valueProvider.format(max);
        }
      }

      return addSubsetIndicator(txt, valueProvider);
    }
  };

  protected String addSubsetIndicator(final String txt, final ColumnSummaryModel.ColumnValueProvider valueProvider) {
    if (valueProvider.isUseValueSubset()) {
      return txt.length() != 0 ? txt + (valueProvider.isValueSubset() ? "*" : "") : txt;
    }

    return txt;
  }
}
