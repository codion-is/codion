package org.jminor.framework.client.model;

import org.jminor.common.model.Event;
import org.jminor.framework.domain.Property;
import org.jminor.framework.domain.Type;

import junit.framework.TestCase;

import java.util.Arrays;
import java.util.Collection;

/**
 * User: Björn Darri
 * Date: 6.9.2009
 * Time: 16:47:57
 */
public class PropertySummaryModelTest extends TestCase {

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

  public void testIntSum() {
    testIntModel.setSummaryType(PropertySummaryModel.SUM);
    assertEquals("15", testIntModel.getSummaryText());
  }

  public void testIntAverage() {
    testIntModel.setSummaryType(PropertySummaryModel.AVERAGE);
    assertEquals("3", testIntModel.getSummaryText());
  }

  public void testIntMininum() {
    testIntModel.setSummaryType(PropertySummaryModel.MINIMUM);
    assertEquals("1", testIntModel.getSummaryText());
  }

  public void testIntMaximum() {
    testIntModel.setSummaryType(PropertySummaryModel.MAXIMUM);
    assertEquals("5", testIntModel.getSummaryText());
  }

  public void testIntMininumMaximum() {
    testIntModel.setSummaryType(PropertySummaryModel.MINIMUM_MAXIMUM);
    assertEquals("1/5", testIntModel.getSummaryText());
  }

  public void testDoubleSum() {
    testDoubleModel.setSummaryType(PropertySummaryModel.SUM);
    assertEquals("16,5", testDoubleModel.getSummaryText());
  }

  public void testDoubleAverage() {
    testDoubleModel.setSummaryType(PropertySummaryModel.AVERAGE);
    assertEquals("3,3", testDoubleModel.getSummaryText());
  }

  public void testDoubleMininum() {
    testDoubleModel.setSummaryType(PropertySummaryModel.MINIMUM);
    assertEquals("1,1", testDoubleModel.getSummaryText());
  }

  public void testDoubleMaximum() {
    testDoubleModel.setSummaryType(PropertySummaryModel.MAXIMUM);
    assertEquals("5,5", testDoubleModel.getSummaryText());
  }

  public void testDoubleMininumMaximum() {
    testDoubleModel.setSummaryType(PropertySummaryModel.MINIMUM_MAXIMUM);
    assertEquals("1,1/5,5", testDoubleModel.getSummaryText());
  }
}
