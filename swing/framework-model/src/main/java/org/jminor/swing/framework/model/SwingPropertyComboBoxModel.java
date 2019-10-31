/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.framework.model;

import org.jminor.common.db.exception.DatabaseException;
import org.jminor.common.db.valuemap.ValueCollectionProvider;
import org.jminor.framework.db.EntityConnectionProvider;
import org.jminor.framework.db.condition.Conditions;
import org.jminor.framework.domain.Property;
import org.jminor.framework.model.PropertyComboBoxModel;
import org.jminor.swing.common.model.combobox.SwingFilteredComboBoxModel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * A combo box model based on a single entity property.
 * @param <T> the type of values in this combo box model
 */
public class SwingPropertyComboBoxModel<T> extends SwingFilteredComboBoxModel<T> implements PropertyComboBoxModel {

  private final ValueCollectionProvider<T> valueProvider;

  /**
   * @param entityId the ID of the underlying entity
   * @param connectionProvider a EntityConnectionProvider instance
   * @param property the underlying property
   * @param nullValue the value to use to represent a null value
   */
  public SwingPropertyComboBoxModel(final String entityId, final EntityConnectionProvider connectionProvider,
                                    final Property.ColumnProperty property, final T nullValue) {
    this(() -> {
      try {
        return (Collection<T>) connectionProvider.getConnection().selectValues(property.getPropertyId(),
                Conditions.entityCondition(entityId));
      }
      catch (final DatabaseException e) {
        throw new RuntimeException(e);
      }
    }, nullValue);
  }

  /**
   * @param valueProvider provides the values to show in this combo box model
   * @param nullValue the value to use to represent a null value
   */
  public SwingPropertyComboBoxModel(final ValueCollectionProvider<T> valueProvider, final T nullValue) {
    super(nullValue);
    this.valueProvider = valueProvider;
  }

  /** {@inheritDoc} */
  @Override
  protected final List<T> initializeContents() {
    return new ArrayList<>(valueProvider.values());
  }
}
