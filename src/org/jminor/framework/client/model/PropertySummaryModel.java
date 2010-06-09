/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.model;

import org.jminor.common.model.Event;
import org.jminor.common.model.Util;
import org.jminor.framework.domain.Property;
import org.jminor.framework.i18n.FrameworkMessages;

import java.text.Format;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * A class for providing summaries of numerical table columns: sum, average, minimum, maximum and minimum & maximum.<br>
 * User: Bjorn Darri<br>
 * Date: 5.9.2009<br>
 * Time: 21:47:06<br>
 */
public class PropertySummaryModel {

  public static final Summary NONE = new None();
  public static final Summary SUM = new Sum();
  public static final Summary AVERAGE = new Average();
  public static final Summary MINIMUM = new Minimum();
  public static final Summary MAXIMUM = new Maximum();
  public static final Summary MINIMUM_MAXIMUM = new MinimumMaximum();

  private final Event evtSummaryTypeChanged = new Event();
  private final Event evtSummaryChanged = new Event();

  private final Property property;
  private final PropertyValueProvider valueProvider;
  private final Format format;

  private Summary summaryType = NONE;

  public PropertySummaryModel(final Property property, final PropertyValueProvider valueProvider) {
    this.property = property;
    this.valueProvider = valueProvider;
    this.format = initializeFormat(property);
    this.valueProvider.bindValuesChangedEvent(evtSummaryChanged);
    evtSummaryTypeChanged.addListener(evtSummaryChanged);
  }

  /**
   * @return the Property this summary model is based on
   */
  public Property getProperty() {
    return property;
  }

  /**
   * @param summaryType the type of summary to show
   */
  public void setSummaryType(final Summary summaryType) {
    Util.rejectNullValue(summaryType);
    if (!this.summaryType.equals(summaryType)) {
      this.summaryType = summaryType;
      evtSummaryTypeChanged.fire();
    }
  }

  public Summary getSummaryType() {
    return summaryType;
  }

  public List<Summary> getSummaryTypes() {
    if (property.isNumerical()) {
      return Arrays.asList(NONE, SUM, AVERAGE, MINIMUM, MAXIMUM, MINIMUM_MAXIMUM);
    }

    return new ArrayList<Summary>();
  }

  public String getSummaryText() {
    final String summaryTxt = summaryType.getSummary(valueProvider.getValues(), property, format);
    return summaryTxt.length() > 0 ? summaryTxt + (valueProvider.isValueSubset() ? "*" : "") : summaryTxt;
  }

  public Event eventSummaryChanged() {
    return evtSummaryChanged;
  }

  public Event eventSummaryTypeChanged() {
    return evtSummaryTypeChanged;
  }

  protected Format initializeFormat(final Property property) {
    return property.getFormat();
  }

  /**
   * Provides the values used when creating the summary value.
   */
  public interface PropertyValueProvider {

    /**
     * @return the values to base the summary on
     */
    Collection<?> getValues();

    /**
     * @return true if the values provided by <code>getValues()</code> is a subset of the total available values
     */
    boolean isValueSubset();

    /**
     * @param event the event to use when notifying changes to the underlying data which require a summary refresh
     */
    void bindValuesChangedEvent(final Event event);
  }

  /**
   * Provides a String containing a summary value based on a collection of values.
   */
  public interface Summary {
    String getSummary(final Collection<?> values, final Property property, final Format format);
  }

  private static class None implements Summary {

    @Override
    public String toString() {
      return FrameworkMessages.get(FrameworkMessages.NONE);
    }

    public String getSummary(final Collection<?> values, final Property property, final Format format) {
      return "";
    }
  }

  private static class Sum implements Summary {

    @Override
    public String toString() {
      return FrameworkMessages.get(FrameworkMessages.SUM);
    }

    public String getSummary(final Collection<?> values, final Property property, final Format format) {
      String txt = "";
      if (property.isInteger()) {
        long sum = 0;
        for (final Object obj : values) {
          sum += (Integer) obj;
        }
        txt = format.format(sum);
      }
      else if (property.isDouble()) {
        double sum = 0;
        for (final Object obj : values) {
          sum += (Double) obj;
        }
        txt = format.format(sum);
      }

      return txt;
    }
  }

  private static class Average implements Summary {

    @Override
    public String toString() {
      return FrameworkMessages.get(FrameworkMessages.AVERAGE);
    }

    public String getSummary(final Collection<?> values, final Property property, final Format format) {
      String txt = "";
      if (property.isInteger()) {
        double sum = 0;
        int count = 0;
        for (final Object obj : values) {
          sum += (Integer)obj;
          count++;
        }
        if (count > 0) {
          txt = format.format(sum / count);
        }
      }
      else if (property.isDouble()) {
        double sum = 0;
        int count = 0;
        for (final Object obj : values) {
          sum += (Double)obj;
          count++;
        }
        if (count > 0) {
          txt = format.format(sum / count);
        }
      }

      return txt;
    }
  }

  private static class Minimum implements Summary {

    @Override
    public String toString() {
      return FrameworkMessages.get(FrameworkMessages.MINIMUM);
    }

    public String getSummary(final Collection<?> values, final Property property, final Format format) {
      String txt = "";
      if (property.isInteger()) {
        int min = Integer.MAX_VALUE;
        for (final Object obj : values) {
          min = Math.min(min, (Integer) obj);
        }
        if (min != Integer.MAX_VALUE) {
          txt = format.format(min);
        }
      }
      else if (property.isDouble()) {
        double min = Double.MAX_VALUE;
        for (final Object obj : values) {
          min = Math.min(min, (Double) obj);
        }
        if (min != Double.MAX_VALUE) {
          txt = format.format(min);
        }
      }

      return txt;
    }
  }

  private static class Maximum implements Summary {

    @Override
    public String toString() {
      return FrameworkMessages.get(FrameworkMessages.MAXIMUM);
    }

    public String getSummary(final Collection<?> values, final Property property, final Format format) {
      String txt = "";
      if (property.isInteger()) {
        int max = Integer.MIN_VALUE;
        for (final Object obj : values) {
          max = Math.max(max, (Integer) obj);
        }
        if (max != Integer.MIN_VALUE) {
          txt = format.format(max);
        }
      }
      else if (property.isDouble()) {
        double max = Double.MIN_VALUE;
        for (final Object obj : values) {
          max = Math.max(max, (Double) obj);
        }
        if (max != Double.MIN_VALUE) {
          txt = format.format(max);
        }
      }

      return txt;
    }
  }

  private static class MinimumMaximum implements Summary {

    @Override
    public String toString() {
      return FrameworkMessages.get(FrameworkMessages.MINIMUM_AND_MAXIMUM);
    }

    public String getSummary(final Collection<?> values, final Property property, final Format format) {
      String txt = "";
      if (property.isInteger()) {
        int min = Integer.MAX_VALUE;
        int max = Integer.MIN_VALUE;
        for (final Object obj : values) {
          max = Math.max(max, (Integer) obj);
          min = Math.min(min, (Integer) obj);
        }
        if (max != Integer.MIN_VALUE) {
          txt = format.format(min) + "/" + format.format(max);
        }
      }
      else if (property.isDouble()) {
        double min = Double.MAX_VALUE;
        double max = Double.MIN_VALUE;
        for (final Object obj : values) {
          max = Math.max(max, (Double) obj);
          min = Math.min(min, (Double) obj);
        }
        if (max != Double.MIN_VALUE) {
          txt = format.format(min) + "/" + format.format(max);
        }
      }

      return txt;
    }
  }
}
