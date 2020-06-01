/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.model;

import is.codion.common.db.exception.DatabaseException;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.domain.entity.EntityIdentity;
import is.codion.framework.domain.property.ColumnProperty;
import is.codion.swing.common.model.combobox.SwingFilteredComboBoxModel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;

import static is.codion.framework.db.condition.Conditions.condition;

/**
 * A combo box model based on a single entity property.
 * @param <T> the type of values in this combo box model
 */
public class SwingPropertyComboBoxModel<T> extends SwingFilteredComboBoxModel<T> {

  private final Supplier<Collection<T>> valueProvider;

  /**
   * @param entityId the id of the underlying entity
   * @param connectionProvider a EntityConnectionProvider instance
   * @param property the underlying property
   * @param nullValue the value to use to represent a null value
   */
  public SwingPropertyComboBoxModel(final EntityIdentity entityId, final EntityConnectionProvider connectionProvider,
                                    final ColumnProperty<?> property, final T nullValue) {
    this(() -> {
      try {
        return connectionProvider.getConnection().selectValues(property.getAttribute(), condition(entityId));
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
  public SwingPropertyComboBoxModel(final Supplier<Collection<T>> valueProvider, final T nullValue) {
    super(nullValue);
    this.valueProvider = valueProvider;
  }

  @Override
  protected final List<T> initializeContents() {
    return new ArrayList<>(valueProvider.get());
  }
}
