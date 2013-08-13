/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.model;

import org.jminor.common.model.Event;
import org.jminor.common.model.EventListener;
import org.jminor.common.model.Events;
import org.jminor.common.model.Util;
import org.jminor.framework.domain.Property;
import org.jminor.framework.i18n.FrameworkMessages;

import java.text.Format;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * A default PropertySummaryModel implementation.
 */
public class DefaultPropertySummaryModel implements PropertySummaryModel {

  private final Event summaryChangedEvent = Events.event();
  private final Event summaryValueChangedEvent = Events.event();

  private final Property property;
  private final PropertyValueProvider valueProvider;

  private Summary summary = SummaryType.NONE;
  private boolean locked = false;

  /**
   * Instantiates a new DefaultPropertySummaryModel
   * @param property the property to summarize
   * @param valueProvider the property value provider
   */
  public DefaultPropertySummaryModel(final Property property, final PropertyValueProvider valueProvider) {
    this.property = property;
    this.valueProvider = valueProvider;
    this.valueProvider.bindValuesChangedEvent(summaryValueChangedEvent);
    this.summaryChangedEvent.addListener(summaryValueChangedEvent);
  }

  /**
   * @return the Property this summary model is based on
   */
  @Override
  public final Property getProperty() {
    return property;
  }

  /** {@inheritDoc} */
  @Override
  public boolean isLocked() {
    return locked;
  }

  /** {@inheritDoc} */
  @Override
  public void setLocked(final boolean value) {
    this.locked = value;
  }

  /** {@inheritDoc} */
  @Override
  public PropertyValueProvider getValueProvider() {
    return valueProvider;
  }

  /** {@inheritDoc} */
  @Override
  public final void setCurrentSummary(final Summary summary) {
    if (isLocked()) {
      throw new IllegalStateException("Summary model is locked");
    }
    Util.rejectNullValue(summary, "summary");
    if (!this.summary.equals(summary)) {
      this.summary = summary;
      summaryChangedEvent.fire();
    }
  }

  /** {@inheritDoc} */
  @Override
  public final Summary getCurrentSummary() {
    return summary;
  }

  /** {@inheritDoc} */
  @Override
  public final List<? extends Summary> getAvailableSummaries() {
    if (property.isNumerical()) {
      return Arrays.asList(SummaryType.NONE, SummaryType.SUM, SummaryType.AVERAGE, SummaryType.MINIMUM, SummaryType.MAXIMUM, SummaryType.MINIMUM_MAXIMUM);
    }

    return Collections.emptyList();
  }

  /** {@inheritDoc} */
  @Override
  public final String getSummaryText() {
    return summary.getSummary(valueProvider, property);
  }

  /** {@inheritDoc} */
  @Override
  public final void addSummaryValueListener(final EventListener listener) {
    summaryValueChangedEvent.addListener(listener);
  }

  /** {@inheritDoc} */
  @Override
  public final void removeSummaryValueListener(final EventListener listener) {
    summaryValueChangedEvent.removeListener(listener);
  }

  /** {@inheritDoc} */
  @Override
  public final void addSummaryListener(final EventListener listener) {
    summaryChangedEvent.addListener(listener);
  }

  /** {@inheritDoc} */
  @Override
  public final void removeSummaryListener(final EventListener listener) {
    summaryChangedEvent.removeListener(listener);
  }

  /**
   * The summary types available in this default summary model
   */
  public enum SummaryType implements Summary {
    NONE {
      /** {@inheritDoc} */
      @Override
      public String toString() {
        return FrameworkMessages.get(FrameworkMessages.NONE);
      }

      /** {@inheritDoc} */
      @Override
      public String getSummary(final PropertyValueProvider valueProvider, final Property property) {
        return "";
      }
    }, SUM {
      /** {@inheritDoc} */
      @Override
      public String toString() {
        return FrameworkMessages.get(FrameworkMessages.SUM);
      }

      /** {@inheritDoc} */
      @Override
      public String getSummary(final PropertyValueProvider valueProvider, final Property property) {
        final Format format = property.getFormat();
        String txt = "";
        final Collection values = valueProvider.getValues();
        if (!values.isEmpty()) {
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
        }

        return addSubsetIndicator(txt, valueProvider);
      }
    }, AVERAGE {
      /** {@inheritDoc} */
      @Override
      public String toString() {
        return FrameworkMessages.get(FrameworkMessages.AVERAGE);
      }

      /** {@inheritDoc} */
      @Override
      public String getSummary(final PropertyValueProvider valueProvider, final Property property) {
        final Format format = property.getFormat();
        String txt = "";
        final Collection values = valueProvider.getValues();
        if (!values.isEmpty()) {
          if (property.isInteger()) {
            double sum = 0;
            int count = 0;
            for (final Object obj : values) {
              sum += (Integer)obj;
              count++;
            }
            txt = format.format(sum / count);
          }
          else if (property.isDouble()) {
            double sum = 0;
            int count = 0;
            for (final Object obj : values) {
              sum += (Double)obj;
              count++;
            }
            txt = format.format(sum / count);
          }
        }

        return addSubsetIndicator(txt, valueProvider);
      }
    }, MINIMUM {
      /** {@inheritDoc} */
      @Override
      public String toString() {
        return FrameworkMessages.get(FrameworkMessages.MINIMUM);
      }

      /** {@inheritDoc} */
      @Override
      public String getSummary(final PropertyValueProvider valueProvider, final Property property) {
        final Format format = property.getFormat();
        String txt = "";
        final Collection values = valueProvider.getValues();
        if (!values.isEmpty()) {
          if (property.isInteger()) {
            int min = Integer.MAX_VALUE;
            for (final Object obj : values) {
              min = Math.min(min, (Integer) obj);
            }
            txt = format.format(min);
          }
          else if (property.isDouble()) {
            double min = Double.MAX_VALUE;
            for (final Object obj : values) {
              min = Math.min(min, (Double) obj);
            }
            txt = format.format(min);
          }
        }
        return addSubsetIndicator(txt, valueProvider);
      }
    }, MAXIMUM {
      /** {@inheritDoc} */
      @Override
      public String toString() {
        return FrameworkMessages.get(FrameworkMessages.MAXIMUM);
      }

      /** {@inheritDoc} */
      @Override
      public String getSummary(final PropertyValueProvider valueProvider, final Property property) {
        final Format format = property.getFormat();
        String txt = "";
        final Collection values = valueProvider.getValues();
        if (!values.isEmpty()) {
          if (property.isInteger()) {
            int max = 0;
            for (final Object obj : values) {
              max = Math.max(max, (Integer) obj);
            }
            txt = format.format(max);
          }
          else if (property.isDouble()) {
            double max = 0;
            for (final Object obj : values) {
              max = Math.max(max, (Double) obj);
            }
            txt = format.format(max);
          }
        }

        return addSubsetIndicator(txt, valueProvider);
      }
    }, MINIMUM_MAXIMUM {
      /** {@inheritDoc} */
      @Override
      public String toString() {
        return FrameworkMessages.get(FrameworkMessages.MINIMUM_AND_MAXIMUM);
      }

      /** {@inheritDoc} */
      @Override
      public String getSummary(final PropertyValueProvider valueProvider, final Property property) {
        final Format format = property.getFormat();
        String txt = "";
        final Collection values = valueProvider.getValues();
        if (!values.isEmpty()) {
          if (property.isInteger()) {
            int min = Integer.MAX_VALUE;
            int max = Integer.MIN_VALUE;
            for (final Object obj : values) {
              max = Math.max(max, (Integer) obj);
              min = Math.min(min, (Integer) obj);
            }
            txt = format.format(min) + "/" + format.format(max);
          }
          else if (property.isDouble()) {
            double min = Double.MAX_VALUE;
            double max = Double.MIN_VALUE;
            for (final Object obj : values) {
              max = Math.max(max, (Double) obj);
              min = Math.min(min, (Double) obj);
            }
            txt = format.format(min) + "/" + format.format(max);
          }
        }

        return addSubsetIndicator(txt, valueProvider);
      }
    };

    protected String addSubsetIndicator(final String txt, final PropertyValueProvider valueProvider) {
      if (valueProvider.isUseValueSubset()) {
        return txt.length() != 0 ? txt + (valueProvider.isValueSubset() ? "*" : "") : txt;
      }

      return txt;
    }
  }
}
