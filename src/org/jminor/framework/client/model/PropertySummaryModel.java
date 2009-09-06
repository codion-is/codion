package org.jminor.framework.client.model;

import org.jminor.common.model.Event;
import org.jminor.framework.domain.Type;
import org.jminor.framework.i18n.FrameworkMessages;

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

  public static final ISummary NONE = new None();
  public static final ISummary SUM = new Sum();
  public static final ISummary AVERAGE = new Average();
  public static final ISummary MINIMUM = new Minimum();
  public static final ISummary MAXIMUM = new Maximum();
  public static final ISummary MINIMUM_MAXIMUM = new MinimumMaximum();

  public final Event evtSummaryTypeChanged = new Event();
  public final Event evtSummaryChanged = new Event();

  private final IPropertyValueProvider valueProvider;
  private final NumberFormat numberFormat = NumberFormat.getInstance();

  private ISummary summaryType = NONE;

  public PropertySummaryModel(final IPropertyValueProvider valueProvider) {
    this(valueProvider, 4);
  }

  public PropertySummaryModel(final IPropertyValueProvider valueProvider, final int maximumFractionDigits) {
    this.valueProvider = valueProvider;
    this.numberFormat.setMaximumFractionDigits(maximumFractionDigits);
    this.valueProvider.bindValuesChangedEvent(evtSummaryChanged);
    evtSummaryTypeChanged.addListener(evtSummaryChanged);
  }

  /**
   * @param summaryType the type of summary to show
   */
  public void setSummaryType(final ISummary summaryType) {
    if (summaryType == null)
      throw new IllegalArgumentException("Use PropertySummaryModel.NONE instead of null");
    if (!this.summaryType.equals(summaryType)) {
      this.summaryType = summaryType;
      evtSummaryTypeChanged.fire();
    }
  }

  public ISummary getSummaryType() {
    return summaryType;
  }

  public List<ISummary> getSummaryTypes() {
    if (valueProvider.getValueType() == Type.INT || valueProvider.getValueType() == Type.DOUBLE)
      return Arrays.asList(NONE, SUM, AVERAGE, MINIMUM, MAXIMUM, MINIMUM_MAXIMUM);

    return new ArrayList<ISummary>();
  }

  public String getSummaryText() {
    final String summaryTxt = summaryType.getSummary(valueProvider.getValues(), valueProvider.getValueType(), numberFormat);
    return summaryTxt.length() > 0 ? summaryTxt + (valueProvider.isValueSubset() ? "*" : "") : summaryTxt;
  }

  public interface IPropertyValueProvider {
    public Collection<Object> getValues();
    public boolean isValueSubset();
    public Type getValueType();
    public void bindValuesChangedEvent(final Event event);
  }

  public interface ISummary {
    public String getSummary(final Collection<Object> values, final Type propertyType, final NumberFormat format);
  }

  private static class None implements ISummary {

    @Override
    public String toString() {
      return FrameworkMessages.get(FrameworkMessages.NONE);
    }

    public String getSummary(final Collection<Object> values, final Type propertyType, final NumberFormat format) {
      return "";
    }
  }

  private static class Sum implements ISummary {

    @Override
    public String toString() {
      return FrameworkMessages.get(FrameworkMessages.SUM);
    }

    public String getSummary(final Collection<Object> values, final Type propertyType, final NumberFormat format) {
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

  private static class Average implements ISummary {

    @Override
    public String toString() {
      return FrameworkMessages.get(FrameworkMessages.AVERAGE);
    }

    public String getSummary(final Collection<Object> values, final Type propertyType, final NumberFormat format) {
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

  private static class Minimum implements ISummary {

    @Override
    public String toString() {
      return FrameworkMessages.get(FrameworkMessages.MINIMUM);
    }

    public String getSummary(final Collection<Object> values, final Type propertyType, final NumberFormat format) {
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

  private static class Maximum implements ISummary {

    @Override
    public String toString() {
      return FrameworkMessages.get(FrameworkMessages.MAXIMUM);
    }

    public String getSummary(final Collection<Object> values, final Type propertyType, final NumberFormat format) {
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

  private static class MinimumMaximum implements ISummary {

    @Override
    public String toString() {
      return FrameworkMessages.get(FrameworkMessages.MINIMUM_AND_MAXIMUM);
    }

    public String getSummary(final Collection<Object> values, final Type propertyType, final NumberFormat format) {
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
