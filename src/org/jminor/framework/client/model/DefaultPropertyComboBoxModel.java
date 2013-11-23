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
public class DefaultPropertyComboBoxModel<T> extends DefaultFilteredComboBoxModel<T> {

  private final ValueCollectionProvider<T> valueProvider;

  /**
   * @param entityID the ID of the underlying entity
   * @param connectionProvider a EntityConnectionProvider instance
   * @param property the underlying property
   * @param nullValue the value to use to represent a null value
   * @param refreshObserver triggers a refresh
   */
  public DefaultPropertyComboBoxModel(final String entityID, final EntityConnectionProvider connectionProvider,
                                      final Property.ColumnProperty property, final T nullValue,
                                      final EventObserver refreshObserver) {
    this(new ValueCollectionProvider<T>() {
      @Override
      public Collection<T> getValues() {
        try {
          return (Collection<T>) connectionProvider.getConnection().selectPropertyValues(entityID, property.getPropertyID(), true);
        }
        catch (DatabaseException e) {
          throw new RuntimeException(e);
        }
      }
    }, nullValue, refreshObserver);
  }

  /**
   * @param valueProvider provides the values to show in this combo box model
   * @param nullValue the value to use to represent a null value
   * @param refreshObserver triggers a refresh
   */
  public DefaultPropertyComboBoxModel(final ValueCollectionProvider<T> valueProvider, final T nullValue,
                                      final EventObserver refreshObserver) {
    super(nullValue);
    this.valueProvider = valueProvider;
    if (refreshObserver != null) {
      refreshObserver.addListener(new EventListener() {
        @Override
        public void eventOccurred() {
          refresh();
        }
      });
    }
  }

  /** {@inheritDoc} */
  @Override
  protected final List<T> initializeContents() {
    return new ArrayList<>(valueProvider.getValues());
  }
}
