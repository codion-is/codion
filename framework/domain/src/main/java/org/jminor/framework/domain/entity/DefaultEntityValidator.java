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

  private static final ResourceBundle MESSAGES = ResourceBundle.getBundle(DefaultEntityValidator.class.getName());

  private static final String ENTITY_PARAM = "entity";
  private static final String PROPERTY_PARAM = "property";
  private static final String VALUE_REQUIRED_KEY = "property_value_is_required";

  private boolean performNullValidation = true;

  private transient Event revalidateEvent;

  /**
   * @return true if this validator performs null validation
   */
  public boolean isPerformNullValidation() {
    return performNullValidation;
  }

  /**
   * @param performNullValidation true if this validator should perform null validation
   * @return this validator instance
   */
  public DefaultEntityValidator setPerformNullValidation(final boolean performNullValidation) {
    this.performNullValidation = performNullValidation;
    return this;
  }

  @Override
  public boolean isValid(final Entity entity, final EntityDefinition definition) {
    try {
      validate(entity, definition);
      return true;
    }
    catch (final ValidationException e) {
      return false;
    }
  }

  @Override
  public boolean isNullable(final Entity entity, final Property property) {
    return property.isNullable();
  }

  @Override
  public final void validate(final Collection<Entity> entities, final EntityDefinition definition) throws ValidationException {
    for (final Entity entity : entities) {
      validate(entity, definition);
    }
  }

  @Override
  public void validate(final Entity entity, final EntityDefinition definition) throws ValidationException {
    Objects.requireNonNull(entity, ENTITY_PARAM);
    for (final Property property : definition.getProperties()) {
      validate(entity, definition, property);
    }
  }

  @Override
  public void validate(final Entity entity, final EntityDefinition definition, final Property property) throws ValidationException {
    Objects.requireNonNull(entity, ENTITY_PARAM);
    Objects.requireNonNull(property, PROPERTY_PARAM);
    if (performNullValidation && !isForeignKeyProperty(property)) {
      performNullValidation(entity, definition, property);
    }
    if (property.isNumerical()) {
      performRangeValidation(entity, property);
    }
    else if (property.isString()) {
      performLengthValidation(entity, property);
    }
  }

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

  @Override
  public final void performNullValidation(final Entity entity, final EntityDefinition definition,
                                          final Property property) throws NullValidationException {
    Objects.requireNonNull(entity, ENTITY_PARAM);
    Objects.requireNonNull(property, PROPERTY_PARAM);
    if (!isNullable(entity, property) && entity.isNull(property)) {
      if ((entity.getKey().isNull() || entity.getOriginalKey().isNull()) && !(property instanceof ForeignKeyProperty)) {
        //a new entity being inserted, allow null for columns with default values and generated primary key values
        final boolean nonKeyColumnPropertyWithoutDefaultValue = isNonKeyColumnPropertyWithoutDefaultValue(property);
        final boolean primaryKeyPropertyWithoutAutoGenerate = isNonGeneratedPrimaryKeyProperty(definition, property);
        if (nonKeyColumnPropertyWithoutDefaultValue || primaryKeyPropertyWithoutAutoGenerate) {
          throw new NullValidationException(property.getPropertyId(), MESSAGES.getString(VALUE_REQUIRED_KEY) + ": " + property);
        }
      }
      else {
        throw new NullValidationException(property.getPropertyId(), MESSAGES.getString(VALUE_REQUIRED_KEY) + ": " + property);
      }
    }
  }

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

  @Override
  public final void revalidate() {
    getRevalidateEvent().onEvent();
  }

  @Override
  public final void addRevalidationListener(final EventListener listener) {
    getRevalidateEvent().addListener(listener);
  }

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

  private static boolean isNonGeneratedPrimaryKeyProperty(final EntityDefinition definition, final Property property) {
    return (property instanceof ColumnProperty
            && ((ColumnProperty) property).isPrimaryKeyProperty()) && !definition.isKeyGenerated();
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
