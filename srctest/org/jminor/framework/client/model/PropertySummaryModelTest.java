package org.jminor.framework.client.model;

import org.jminor.common.model.Event;
import org.jminor.framework.domain.Property;
import org.jminor.framework.domain.Type;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collection;

/**
 * User: Björn Darri
 * Date: 6.9.2009
 * Time: 16:47:57
 */
public class PropertySummaryModelTest {

  final PropertySummaryModel testIntModel = new PropertySummaryModel(new Property("TestProperty", Type.INT),
          new PropertySummaryModel.PropertyValueProvider() {
    public Collection<?> getValues() {
      return Arrays.asList(1,2,3,4,5);
    }
    public boolean isValueSubset() {
      return false;
    }
    public void bindValuesChangedEvent(Event event) {}
  });

  final PropertySummaryModel testDoubleModel = new PropertySummaryModel(new Property("TestProperty", Type.DOUBLE),
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
  public void intSum() {
    testIntModel.setSummaryType(PropertySummaryModel.SUM);
    assertEquals("15", testIntModel.getSummaryText());
  }

  @Test
  public void intAverage() {
    testIntModel.setSummaryType(PropertySummaryModel.AVERAGE);
    assertEquals("3", testIntModel.getSummaryText());
  }

  @Test
  public void intMininum() {
    testIntModel.setSummaryType(PropertySummaryModel.MINIMUM);
    assertEquals("1", testIntModel.getSummaryText());
  }

  @Test
  public void intMaximum() {
    testIntModel.setSummaryType(PropertySummaryModel.MAXIMUM);
    assertEquals("5", testIntModel.getSummaryText());
  }

  @Test
  public void intMininumMaximum() {
    testIntModel.setSummaryType(PropertySummaryModel.MINIMUM_MAXIMUM);
    assertEquals("1/5", testIntModel.getSummaryText());
  }

  @Test
  public void doubleSum() {
    testDoubleModel.setSummaryType(PropertySummaryModel.SUM);
    assertEquals("16,5", testDoubleModel.getSummaryText());
  }

  @Test
  public void doubleAverage() {
    testDoubleModel.setSummaryType(PropertySummaryModel.AVERAGE);
    assertEquals("3,3", testDoubleModel.getSummaryText());
  }

  @Test
  public void doubleMininum() {
    testDoubleModel.setSummaryType(PropertySummaryModel.MINIMUM);
    assertEquals("1,1", testDoubleModel.getSummaryText());
  }

  @Test
  public void doubleMaximum() {
    testDoubleModel.setSummaryType(PropertySummaryModel.MAXIMUM);
    assertEquals("5,5", testDoubleModel.getSummaryText());
  }

  @Test
  public void doubleMininumMaximum() {
    testDoubleModel.setSummaryType(PropertySummaryModel.MINIMUM_MAXIMUM);
    assertEquals("1,1/5,5", testDoubleModel.getSummaryText());
  }
}
