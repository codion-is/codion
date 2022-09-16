/*
 * Copyright (c) 2008 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.model;

import is.codion.common.db.exception.DatabaseException;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.domain.entity.Attribute;
import is.codion.swing.common.model.component.combobox.SwingFilteredComboBoxModel;

import java.util.Collection;
import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;

/**
 * A combo box model based on all the values of a single attribute.
 * For instances use the {@link #swingPropertyComboBoxModel(EntityConnectionProvider, Attribute)} or
 * {@link #swingPropertyComboBoxModel(Supplier)} factory methods.
 * @param <T> the type of values in this combo box model
 * @see #swingPropertyComboBoxModel(EntityConnectionProvider, Attribute)
 * @see #swingPropertyComboBoxModel(Supplier)
 */
public final class SwingPropertyComboBoxModel<T> extends SwingFilteredComboBoxModel<T> {

  private final Supplier<Collection<T>> valueSupplier;

  private SwingPropertyComboBoxModel(Supplier<Collection<T>> valueSupplier) {
    this.valueSupplier = requireNonNull(valueSupplier, "valueSupplier");
  }

  /**
   * @param connectionProvider a EntityConnectionProvider instance
   * @param attribute the underlying attribute
   * @param <T> the type of values in this combo box model
   * @return a new {@link SwingPropertyComboBoxModel} instance
   */
  public static <T> SwingPropertyComboBoxModel<T> swingPropertyComboBoxModel(EntityConnectionProvider connectionProvider, Attribute<T> attribute) {
    return new SwingPropertyComboBoxModel<>(new DefaultValueSupplier<>(requireNonNull(connectionProvider, "connectionProvider"), attribute));
  }

  /**
   * @param valueSupplier provides the values to show in this combo box model
   * @param <T> the type of values in this combo box model
   * @return a new {@link SwingPropertyComboBoxModel} instance
   */
  public static <T> SwingPropertyComboBoxModel<T> swingPropertyComboBoxModel(Supplier<Collection<T>> valueSupplier) {
    return new SwingPropertyComboBoxModel<>(valueSupplier);
  }

  @Override
  protected Collection<T> refreshItems() {
    return valueSupplier.get();
  }

  private static final class DefaultValueSupplier<T> implements Supplier<Collection<T>> {

    private final EntityConnectionProvider connectionProvider;
    private final Attribute<T> attribute;

    private DefaultValueSupplier(EntityConnectionProvider connectionProvider, Attribute<T> attribute) {
      this.connectionProvider = connectionProvider;
      this.attribute = requireNonNull(attribute, "attribute");
    }

    @Override
    public Collection<T> get() {
      try {
        return connectionProvider.connection().select(attribute);
      }
      catch (DatabaseException e) {
        throw new RuntimeException(e);
      }
    }
  }
}
