/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model.table;

import org.jminor.common.EventDataListener;
import org.jminor.common.EventListener;

import org.junit.jupiter.api.Test;

import java.text.Format;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.Collection;

import static org.junit.jupiter.api.Assertions.*;

public class DefaultColumnSummaryModelTest {

  private final Format numberFormat = NumberFormat.getInstance();

  final ColumnSummaryModel testIntModel = new DefaultColumnSummaryModel(new ColumnSummaryModel.ColumnValueProvider() {
    @Override
    public String format(final Object value) {return numberFormat.format(value);}
    @Override
    public boolean isNumerical() {
      return true;
    }
    @Override
    public boolean isInteger() {
      return true;
    }
    @Override
    public boolean isDouble() {
      return false;
    }
    @Override
    public Collection getValues() {
      return Arrays.asList(1, 2, 3, 4, 5);
    }
    @Override
    public boolean isValueSubset() {
      return false;
    }
    @Override
    public boolean isUseValueSubset() {
      return false;
    }
    @Override
    public void setUseValueSubset(final boolean value) {}
    @Override
    public void addValuesChangedListener(final EventListener event) {}
  });

  final ColumnSummaryModel testDoubleModel = new DefaultColumnSummaryModel(new ColumnSummaryModel.ColumnValueProvider() {
    @Override
    public String format(final Object value) {return numberFormat.format(value);}
    @Override
    public boolean isNumerical() {
      return true;
    }
    @Override
    public boolean isInteger() {
      return false;
    }
    @Override
    public boolean isDouble() {
      return true;
    }
    @Override
    public Collection getValues() {
      return Arrays.asList(1.1, 2.2, 3.3, 4.4, 5.5);
    }
    @Override
    public boolean isValueSubset() {
      return false;
    }
    @Override
    public boolean isUseValueSubset() {
      return false;
    }
    @Override
    public void setUseValueSubset(final boolean value) {}
    @Override
    public void addValuesChangedListener(final EventListener event) {}
  });

  @Test
  public void test() {
    testIntModel.setSummary(ColumnSummary.SUM);
    assertEquals(ColumnSummary.SUM, testIntModel.getSummary());
    assertTrue(testIntModel.getAvailableSummaries().size() > 0);
    final EventListener listener = () -> {};
    final EventDataListener<ColumnSummaryModel.Summary> summaryListener = data -> {};
    testIntModel.addSummaryValueListener(listener);
    testIntModel.addSummaryListener(summaryListener);
    testIntModel.removeSummaryValueListener(listener);
    testIntModel.removeSummaryListener(summaryListener);
  }

  @Test
  public void intSum() {
    testIntModel.setSummary(ColumnSummary.SUM);
    assertEquals("15", testIntModel.getSummaryText());
  }

  @Test
  public void intAverage() {
    testIntModel.setSummary(ColumnSummary.AVERAGE);
    assertEquals("3", testIntModel.getSummaryText());
  }

  @Test
  public void intMininum() {
    testIntModel.setSummary(ColumnSummary.MINIMUM);
    assertEquals("1", testIntModel.getSummaryText());
  }

  @Test
  public void intMaximum() {
    testIntModel.setSummary(ColumnSummary.MAXIMUM);
    assertEquals("5", testIntModel.getSummaryText());
  }

  @Test
  public void intMininumMaximum() {
    testIntModel.setSummary(ColumnSummary.MINIMUM_MAXIMUM);
    assertEquals("1/5", testIntModel.getSummaryText());
  }

  @Test
  public void doubleSum() {
    testDoubleModel.setSummary(ColumnSummary.SUM);
    assertEquals(numberFormat.format(16.5), testDoubleModel.getSummaryText());
  }

  @Test
  public void doubleAverage() {
    testDoubleModel.setSummary(ColumnSummary.AVERAGE);
    assertEquals(numberFormat.format(3.3), testDoubleModel.getSummaryText());
  }

  @Test
  public void doubleMininum() {
    testDoubleModel.setSummary(ColumnSummary.MINIMUM);
    assertEquals(numberFormat.format(1.1), testDoubleModel.getSummaryText());
  }

  @Test
  public void doubleMaximum() {
    testDoubleModel.setSummary(ColumnSummary.MAXIMUM);
    assertEquals(numberFormat.format(5.5), testDoubleModel.getSummaryText());
  }

  @Test
  public void doubleMininumMaximum() {
    testDoubleModel.setSummary(ColumnSummary.MINIMUM_MAXIMUM);
    assertEquals(numberFormat.format(1.1) + "/" + numberFormat.format(5.5), testDoubleModel.getSummaryText());
  }

  @Test
  public void locked() {
    testDoubleModel.setLocked(true);
    assertThrows(IllegalStateException.class, () -> testDoubleModel.setSummary(ColumnSummary.MINIMUM_MAXIMUM));
  }
}
