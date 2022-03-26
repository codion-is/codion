/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.entity;

import is.codion.framework.domain.entity.exception.LengthValidationException;
import is.codion.framework.domain.entity.exception.NullValidationException;
import is.codion.framework.domain.entity.exception.RangeValidationException;
import is.codion.framework.domain.entity.exception.ValidationException;
import is.codion.framework.domain.property.Property;

import java.util.Collection;

/**
 * Responsible for providing validation for entities.
 */
public interface EntityValidator {

  /**
   * Returns true if the property based on the given attribute accepts a null value for the given entity,
   * by default this method simply returns the nullable state of the property.
   * @param entity the entity being validated
   * @param attribute the attribute
   * @param <T> the value type
   * @return true if the attribute accepts a null value
   */
  <T> boolean isNullable(Entity entity, Attribute<T> attribute);

  /**
   * Returns true if the given entity contains only valid values.
   * @param entity the entity
   * @return true if the given entity contains only valid values
   */
  boolean isValid(Entity entity);

  /**
   * Checks if the values in the given entity are valid
   * @param entity the entity
   * @throws ValidationException in case of an invalid value
   */
  void validate(Entity entity) throws ValidationException;

  /**
   * Checks if the value associated with the give attribute is valid, throws a ValidationException if not
   * @param entity the entity to validate
   * @param attribute the attribute the value is associated with
   * @param <T> the value type
   * @throws ValidationException if the given value is not valid for the given attribute
   */
  <T> void validate(Entity entity, Attribute<T> attribute) throws ValidationException;

  /**
   * Validates the given entities, assumes they are all the same type.
   * @param entities the entities to validate
   * @throws ValidationException in case the validation fails
   */
  void validate(Collection<Entity> entities) throws ValidationException;

  /**
   * Performs a null validation on the given attribute
   * @param entity the entity
   * @param attribute the attribute
   * @param <T> the value type
   * @throws NullValidationException in case the attribute value is null and the attribute is not nullable
   * @see Property.Builder#nullable(boolean)
   * @see Property#isNullable()
   */
  <T> void performNullValidation(Entity entity, Attribute<T> attribute) throws NullValidationException;

  /**
   * Performs a range validation on the given number based attribute
   * @param entity the entity
   * @param attribute the attribute
   * @param <T> the value type
   * @throws RangeValidationException in case the value of the given attribute is outside the legal range
   * @see Property.Builder#range(double, double)
   */
  <T extends Number> void performRangeValidation(Entity entity, Attribute<T> attribute) throws RangeValidationException;

  /**
   * Performs a length validation on the given string based attribute
   * @param entity the entity
   * @param attribute the attribute
   * @throws LengthValidationException in case the length of the value of the given attribute
   * @see Property.Builder#maximumLength(int)
   */
  void performLengthValidation(Entity entity, Attribute<String> attribute) throws LengthValidationException;
}
