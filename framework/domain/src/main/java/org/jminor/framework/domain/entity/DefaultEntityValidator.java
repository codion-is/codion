/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.domain.entity;

import org.jminor.common.event.Event;
import org.jminor.common.event.EventListener;
import org.jminor.common.event.Events;
import org.jminor.framework.domain.entity.exception.LengthValidationException;
import org.jminor.framework.domain.entity.exception.NullValidationException;
import org.jminor.framework.domain.entity.exception.RangeValidationException;
import org.jminor.framework.domain.entity.exception.ValidationException;
import org.jminor.framework.domain.property.ColumnProperty;
import org.jminor.framework.domain.property.ForeignKeyProperty;
import org.jminor.framework.domain.property.Property;

import java.util.Collection;
import java.util.Locale;
import java.util.Objects;
import java.util.ResourceBundle;

/**
 * A default {@link Validator} implementation providing null validation for properties marked as not null,
 * range validation for numerical properties with max and/or min values specified and string length validation
 * based on the specified max length.
 * This Validator can be extended to provide further validation.
 * @see Property.Builder#nullable(boolean)
 * @see Property.Builder#minimumValue(double)
 * @see Property.Builder#maximumValue(double)
 * @see Property.Builder#maximumLength(int)
 */
public class DefaultEntityValidator implements Validator {

  private static final long serialVersionUID = 1;

  private static final ResourceBundle MESSAGES = ResourceBundle.getBundle(DefaultEntityValidator.class.getName(), Locale.getDefault());

  private static final String ENTITY_PARAM = "entity";
  private static final String PROPERTY_PARAM = "property";
  private static final String VALUE_REQUIRED_KEY = "property_value_is_required";

  private final boolean performNullValidation;

  private transient Event revalidateEvent;

  /**
   * Instantiates a new {@link Validator}
   */
  public DefaultEntityValidator() {
    this(true);
  }

  /**
   * Instantiates a new {@link Validator}
   * @param performNullValidation if true then automatic null validation is performed
   */
  public DefaultEntityValidator(final boolean performNullValidation) {
    this.performNullValidation = performNullValidation;
  }

  /** {@inheritDoc} */
  @Override
  public boolean isValid(final Entity entity) {
    try {
      validate(entity);
      return true;
    }
    catch (final ValidationException e) {
      return false;
    }
  }

  /**
   * Returns true if the given property accepts a null value for the given entity,
   * by default this method simply returns {@code property.isNullable()}
   * @param entity the entity being validated
   * @param property the property
   * @return true if the property accepts a null value
   */
  @Override
  public boolean isNullable(final Entity entity, final Property property) {
    return property.isNullable();
  }

  /**
   * Validates all writable properties in the given entities
   * @param entities the entities to validate
   * @throws ValidationException in case validation fails
   */
  @Override
  public final void validate(final Collection<Entity> entities) throws ValidationException {
    for (final Entity entity : entities) {
      validate(entity);
    }
  }

  /**
   * Validates all writable properties in the given entity
   * @param entity the entity to validate
   * @throws ValidationException in case validation fails
   */
  @Override
  public void validate(final Entity entity) throws ValidationException {
    Objects.requireNonNull(entity, ENTITY_PARAM);
    for (final Property property : entity.getDefinition().getProperties()) {
      validate(entity, property);
    }
  }

  /** {@inheritDoc} */
  @Override
  public void validate(final Entity entity, final Property property) throws ValidationException {
    Objects.requireNonNull(entity, ENTITY_PARAM);
    Objects.requireNonNull(property, PROPERTY_PARAM);
    if (performNullValidation && !isForeignKeyProperty(property)) {
      performNullValidation(entity, property);
    }
    if (property.isNumerical()) {
      performRangeValidation(entity, property);
    }
    else if (property.isString()) {
      performLengthValidation(entity, property);
    }
  }

  /** {@inheritDoc} */
  @Override
  public final void performRangeValidation(final Entity entity, final Property property) throws RangeValidationException {
    Objects.requireNonNull(entity, ENTITY_PARAM);
    Objects.requireNonNull(property, PROPERTY_PARAM);
    if (entity.isNull(property)) {
      return;
    }

    final Number value = (Number) entity.get(property);
    if (value.doubleValue() < (property.getMinimumValue() == null ? Double.NEGATIVE_INFINITY : property.getMinimumValue())) {
      throw new RangeValidationException(property.getPropertyId(), value, "'" + property + "' " +
              MESSAGES.getString("property_value_too_small") + " " + property.getMinimumValue());
    }
    if (value.doubleValue() > (property.getMaximumValue() == null ? Double.POSITIVE_INFINITY : property.getMaximumValue())) {
      throw new RangeValidationException(property.getPropertyId(), value, "'" + property + "' " +
              MESSAGES.getString("property_value_too_large") + " " + property.getMaximumValue());
    }
  }

  /** {@inheritDoc} */
  @Override
  public final void performNullValidation(final Entity entity, final Property property) throws NullValidationException {
    Objects.requireNonNull(entity, ENTITY_PARAM);
    Objects.requireNonNull(property, PROPERTY_PARAM);
    if (!isNullable(entity, property) && entity.isNull(property)) {
      if ((entity.getKey().isNull() || entity.getOriginalKey().isNull()) && !(property instanceof ForeignKeyProperty)) {
        //a new entity being inserted, allow null for columns with default values and generated primary key values
        final boolean nonKeyColumnPropertyWithoutDefaultValue = isNonKeyColumnPropertyWithoutDefaultValue(property);
        final boolean primaryKeyPropertyWithoutAutoGenerate = isNonGeneratedPrimaryKeyProperty(entity, property);
        if (nonKeyColumnPropertyWithoutDefaultValue || primaryKeyPropertyWithoutAutoGenerate) {
          throw new NullValidationException(property.getPropertyId(), MESSAGES.getString(VALUE_REQUIRED_KEY) + ": " + property);
        }
      }
      else {
        throw new NullValidationException(property.getPropertyId(), MESSAGES.getString(VALUE_REQUIRED_KEY) + ": " + property);
      }
    }
  }

  /** {@inheritDoc} */
  @Override
  public final void performLengthValidation(final Entity entity, final Property property) throws LengthValidationException {
    Objects.requireNonNull(entity, ENTITY_PARAM);
    Objects.requireNonNull(property, PROPERTY_PARAM);
    if (entity.isNull(property)) {
      return;
    }

    final int maxLength = property.getMaximumLength();
    final String value = (String) entity.get(property);
    if (maxLength != -1 && value.length() > maxLength) {
      throw new LengthValidationException(property.getPropertyId(), value, "'" + property + "' " +
              MESSAGES.getString("property_value_too_long") + " " + maxLength);
    }
  }

  /** {@inheritDoc} */
  @Override
  public final void revalidate() {
    getRevalidateEvent().onEvent();
  }

  /** {@inheritDoc} */
  @Override
  public final void addRevalidationListener(final EventListener listener) {
    getRevalidateEvent().addListener(listener);
  }

  /** {@inheritDoc} */
  @Override
  public final void removeRevalidationListener(final EventListener listener) {
    getRevalidateEvent().removeListener(listener);
  }

  private Event getRevalidateEvent() {
    if (revalidateEvent == null) {
      revalidateEvent = Events.event();
    }

    return revalidateEvent;
  }

  private static boolean isNonGeneratedPrimaryKeyProperty(final Entity entity, final Property property) {
    return (property instanceof ColumnProperty
            && ((ColumnProperty) property).isPrimaryKeyProperty()) && !entity.getDefinition().isKeyGenerated();
  }

  /**
   * @param property the property
   * @return true if the property is a part of a foreign key
   */
  private static boolean isForeignKeyProperty(final Property property) {
    return property instanceof ColumnProperty && ((ColumnProperty) property).isForeignKeyProperty();
  }

  private static boolean isNonKeyColumnPropertyWithoutDefaultValue(final Property property) {
    return property instanceof ColumnProperty && !((ColumnProperty) property).isPrimaryKeyProperty()
            && !((ColumnProperty) property).columnHasDefaultValue();
  }
}
