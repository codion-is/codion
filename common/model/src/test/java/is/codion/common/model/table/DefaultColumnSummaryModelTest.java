/*
 * Copyright (c) 2009 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.model.table;

import is.codion.common.model.table.ColumnSummaryModel.SummaryValueProvider;
import is.codion.common.model.table.ColumnSummaryModel.SummaryValues;

import org.junit.jupiter.api.Test;

import java.text.Format;
import java.text.NumberFormat;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.*;

public class DefaultColumnSummaryModelTest {

  private final Format numberFormat = NumberFormat.getInstance();

  final ColumnSummaryModel testIntModel = new DefaultColumnSummaryModel<>(new SummaryValueProvider<Integer>() {
    @Override
    public String format(Object value) {return numberFormat.format(value);}
    @Override
    public SummaryValues<Integer> values() {
      return ColumnSummaryModel.summaryValues(asList(1, 2, 3, null, 4, 5), false);
    }
    @Override
    public void addListener(Runnable event) {}
  });

  final ColumnSummaryModel testDoubleModel = new DefaultColumnSummaryModel<>(new SummaryValueProvider<Double>() {
    @Override
    public String format(Object value) {return numberFormat.format(value);}
    @Override
    public SummaryValues<Double> values() {
      return ColumnSummaryModel.summaryValues(asList(1.1, 2.2, 3.3, null, 4.4, 5.5), false);
    }
    @Override
    public void addListener(Runnable event) {}
  });

  @Test
  void test() {
    testIntModel.summaryValue().set(ColumnSummary.SUM);
    assertEquals(ColumnSummary.SUM, testIntModel.summaryValue().get());
    assertTrue(!testIntModel.summaries().isEmpty());
  }

  @Test
  void intSum() {
    testIntModel.summaryValue().set(ColumnSummary.SUM);
    assertEquals("15", testIntModel.summaryTextObserver().get());
  }

  @Test
  void intAverage() {
    testIntModel.summaryValue().set(ColumnSummary.AVERAGE);
    assertEquals(numberFormat.format(2.5), testIntModel.summaryTextObserver().get());
  }

  @Test
  void intMininum() {
    testIntModel.summaryValue().set(ColumnSummary.MINIMUM);
    assertEquals("1", testIntModel.summaryTextObserver().get());
  }

  @Test
  void intMaximum() {
    testIntModel.summaryValue().set(ColumnSummary.MAXIMUM);
    assertEquals("5", testIntModel.summaryTextObserver().get());
  }

  @Test
  void intMininumMaximum() {
    testIntModel.summaryValue().set(ColumnSummary.MINIMUM_MAXIMUM);
    assertEquals("1/5", testIntModel.summaryTextObserver().get());
  }

  @Test
  void doubleSum() {
    testDoubleModel.summaryValue().set(ColumnSummary.SUM);
    assertEquals(numberFormat.format(16.5), testDoubleModel.summaryTextObserver().get());
  }

  @Test
  void doubleAverage() {
    testDoubleModel.summaryValue().set(ColumnSummary.AVERAGE);
    assertEquals(numberFormat.format(2.75), testDoubleModel.summaryTextObserver().get());
  }

  @Test
  void doubleMininum() {
    testDoubleModel.summaryValue().set(ColumnSummary.MINIMUM);
    assertEquals(numberFormat.format(1.1), testDoubleModel.summaryTextObserver().get());
  }

  @Test
  void doubleMaximum() {
    testDoubleModel.summaryValue().set(ColumnSummary.MAXIMUM);
    assertEquals(numberFormat.format(5.5), testDoubleModel.summaryTextObserver().get());
  }

  @Test
  void doubleMininumMaximum() {
    testDoubleModel.summaryValue().set(ColumnSummary.MINIMUM_MAXIMUM);
    assertEquals(numberFormat.format(1.1) + "/" + numberFormat.format(5.5), testDoubleModel.summaryTextObserver().get());
  }

  @Test
  void locked() {
    testDoubleModel.lockedState().set(true);
    assertThrows(IllegalStateException.class, () -> testDoubleModel.summaryValue().set(ColumnSummary.MINIMUM_MAXIMUM));
  }
}
