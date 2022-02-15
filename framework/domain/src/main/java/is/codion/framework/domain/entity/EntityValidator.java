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
   * Returns true if the given property accepts a null value for the given entity,
   * by default this method simply returns {@code property.isNullable()}
   * @param entity the entity being validated
   * @param property the property
   * @param <T> the value type
   * @return true if the property accepts a null value
   */
  <T> boolean isNullable(Entity entity, Property<T> property);

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
   * @param <T> the value type
   * @throws ValidationException if the given value is not valid for the given property
   */
  <T> void validate(Entity entity, EntityDefinition definition, Property<T> property) throws ValidationException;

  /**
   * Validates the given entities, assumes they are all the same type.
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
   * @param <T> the value type
   * @throws NullValidationException in case the property value is null and the property is not nullable
   * @see Property.Builder#nullable(boolean)
   * @see Property#isNullable()
   */
  <T> void performNullValidation(Entity entity, EntityDefinition definition, Property<T> property) throws NullValidationException;

  /**
   * Performs a range validation on the given number based property
   * @param entity the entity
   * @param property the property
   * @param <T> the value type
   * @throws RangeValidationException in case the value of the given property is outside the legal range
   * @see Property.Builder#range(double, double)
   */
  <T extends Number> void performRangeValidation(Entity entity, Property<T> property) throws RangeValidationException;

  /**
   * Performs a length validation on the given string based property
   * @param entity the entity
   * @param property the property
   * @throws LengthValidationException in case the length of the value of the given property
   * @see Property.Builder#maximumLength(int)
   */
  void performLengthValidation(Entity entity, Property<String> property) throws LengthValidationException;
}
