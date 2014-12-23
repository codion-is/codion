/*
 * Copyright (c) 2004 - 2015, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.model;

import org.jminor.common.model.Event;
import org.jminor.common.model.EventInfoListener;
import org.jminor.common.model.EventListener;
import org.jminor.framework.domain.Property;

import java.util.Collection;
import java.util.List;

/**
 * A interface defining a class for providing summaries of numerical table columns: sum, average, minimum, maximum and minimum & maximum.<br>
 */
public interface PropertySummaryModel {

  /**
   * Specifies a summary provider
   */
  interface Summary {

    /**
     * Returns a String containing the summary information for the given property
     * @param valueProvider the object responsible for providing the values for the summary
     * @param property the property on which values to base the summary
     * @return a summary text
     */
    String getSummary(final PropertyValueProvider valueProvider, final Property property);
  }

  /**
   * @return the Property this summary model is based on
   */
  Property getProperty();

  /**
   * @return the value provider
   */
  PropertyValueProvider getValueProvider();

  /**
   * @param summary the type of summary to show
   */
  void setCurrentSummary(final Summary summary);

  /**
   * @return the current summary type
   */
  Summary getCurrentSummary();

  /**
   * @return true if changing the summary type is disabled
   */
  boolean isLocked();

  /**
   * @param value if true then changing summary type is disable
   */
  void setLocked(final boolean value);

  /**
   * @return a list containing the available summaries
   */
  List<? extends Summary> getAvailableSummaries();

  /**
   * @return a string representing the summary value
   */
  String getSummaryText();

  /**
   * @param listener a listener to be notified each time the summary value changes
   */
  void addSummaryValueListener(final EventListener listener);

  /**
   * @param listener the listener to remove
   */
  void removeSummaryValueListener(final EventListener listener);

  /**
   * @param listener a listener to be notified each time the summary type changes
   */
  void addSummaryListener(final EventInfoListener<Summary> listener);

  /**
   * @param listener the listener to remove
   */
  void removeSummaryListener(final EventInfoListener<Summary> listener);

  /**
   * Provides the values used when creating the summary value.
   */
  interface PropertyValueProvider {

    /**
     * @return the values to base the summary on
     */
    Collection getValues();

    /**
     * @return true if the values provided by <code>getValues()</code> is a subset of the total available values
     */
    boolean isValueSubset();

    /**
     * @return true if this value provider returns a subset of the data if available
     */
    boolean isUseValueSubset();

    /**
     * @param value true if this value provider should return a subset of the data if available
     */
    void setUseValueSubset(final boolean value);

    /**
     * @param event the event to use when notifying changes to the underlying data which require a summary refresh
     */
    void bindValuesChangedEvent(final Event event);
  }
}
