/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.model;

import org.jminor.common.model.Event;
import org.jminor.framework.domain.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.Types;
import java.util.Arrays;
import java.util.Collection;

/**
 * User: Bjorn Darri
 * Date: 6.9.2009
 * Time: 16:47:57
 */
public class DefaultPropertySummaryModelTest {

  final PropertySummaryModel testIntModel = new DefaultPropertySummaryModel(Properties.columnProperty("TestProperty", Types.INTEGER),
          new PropertySummaryModel.PropertyValueProvider() {
    public Collection<?> getValues() {
      return Arrays.asList(1,2,3,4,5);
    }
    public boolean isValueSubset() {
      return false;
    }
    public void bindValuesChangedEvent(Event event) {}
  });

  final PropertySummaryModel testDoubleModel = new DefaultPropertySummaryModel(Properties.columnProperty("TestProperty", Types.DOUBLE),
          new PropertySummaryModel.PropertyValueProvider() {
    public Collection<?> getValues() {
      return Arrays.asList(1.1, 2.2, 3.3, 4.4, 5.5);
    }
    public boolean isValueSubset() {
      return false;
    }
    public void bindValuesChangedEvent(Event event) {}
  });

  @Test
  public void test() {
    testIntModel.setSummaryType(PropertySummaryModel.SummaryType.SUM);
    assertEquals("TestProperty", testIntModel.getProperty().getPropertyID());
    assertEquals(PropertySummaryModel.SummaryType.SUM, testIntModel.getSummaryType());
    assertTrue(testIntModel.getSummaryTypes().size() > 0);
    final ActionListener listener = new ActionListener() {
      public void actionPerformed(final ActionEvent e) {}
    };
    testIntModel.addSummaryListener(listener);
    testIntModel.addSummaryTypeListener(listener);
    testIntModel.removeSummaryListener(listener);
    testIntModel.removeSummaryTypeListener(listener);
  }

  @Test
  public void intSum() {
    testIntModel.setSummaryType(PropertySummaryModel.SummaryType.SUM);
    assertEquals("15", testIntModel.getSummaryText());
  }

  @Test
  public void intAverage() {
    testIntModel.setSummaryType(PropertySummaryModel.SummaryType.AVERAGE);
    assertEquals("3", testIntModel.getSummaryText());
  }

  @Test
  public void intMininum() {
    testIntModel.setSummaryType(PropertySummaryModel.SummaryType.MINIMUM);
    assertEquals("1", testIntModel.getSummaryText());
  }

  @Test
  public void intMaximum() {
    testIntModel.setSummaryType(PropertySummaryModel.SummaryType.MAXIMUM);
    assertEquals("5", testIntModel.getSummaryText());
  }

  @Test
  public void intMininumMaximum() {
    testIntModel.setSummaryType(PropertySummaryModel.SummaryType.MINIMUM_MAXIMUM);
    assertEquals("1/5", testIntModel.getSummaryText());
  }

  @Test
  public void doubleSum() {
    testDoubleModel.setSummaryType(PropertySummaryModel.SummaryType.SUM);
    assertEquals("16,5", testDoubleModel.getSummaryText());
  }

  @Test
  public void doubleAverage() {
    testDoubleModel.setSummaryType(PropertySummaryModel.SummaryType.AVERAGE);
    assertEquals("3,3", testDoubleModel.getSummaryText());
  }

  @Test
  public void doubleMininum() {
    testDoubleModel.setSummaryType(PropertySummaryModel.SummaryType.MINIMUM);
    assertEquals("1,1", testDoubleModel.getSummaryText());
  }

  @Test
  public void doubleMaximum() {
    testDoubleModel.setSummaryType(PropertySummaryModel.SummaryType.MAXIMUM);
    assertEquals("5,5", testDoubleModel.getSummaryText());
  }

  @Test
  public void doubleMininumMaximum() {
    testDoubleModel.setSummaryType(PropertySummaryModel.SummaryType.MINIMUM_MAXIMUM);
    assertEquals("1,1/5,5", testDoubleModel.getSummaryText());
  }
}
