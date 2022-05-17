/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.model.table;

import is.codion.common.event.EventListener;
import is.codion.common.model.table.ColumnSummaryModel.SummaryValueProvider;

import org.junit.jupiter.api.Test;

import java.text.Format;
import java.text.NumberFormat;
import java.util.Collection;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.*;

public class DefaultColumnSummaryModelTest {

  private final Format numberFormat = NumberFormat.getInstance();

  final ColumnSummaryModel testIntModel = new DefaultColumnSummaryModel<>(new SummaryValueProvider<Integer>() {
    @Override
    public String format(Object value) {return numberFormat.format(value);}
    @Override
    public Collection<Integer> getValues() {
      return asList(1, 2, 3, null, 4, 5);
    }
    @Override
    public boolean isValueSubset() {
      return false;
    }
    @Override
    public void addValuesChangedListener(EventListener event) {}
  });

  final ColumnSummaryModel testDoubleModel = new DefaultColumnSummaryModel<>(new SummaryValueProvider<Double>() {
    @Override
    public String format(Object value) {return numberFormat.format(value);}
    @Override
    public Collection<Double> getValues() {
      return asList(1.1, 2.2, 3.3, null, 4.4, 5.5);
    }
    @Override
    public boolean isValueSubset() {
      return false;
    }
    @Override
    public void addValuesChangedListener(EventListener event) {}
  });

  @Test
  void test() {
    testIntModel.getSummaryValue().set(ColumnSummary.SUM);
    assertEquals(ColumnSummary.SUM, testIntModel.getSummaryValue().get());
    assertTrue(testIntModel.getAvailableSummaries().size() > 0);
  }

  @Test
  void intSum() {
    testIntModel.getSummaryValue().set(ColumnSummary.SUM);
    assertEquals("15", testIntModel.getSummaryTextObserver().get());
  }

  @Test
  void intAverage() {
    testIntModel.getSummaryValue().set(ColumnSummary.AVERAGE);
    assertEquals(numberFormat.format(2.5), testIntModel.getSummaryTextObserver().get());
  }

  @Test
  void intMininum() {
    testIntModel.getSummaryValue().set(ColumnSummary.MINIMUM);
    assertEquals("1", testIntModel.getSummaryTextObserver().get());
  }

  @Test
  void intMaximum() {
    testIntModel.getSummaryValue().set(ColumnSummary.MAXIMUM);
    assertEquals("5", testIntModel.getSummaryTextObserver().get());
  }

  @Test
  void intMininumMaximum() {
    testIntModel.getSummaryValue().set(ColumnSummary.MINIMUM_MAXIMUM);
    assertEquals("1/5", testIntModel.getSummaryTextObserver().get());
  }

  @Test
  void doubleSum() {
    testDoubleModel.getSummaryValue().set(ColumnSummary.SUM);
    assertEquals(numberFormat.format(16.5), testDoubleModel.getSummaryTextObserver().get());
  }

  @Test
  void doubleAverage() {
    testDoubleModel.getSummaryValue().set(ColumnSummary.AVERAGE);
    assertEquals(numberFormat.format(2.75), testDoubleModel.getSummaryTextObserver().get());
  }

  @Test
  void doubleMininum() {
    testDoubleModel.getSummaryValue().set(ColumnSummary.MINIMUM);
    assertEquals(numberFormat.format(1.1), testDoubleModel.getSummaryTextObserver().get());
  }

  @Test
  void doubleMaximum() {
    testDoubleModel.getSummaryValue().set(ColumnSummary.MAXIMUM);
    assertEquals(numberFormat.format(5.5), testDoubleModel.getSummaryTextObserver().get());
  }

  @Test
  void doubleMininumMaximum() {
    testDoubleModel.getSummaryValue().set(ColumnSummary.MINIMUM_MAXIMUM);
    assertEquals(numberFormat.format(1.1) + "/" + numberFormat.format(5.5), testDoubleModel.getSummaryTextObserver().get());
  }

  @Test
  void locked() {
    testDoubleModel.getLockedState().set(true);
    assertThrows(IllegalStateException.class, () -> testDoubleModel.getSummaryValue().set(ColumnSummary.MINIMUM_MAXIMUM));
  }
}
