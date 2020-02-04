/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.model;

import org.jminor.common.value.AbstractValue;
import org.jminor.framework.domain.Entity;

import java.util.List;
import java.util.Objects;

import static java.util.Objects.requireNonNull;

/**
 * Selects an Entity in a combo box model according to a property value.
 * @param <T> the value type
 */
public final class EntityComboBoxModelSelector<T> extends AbstractValue<T> {

  private final EntityComboBoxModel comboBoxModel;
  private final String propertyId;
  private final Finder<T> finder;

  /**
   * @param comboBoxModel the combo box model
   * @param propertyId the property
   */
  public EntityComboBoxModelSelector(final EntityComboBoxModel comboBoxModel, final String propertyId) {
    this(comboBoxModel, propertyId, defaultFinder(propertyId));
  }

  /**
   * @param comboBoxModel the combo box model
   * @param propertyId the property
   * @param finder the Finder instance responsible for finding the entity by value
   */
  public EntityComboBoxModelSelector(final EntityComboBoxModel comboBoxModel, final String propertyId,
                                     final Finder<T> finder) {
    this.comboBoxModel = requireNonNull(comboBoxModel);
    this.propertyId = requireNonNull(propertyId);
    this.finder = requireNonNull(finder);
    this.comboBoxModel.addSelectionListener(selected -> notifyValueChange(get()));
  }

  /**
   * Selects the first entity found in the underlying combo box model, which
   * has the the given value associated with the underlying property.
   * @param value the value
   */
  @Override
  public void set(final T value) {
    comboBoxModel.setSelectedItem(value == null ? null : finder.findEntity(comboBoxModel.getVisibleItems(), value));
  }

  /**
   * @return the value of the underlying property in the selected Entity, null if the selection is empty
   */
  @Override
  public T get() {
    if (comboBoxModel.isSelectionEmpty()) {
      return null;
    }

    return (T) comboBoxModel.getSelectedValue().get(propertyId);
  }

  /**
   * @return true
   */
  @Override
  public boolean isNullable() {
    return true;
  }

  public static <T> Finder<T> defaultFinder(final String propertyId) {
    return new DefaultFinder<>(propertyId);
  }

  public interface Finder<T> {

    /**
     * Returns the first Entity in the given list with {@code value} associated with
     * the underlying property. Only called for non-null {@code value}s.
     * @param entities the entities to search
     * @param value the value to search for, never null
     * @return the first Entity in the given list with the given value.
     */
    Entity findEntity(List<Entity> entities, T value);
  }

  private static final class DefaultFinder<T> implements Finder<T> {

    private final String propertyId;

    private DefaultFinder(final String propertyId) {
      this.propertyId = propertyId;
    }

    @Override
    public Entity findEntity(final List<Entity> entities, final T value) {
      return entities.stream().filter(entity ->
                    Objects.equals(value, entity.get(propertyId))).findFirst().orElse(null);
    }
  }
}
