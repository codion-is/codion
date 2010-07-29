/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.model;

import org.jminor.common.model.Event;
import org.jminor.common.model.Util;
import org.jminor.framework.domain.Property;
import org.jminor.framework.i18n.FrameworkMessages;

import java.awt.event.ActionListener;
import java.text.Format;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DefaultPropertySummaryModel implements PropertySummaryModel {

  private static final Summary NONE = new None();
  private static final Summary SUM = new Sum();
  private static final Summary AVERAGE = new Average();
  private static final Summary MINIMUM = new Minimum();
  private static final Summary MAXIMUM = new Maximum();
  private static final Summary MINIMUM_MAXIMUM = new MinimumMaximum();

  private final Event evtSummaryTypeChanged = new Event();
  private final Event evtSummaryChanged = new Event();

  private final Property property;
  private final PropertyValueProvider valueProvider;

  private SummaryType summaryType = SummaryType.NONE;

  public DefaultPropertySummaryModel(final Property property, final PropertyValueProvider valueProvider) {
    this.property = property;
    this.valueProvider = valueProvider;
    this.valueProvider.bindValuesChangedEvent(evtSummaryChanged);
    evtSummaryTypeChanged.addListener(evtSummaryChanged);
  }

  /**
   * @return the Property this summary model is based on
   */
  public final Property getProperty() {
    return property;
  }

  /**
   * @param summaryType the type of summary to show
   */
  public final void setSummaryType(final SummaryType summaryType) {
    Util.rejectNullValue(summaryType, "summaryType");
    if (!this.summaryType.equals(summaryType)) {
      this.summaryType = summaryType;
      evtSummaryTypeChanged.fire();
    }
  }

  public final SummaryType getSummaryType() {
    return summaryType;
  }

  public final List<SummaryType> getSummaryTypes() {
    if (property.isNumerical()) {
      return Arrays.asList(SummaryType.NONE, SummaryType.SUM, SummaryType.AVERAGE, SummaryType.MINIMUM, SummaryType.MAXIMUM, SummaryType.MINIMUM_MAXIMUM);
    }

    return new ArrayList<SummaryType>();
  }

  public final String getSummaryText() {
    switch (summaryType) {
      case NONE:
        return NONE.getSummary(valueProvider, property);
      case AVERAGE:
        return AVERAGE.getSummary(valueProvider, property);
      case MAXIMUM:
        return MAXIMUM.getSummary(valueProvider, property);
      case MINIMUM:
        return MINIMUM.getSummary(valueProvider, property);
      case MINIMUM_MAXIMUM:
        return MINIMUM_MAXIMUM.getSummary(valueProvider, property);
      case SUM:
        return SUM.getSummary(valueProvider, property);
      default:
        throw new RuntimeException("Unknown SummaryType: " + summaryType);
    }
  }

  public final void addSummaryListener(final ActionListener listener) {
    evtSummaryChanged.addListener(listener);
  }

  public final void addSummaryTypeListener(final ActionListener listener) {
    evtSummaryTypeChanged.addListener(listener);
  }

  public final void removeSummaryListener(final ActionListener listener) {
    evtSummaryChanged.removeListener(listener);
  }

  public final void removeSummaryTypeListener(final ActionListener listener) {
    evtSummaryTypeChanged.removeListener(listener);
  }

  /**
   * Provides a String containing a summary value based on a collection of values.
   */
  private abstract static class Summary {
    abstract String getSummary(final PropertyValueProvider valueProvider, final Property property);

    String addSubsetIndicator(final String txt, final PropertyValueProvider valueProvider) {
      return txt.length() > 0 ? txt + (valueProvider.isValueSubset() ? "*" : "") : txt;
    }
  }

  private static class None extends Summary {

    @Override
    public String toString() {
      return FrameworkMessages.get(FrameworkMessages.NONE);
    }

    @Override
    public String getSummary(final PropertyValueProvider valueProvider, final Property property) {
      return "";
    }
  }

  private static class Sum extends Summary {

    @Override
    public String toString() {
      return FrameworkMessages.get(FrameworkMessages.SUM);
    }

    @Override
    public String getSummary(final PropertyValueProvider valueProvider, final Property property) {
      final Format format = property.getFormat();
      String txt = "";
      if (property.isInteger()) {
        long sum = 0;
        for (final Object obj : valueProvider.getValues()) {
          sum += (Integer) obj;
        }
        txt = format.format(sum);
      }
      else if (property.isDouble()) {
        double sum = 0;
        for (final Object obj : valueProvider.getValues()) {
          sum += (Double) obj;
        }
        txt = format.format(sum);
      }

      return addSubsetIndicator(txt, valueProvider);
    }
  }

  private static class Average extends Summary {

    @Override
    public String toString() {
      return FrameworkMessages.get(FrameworkMessages.AVERAGE);
    }

    @Override
    public String getSummary(final PropertyValueProvider valueProvider, final Property property) {
      final Format format = property.getFormat();
      String txt = "";
      if (property.isInteger()) {
        double sum = 0;
        int count = 0;
        for (final Object obj : valueProvider.getValues()) {
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
        for (final Object obj : valueProvider.getValues()) {
          sum += (Double)obj;
          count++;
        }
        if (count > 0) {
          txt = format.format(sum / count);
        }
      }

      return addSubsetIndicator(txt, valueProvider);
    }
  }

  private static class Minimum extends Summary {

    @Override
    public String toString() {
      return FrameworkMessages.get(FrameworkMessages.MINIMUM);
    }

    @Override
    public String getSummary(final PropertyValueProvider valueProvider, final Property property) {
      final Format format = property.getFormat();
      String txt = "";
      if (property.isInteger()) {
        int min = Integer.MAX_VALUE;
        for (final Object obj : valueProvider.getValues()) {
          min = Math.min(min, (Integer) obj);
        }
        if (min != Integer.MAX_VALUE) {
          txt = format.format(min);
        }
      }
      else if (property.isDouble()) {
        double min = Double.MAX_VALUE;
        for (final Object obj : valueProvider.getValues()) {
          min = Math.min(min, (Double) obj);
        }
        if (min != Double.MAX_VALUE) {
          txt = format.format(min);
        }
      }

      return addSubsetIndicator(txt, valueProvider);
    }
  }

  private static class Maximum extends Summary {

    @Override
    public String toString() {
      return FrameworkMessages.get(FrameworkMessages.MAXIMUM);
    }

    @Override
    public String getSummary(final PropertyValueProvider valueProvider, final Property property) {
      final Format format = property.getFormat();
      String txt = "";
      if (property.isInteger()) {
        int max = Integer.MIN_VALUE;
        for (final Object obj : valueProvider.getValues()) {
          max = Math.max(max, (Integer) obj);
        }
        if (max != Integer.MIN_VALUE) {
          txt = format.format(max);
        }
      }
      else if (property.isDouble()) {
        double max = Double.MIN_VALUE;
        for (final Object obj : valueProvider.getValues()) {
          max = Math.max(max, (Double) obj);
        }
        if (max != Double.MIN_VALUE) {
          txt = format.format(max);
        }
      }

      return addSubsetIndicator(txt, valueProvider);
    }
  }

  private static class MinimumMaximum extends Summary {

    @Override
    public String toString() {
      return FrameworkMessages.get(FrameworkMessages.MINIMUM_AND_MAXIMUM);
    }

    @Override
    public String getSummary(final PropertyValueProvider valueProvider, final Property property) {
      final Format format = property.getFormat();
      String txt = "";
      if (property.isInteger()) {
        int min = Integer.MAX_VALUE;
        int max = Integer.MIN_VALUE;
        for (final Object obj : valueProvider.getValues()) {
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
        for (final Object obj : valueProvider.getValues()) {
          max = Math.max(max, (Double) obj);
          min = Math.min(min, (Double) obj);
        }
        if (max != Double.MIN_VALUE) {
          txt = format.format(min) + "/" + format.format(max);
        }
      }

      return addSubsetIndicator(txt, valueProvider);
    }
  }
}
