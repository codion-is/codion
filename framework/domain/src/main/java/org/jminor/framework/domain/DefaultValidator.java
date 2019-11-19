/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.domain;

import org.jminor.common.db.valuemap.DefaultValueMap;
import org.jminor.common.db.valuemap.exception.LengthValidationException;
import org.jminor.common.db.valuemap.exception.NullValidationException;
import org.jminor.common.db.valuemap.exception.RangeValidationException;
import org.jminor.common.db.valuemap.exception.ValidationException;
import org.jminor.framework.domain.property.ColumnProperty;
import org.jminor.framework.domain.property.ForeignKeyProperty;
import org.jminor.framework.domain.property.Property;

import java.util.Collection;
import java.util.Locale;
import java.util.Objects;
import java.util.ResourceBundle;

/**
 * A default {@link Entity.Validator} implementation providing null validation for properties marked as not null,
 * range validation for numerical properties with max and/or min values specified and string length validation
 * based on the specified max length.
 * This Validator can be extended to provide further validation.
 * @see Property.Builder#setNullable(boolean)
 * @see Property.Builder#setMin(double)
 * @see Property.Builder#setMax(double)
 * @see Property.Builder#setMaxLength(int)
 */
public class DefaultValidator extends DefaultValueMap.DefaultValidator<Property, Entity> implements Entity.Validator {

  private static final long serialVersionUID = 1;

  private static final ResourceBundle MESSAGES = ResourceBundle.getBundle(DefaultValidator.class.getName(), Locale.getDefault());

  private static final String ENTITY_PARAM = "entity";
  private static final String PROPERTY_PARAM = "property";
  private static final String VALUE_REQUIRED_KEY = "property_value_is_required";

  private final boolean performNullValidation;

  /**
   * Instantiates a new {@link Entity.Validator}
   */
  public DefaultValidator() {
    this(true);
  }

  /**
   * Instantiates a new {@link Entity.Validator}
   * @param performNullValidation if true then automatic null validation is performed
   */
  public DefaultValidator(final boolean performNullValidation) {
    this.performNullValidation = performNullValidation;
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
    for (final Property property : entity.getProperties()) {
      if (!property.isReadOnly()) {
        validate(entity, property);
      }
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
    if (value.doubleValue() < (property.getMin() == null ? Double.NEGATIVE_INFINITY : property.getMin())) {
      throw new RangeValidationException(property.getPropertyId(), value, "'" + property + "' " +
              MESSAGES.getString("property_value_too_small") + " " + property.getMin());
    }
    if (value.doubleValue() > (property.getMax() == null ? Double.POSITIVE_INFINITY : property.getMax())) {
      throw new RangeValidationException(property.getPropertyId(), value, "'" + property + "' " +
              MESSAGES.getString("property_value_too_large") + " " + property.getMax());
    }
  }

  /** {@inheritDoc} */
  @Override
  public final void performNullValidation(final Entity entity, final Property property) throws NullValidationException {
    Objects.requireNonNull(entity, ENTITY_PARAM);
    Objects.requireNonNull(property, PROPERTY_PARAM);
    if (!isNullable(entity, property) && entity.isNull(property)) {
      if ((entity.getKey().isNull() || entity.getOriginalKey().isNull()) && !(property instanceof ForeignKeyProperty)) {
        //a new entity being inserted, allow null for columns with default values and auto generated primary key values
        final boolean nonKeyColumnPropertyWithoutDefaultValue = isNonKeyColumnPropertyWithoutDefaultValue(property);
        final boolean primaryKeyPropertyWithoutAutoGenerate = isPrimaryKeyPropertyWithoutAutoGenerate(entity, property);
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
  public void performLengthValidation(final Entity entity, final Property property) throws LengthValidationException {
    Objects.requireNonNull(entity, ENTITY_PARAM);
    Objects.requireNonNull(property, PROPERTY_PARAM);
    if (entity.isNull(property)) {
      return;
    }

    final int maxLength = property.getMaxLength();
    final String value = (String) entity.get(property);
    if (maxLength != -1 && value.length() > maxLength) {
      throw new LengthValidationException(property.getPropertyId(), value, "'" + property + "' " +
              MESSAGES.getString("property_value_too_long") + " " + maxLength);
    }
  }

  private static boolean isPrimaryKeyPropertyWithoutAutoGenerate(final Entity entity, final Property property) {
    return (property instanceof ColumnProperty
            && ((ColumnProperty) property).isPrimaryKeyProperty()) && entity.getKeyGeneratorType().isManual();
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
