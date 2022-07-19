/*
 * Copyright (c) 2020 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.entity;

import is.codion.framework.domain.entity.exception.ValidationException;

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
}
