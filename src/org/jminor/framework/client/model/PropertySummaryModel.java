package org.jminor.framework.client.model;

import org.jminor.common.model.Event;
import org.jminor.framework.domain.Property;
import org.jminor.framework.domain.Type;
import org.jminor.framework.i18n.FrameworkMessages;

import java.text.Format;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * User: Björn Darri
 * Date: 5.9.2009
 * Time: 21:47:06
 *
 * The following summary types are implemented: Sum, average, minimum, maximum and minimum & maximum
 */
public class PropertySummaryModel {

  public static final Summary NONE = new None();
  public static final Summary SUM = new Sum();
  public static final Summary AVERAGE = new Average();
  public static final Summary MINIMUM = new Minimum();
  public static final Summary MAXIMUM = new Maximum();
  public static final Summary MINIMUM_MAXIMUM = new MinimumMaximum();

  public final Event evtSummaryTypeChanged = new Event();
  public final Event evtSummaryChanged = new Event();

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
    if (summaryType == null)
      throw new IllegalArgumentException("Use PropertySummaryModel.NONE instead of null");
    if (!this.summaryType.equals(summaryType)) {
      this.summaryType = summaryType;
      evtSummaryTypeChanged.fire();
    }
  }

  public Summary getSummaryType() {
    return summaryType;
  }

  public List<Summary> getSummaryTypes() {
    if (property.getPropertyType() == Type.INT || property.getPropertyType() == Type.DOUBLE)
      return Arrays.asList(NONE, SUM, AVERAGE, MINIMUM, MAXIMUM, MINIMUM_MAXIMUM);

    return new ArrayList<Summary>();
  }

  public String getSummaryText() {
    final String summaryTxt = summaryType.getSummary(valueProvider.getValues(), property.getPropertyType(), format);
    return summaryTxt.length() > 0 ? summaryTxt + (valueProvider.isValueSubset() ? "*" : "") : summaryTxt;
  }

  protected Format initializeFormat(final Property property) {
    final NumberFormat format = NumberFormat.getInstance();
    if (property.getMaximumFractionDigits() != -1)
      format.setMaximumFractionDigits(property.getMaximumFractionDigits());
    else
      format.setMaximumFractionDigits(4);
    format.setGroupingUsed(property.useNumberFormatGrouping());

    return format;
  }

  public interface PropertyValueProvider {
    public Collection<?> getValues();
    public boolean isValueSubset();
    public void bindValuesChangedEvent(final Event event);
  }

  public interface Summary {
    public String getSummary(final Collection<?> values, final Type propertyType, final Format format);
  }

  private static class None implements Summary {

    @Override
    public String toString() {
      return FrameworkMessages.get(FrameworkMessages.NONE);
    }

    public String getSummary(final Collection<?> values, final Type propertyType, final Format format) {
      return "";
    }
  }

  private static class Sum implements Summary {

    @Override
    public String toString() {
      return FrameworkMessages.get(FrameworkMessages.SUM);
    }

    public String getSummary(final Collection<?> values, final Type propertyType, final Format format) {
      String txt = "";
      if (propertyType == Type.INT) {
        int sum = 0;
        for (final Object obj : values)
          sum += (Integer)obj;
        txt = format.format(sum);
      }
      else if (propertyType == Type.DOUBLE) {
        double sum = 0;
        for (final Object obj : values)
          sum += (Double)obj;
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

    public String getSummary(final Collection<?> values, final Type propertyType, final Format format) {
      String txt = "";
      if (propertyType == Type.INT) {
        double sum = 0;
        int count = 0;
        for (final Object obj : values) {
          sum += (Integer)obj;
          count++;
        }
        if (count > 0)
          txt = format.format(sum/count);
      }
      else if (propertyType == Type.DOUBLE) {
        double sum = 0;
        int count = 0;
        for (final Object obj : values) {
          sum += (Double)obj;
          count++;
        }
        if (count > 0)
          txt = format.format(sum/count);
      }

      return txt;
    }
  }

  private static class Minimum implements Summary {

    @Override
    public String toString() {
      return FrameworkMessages.get(FrameworkMessages.MINIMUM);
    }

    public String getSummary(final Collection<?> values, final Type propertyType, final Format format) {
      String txt = "";
      if (propertyType == Type.INT) {
        int min = Integer.MAX_VALUE;
        for (final Object obj : values)
          min = Math.min(min, (Integer) obj);
        if (min != Integer.MAX_VALUE)
          txt = format.format(min);
      }
      else if (propertyType == Type.DOUBLE) {
        double min = Double.MAX_VALUE;
        for (final Object obj : values)
          min = Math.min(min, (Double) obj);
        if (min != Double.MAX_VALUE)
          txt = format.format(min);
      }

      return txt;
    }
  }

  private static class Maximum implements Summary {

    @Override
    public String toString() {
      return FrameworkMessages.get(FrameworkMessages.MAXIMUM);
    }

    public String getSummary(final Collection<?> values, final Type propertyType, final Format format) {
      String txt = "";
      if (propertyType == Type.INT) {
        int max = Integer.MIN_VALUE;
        for (final Object obj : values)
          max = Math.max(max, (Integer) obj);
        if (max != Integer.MIN_VALUE)
          txt = format.format(max);
      }
      else if (propertyType == Type.DOUBLE) {
        double max = Double.MIN_VALUE;
        for (final Object obj : values)
          max = Math.max(max, (Double) obj);
        if (max != Double.MIN_VALUE)
          txt = format.format(max);
      }

      return txt;
    }
  }

  private static class MinimumMaximum implements Summary {

    @Override
    public String toString() {
      return FrameworkMessages.get(FrameworkMessages.MINIMUM_AND_MAXIMUM);
    }

    public String getSummary(final Collection<?> values, final Type propertyType, final Format format) {
      String txt = "";
      if (propertyType == Type.INT) {
        int min = Integer.MAX_VALUE;
        int max = Integer.MIN_VALUE;
        for (final Object obj : values) {
          max = Math.max(max, (Integer) obj);
          min = Math.min(min, (Integer) obj);
        }
        if (max != Integer.MIN_VALUE)
          txt = format.format(min) + "/" + format.format(max);
      }
      else if (propertyType == Type.DOUBLE) {
        double min = Double.MAX_VALUE;
        double max = Double.MIN_VALUE;
        for (final Object obj : values) {
          max = Math.max(max, (Double) obj);
          min = Math.min(min, (Double) obj);
        }
        if (max != Double.MIN_VALUE)
          txt = format.format(min) + "/" + format.format(max);
      }

      return txt;
    }
  }
}
