/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.model;

import org.jminor.common.model.Event;
import org.jminor.framework.domain.Property;
import org.jminor.framework.i18n.FrameworkMessages;

import java.text.Format;
import java.util.Collection;
import java.util.List;

/**
 * A class for providing summaries of numerical table columns: sum, average, minimum, maximum and minimum & maximum.<br>
 * User: Bjorn Darri<br>
 * Date: 5.9.2009<br>
 * Time: 21:47:06<br>
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

  Format initializeFormat(final Property property);

  String getSummaryText();

  Event eventSummaryChanged();

  Event eventSummaryTypeChanged();

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
