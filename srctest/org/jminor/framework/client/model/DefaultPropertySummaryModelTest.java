/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.model;

import org.jminor.common.model.Event;
import org.jminor.common.model.EventListener;
import org.jminor.framework.domain.Properties;

import org.junit.Test;

import java.sql.Types;
import java.text.Format;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.Collection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class DefaultPropertySummaryModelTest {

  final PropertySummaryModel testIntModel = new DefaultPropertySummaryModel(Properties.columnProperty("TestProperty", Types.INTEGER),
          new PropertySummaryModel.PropertyValueProvider() {
    @Override
    public Collection getValues() {
      return Arrays.asList(1,2,3,4,5);
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
    public void bindValuesChangedEvent(final Event event) {}
  });

  final PropertySummaryModel testDoubleModel = new DefaultPropertySummaryModel(Properties.columnProperty("TestProperty", Types.DOUBLE),
          new PropertySummaryModel.PropertyValueProvider() {
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
    public void bindValuesChangedEvent(final Event event) {}
  });

  private final Format numberFormat = NumberFormat.getInstance();

  @Test
  public void test() {
    testIntModel.setCurrentSummary(DefaultPropertySummaryModel.SummaryType.SUM);
    assertEquals("TestProperty", testIntModel.getProperty().getPropertyID());
    assertEquals(DefaultPropertySummaryModel.SummaryType.SUM, testIntModel.getCurrentSummary());
    assertTrue(testIntModel.getAvailableSummaries().size() > 0);
    final EventListener listener = new EventListener() {
      @Override
      public void eventOccurred() {}
    };
    testIntModel.addSummaryValueListener(listener);
    testIntModel.addSummaryListener(listener);
    testIntModel.removeSummaryValueListener(listener);
    testIntModel.removeSummaryListener(listener);
  }

  @Test
  public void intSum() {
    testIntModel.setCurrentSummary(DefaultPropertySummaryModel.SummaryType.SUM);
    assertEquals("15", testIntModel.getSummaryText());
  }

  @Test
  public void intAverage() {
    testIntModel.setCurrentSummary(DefaultPropertySummaryModel.SummaryType.AVERAGE);
    assertEquals("3", testIntModel.getSummaryText());
  }

  @Test
  public void intMininum() {
    testIntModel.setCurrentSummary(DefaultPropertySummaryModel.SummaryType.MINIMUM);
    assertEquals("1", testIntModel.getSummaryText());
  }

  @Test
  public void intMaximum() {
    testIntModel.setCurrentSummary(DefaultPropertySummaryModel.SummaryType.MAXIMUM);
    assertEquals("5", testIntModel.getSummaryText());
  }

  @Test
  public void intMininumMaximum() {
    testIntModel.setCurrentSummary(DefaultPropertySummaryModel.SummaryType.MINIMUM_MAXIMUM);
    assertEquals("1/5", testIntModel.getSummaryText());
  }

  @Test
  public void doubleSum() {
    testDoubleModel.setCurrentSummary(DefaultPropertySummaryModel.SummaryType.SUM);
    assertEquals(numberFormat.format(16.5), testDoubleModel.getSummaryText());
  }

  @Test
  public void doubleAverage() {
    testDoubleModel.setCurrentSummary(DefaultPropertySummaryModel.SummaryType.AVERAGE);
    assertEquals(numberFormat.format(3.3), testDoubleModel.getSummaryText());
  }

  @Test
  public void doubleMininum() {
    testDoubleModel.setCurrentSummary(DefaultPropertySummaryModel.SummaryType.MINIMUM);
    assertEquals(numberFormat.format(1.1), testDoubleModel.getSummaryText());
  }

  @Test
  public void doubleMaximum() {
    testDoubleModel.setCurrentSummary(DefaultPropertySummaryModel.SummaryType.MAXIMUM);
    assertEquals(numberFormat.format(5.5), testDoubleModel.getSummaryText());
  }

  @Test
  public void doubleMininumMaximum() {
    testDoubleModel.setCurrentSummary(DefaultPropertySummaryModel.SummaryType.MINIMUM_MAXIMUM);
    assertEquals(numberFormat.format(1.1) + "/" + numberFormat.format(5.5), testDoubleModel.getSummaryText());
  }

  @Test(expected = IllegalStateException.class)
  public void locked() {
    testDoubleModel.setLocked(true);
    testDoubleModel.setCurrentSummary(DefaultPropertySummaryModel.SummaryType.MINIMUM_MAXIMUM);
  }
}
