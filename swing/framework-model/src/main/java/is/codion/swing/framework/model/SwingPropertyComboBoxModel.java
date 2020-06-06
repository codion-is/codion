/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.model;

import is.codion.common.db.exception.DatabaseException;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.domain.entity.Attribute;
import is.codion.swing.common.model.combobox.SwingFilteredComboBoxModel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;

/**
 * A combo box model based on a single entity property.
 * @param <T> the type of values in this combo box model
 */
public class SwingPropertyComboBoxModel<T> extends SwingFilteredComboBoxModel<T> {

  private final Supplier<Collection<T>> valueProvider;

  /**
   * @param connectionProvider a EntityConnectionProvider instance
   * @param attribute the underlying attribute
   * @param nullString the String to use to represent a null value
   */
  public SwingPropertyComboBoxModel(final EntityConnectionProvider connectionProvider,
                                    final Attribute<T> attribute, final String nullString) {
    this(new DefaultValueProvider<>(connectionProvider, attribute), nullString);
  }

  /**
   * @param valueProvider provides the values to show in this combo box model
   * @param nullString the String to use to represent a null value
   */
  public SwingPropertyComboBoxModel(final Supplier<Collection<T>> valueProvider, final String nullString) {
    super(nullString);
    this.valueProvider = valueProvider;
  }

  @Override
  protected final List<T> initializeContents() {
    return new ArrayList<>(valueProvider.get());
  }

  private static final class DefaultValueProvider<T> implements Supplier<Collection<T>> {

    private final EntityConnectionProvider connectionProvider;
    private final Attribute<T> attribute;

    private DefaultValueProvider(final EntityConnectionProvider connectionProvider, final Attribute<T> attribute) {
      this.connectionProvider = requireNonNull(connectionProvider, "connectionProvider");
      this.attribute = requireNonNull(attribute, "attribute");
    }

    @Override
    public Collection<T> get() {
      try {
        return connectionProvider.getConnection().selectValues(attribute);
      }
      catch (final DatabaseException e) {
        throw new RuntimeException(e);
      }
    }
  }
}
