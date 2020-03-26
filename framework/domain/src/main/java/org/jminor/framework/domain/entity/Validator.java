/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.domain.entity;

import org.jminor.common.event.EventListener;
import org.jminor.framework.domain.entity.exception.LengthValidationException;
import org.jminor.framework.domain.entity.exception.NullValidationException;
import org.jminor.framework.domain.entity.exception.RangeValidationException;
import org.jminor.framework.domain.entity.exception.ValidationException;
import org.jminor.framework.domain.property.Property;

import java.io.Serializable;
import java.util.Collection;

/**
 * Responsible for providing validation for entities.
 */
public interface Validator extends Serializable {

  /**
   * Returns true if the given property accepts a null value for the given entity,
   * by default this method simply returns {@code property.isNullable()}
   * @param entity the entity being validated
   * @param property the property
   * @return true if the property accepts a null value
   */
  boolean isNullable(Entity entity, Property property);

  /**
   * Returns true if the given entity contains only valid values.
   * @param entity the entity
   * @param definition the definition of the entity to validate
   * @return true if the given entity contains only valid values
   */
  boolean isValid(Entity entity, EntityDefinition definition);

  /**
   * Checks if the values in the given entity are valid
   * @param entity the entity
   * @param definition the definition of the entity to validate
   * @throws ValidationException in case of an invalid value
   */
  void validate(Entity entity, EntityDefinition definition) throws ValidationException;

  /**
   * Checks if the value associated with the give property is valid, throws a ValidationException if not
   * @param entity the entity to validate
   * @param definition the definition of the entity to validate
   * @param property the property the value is associated with
   * @throws ValidationException if the given value is not valid for the given property
   */
  void validate(Entity entity, EntityDefinition definition, Property property) throws ValidationException;

  /**
   * Validates the given entities, assumes they are all of the same type.
   * @param entities the entities to validate
   * @param definition the definition of the entity to validate
   * @throws ValidationException in case the validation fails
   */
  void validate(Collection<Entity> entities, EntityDefinition definition) throws ValidationException;

  /**
   * Performs a null validation on the given property
   * @param entity the entity
   * @param definition the definition of the entity to validate
   * @param property the property
   * @throws NullValidationException in case the property value is null and the property is not nullable
   * @see Property.Builder#nullable(boolean)
   * @see Property#isNullable()
   */
  void performNullValidation(Entity entity, EntityDefinition definition, Property property) throws NullValidationException;

  /**
   * Performs a range validation on the given number based property
   * @param entity the entity
   * @param property the property
   * @throws RangeValidationException in case the value of the given property is outside the legal range
   * @see Property.Builder#maximumValue(double)
   * @see Property.Builder#minimumValue(double)
   */
  void performRangeValidation(Entity entity, Property property) throws RangeValidationException;

  /**
   * Performs a length validation on the given string based property
   * @param entity the entity
   * @param property the property
   * @throws LengthValidationException in case the length of the value of the given property
   * @see Property.Builder#maximumLength(int)
   */
  void performLengthValidation(Entity entity, Property property) throws LengthValidationException;

  /**
   * Notifies all re-validation listeners that a re-validation is called for, for example
   * due to modified validation settings
   * @see #addRevalidationListener(EventListener)
   */
  void revalidate();

  /**
   * @param listener a listener notified each time a re-validation of all values is required, for example
   * when the underlying validation settings have changed
   */
  void addRevalidationListener(EventListener listener);

  /**
   * @param listener a listener to remove
   */
  void removeRevalidationListener(EventListener listener);
}
