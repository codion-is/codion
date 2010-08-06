/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.model;

import org.jminor.common.model.Event;
import org.jminor.framework.domain.Property;
import org.jminor.framework.i18n.FrameworkMessages;

import java.awt.event.ActionListener;
import java.util.Collection;
import java.util.List;

/**
 * A interface defining a class for providing summaries of numerical table columns: sum, average, minimum, maximum and minimum & maximum.<br>
 */
public interface PropertySummaryModel {

  public enum SummaryType {
    NONE {
      @Override
      public String toString() {
        return FrameworkMessages.get(FrameworkMessages.NONE);
      }
    }, SUM {
      @Override
      public String toString() {
        return FrameworkMessages.get(FrameworkMessages.SUM);
      }
    }, AVERAGE {
      @Override
      public String toString() {
        return FrameworkMessages.get(FrameworkMessages.AVERAGE);
      }
    }, MINIMUM {
      @Override
      public String toString() {
        return FrameworkMessages.get(FrameworkMessages.MINIMUM);
      }
    }, MAXIMUM {
      @Override
      public String toString() {
        return FrameworkMessages.get(FrameworkMessages.MAXIMUM);
      }
    }, MINIMUM_MAXIMUM {
      @Override
      public String toString() {
        return FrameworkMessages.get(FrameworkMessages.MINIMUM_AND_MAXIMUM);
      }
    }
  }

  /**
   * @return the Property this summary model is based on
   */
  Property getProperty();

  /**
   * @param summaryType the type of summary to show
   */
  void setSummaryType(final SummaryType summaryType);

  SummaryType getSummaryType();

  List<SummaryType> getSummaryTypes();

  String getSummaryText();

  void addSummaryListener(final ActionListener listener);

  void removeSummaryListener(final ActionListener listener);

  void addSummaryTypeListener(final ActionListener listener);

  void removeSummaryTypeListener(final ActionListener listener);

  /**
   * Provides the values used when creating the summary value.
   */
  interface PropertyValueProvider {

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
}
