/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.model;

import org.jminor.common.db.exception.DatabaseException;
import org.jminor.common.model.EventListener;
import org.jminor.common.model.EventObserver;
import org.jminor.common.model.combobox.DefaultFilteredComboBoxModel;
import org.jminor.common.model.valuemap.ValueCollectionProvider;
import org.jminor.framework.db.provider.EntityConnectionProvider;
import org.jminor.framework.domain.Property;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * A combo box model based on a single entity property.
 */
public class DefaultPropertyComboBoxModel extends DefaultFilteredComboBoxModel {

  private final ValueCollectionProvider<Object> valueProvider;

  /**
   * @param entityID the ID of the underlying entity
   * @param connectionProvider a EntityConnectionProvider instance
   * @param property the underlying property
   * @param nullValueString the value to use to represent a null value
   * @param refreshEvent triggers a refresh
   */
  public DefaultPropertyComboBoxModel(final String entityID, final EntityConnectionProvider connectionProvider,
                                      final Property.ColumnProperty property, final String nullValueString,
                                      final EventObserver refreshEvent) {
    this(new ValueCollectionProvider<Object>() {
      /** {@inheritDoc} */
      @Override
      public Collection<Object> getValues() {
        try {
          return connectionProvider.getConnection().selectPropertyValues(entityID, property.getPropertyID(), true);
        }
        catch (DatabaseException e) {
          throw new RuntimeException(e);
        }
      }
    }, nullValueString == null ? (property.isNullable() ? "" : null) : nullValueString, refreshEvent);
  }

  /**
   * @param valueProvider provides the values to show in this combo box model
   * @param nullValueString the value to use to represent a null value
   * @param refreshEvent triggers a refresh
   */
  public DefaultPropertyComboBoxModel(final ValueCollectionProvider<Object> valueProvider, final String nullValueString,
                                      final EventObserver refreshEvent) {
    super(nullValueString);
    this.valueProvider = valueProvider;
    if (refreshEvent != null) {
      refreshEvent.addListener(new EventListener() {
        /** {@inheritDoc} */
        @Override
        public void eventOccurred() {
          refresh();
        }
      });
    }
  }

  /** {@inheritDoc} */
  @Override
  protected final List<?> initializeContents() {
    return new ArrayList<>(valueProvider.getValues());
  }
}
