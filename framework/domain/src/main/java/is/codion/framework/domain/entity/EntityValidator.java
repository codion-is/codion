/*
 * Copyright (c) 2020 - 2024, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.entity;

import is.codion.common.Configuration;
import is.codion.common.property.PropertyValue;
import is.codion.framework.domain.entity.attribute.Attribute;
import is.codion.framework.domain.entity.exception.ValidationException;

/**
 * Responsible for providing validation for entities.
 */
public interface EntityValidator {

  /**
   * Specifies whether the default validator performs strict validation or not.
   * By default all non-read-only attribute values are validated if the entity
   * is being inserted (as in, when it does not exist according to {@link Entity#exists()}).
   * If the entity exists, only modified values are validated.
   * With strict validation enabled all values are validated, regardless
   * of whether the entity exists or not.<br>
   * Value type: Boolean<br>
   * Default value: false
   */
  PropertyValue<Boolean> STRICT_VALIDATION = Configuration.booleanValue("codion.domain.strictValidation", false);

  /**
   * Returns true if the value based on the given attribute accepts a null value for the given entity,
   * by default this method simply returns the nullable state of the underlying attribute.
   * @param entity the entity being validated
   * @param attribute the attribute
   * @param <T> the value type
   * @return true if the attribute accepts a null value
   */
  <T> boolean nullable(Entity entity, Attribute<T> attribute);

  /**
   * Returns true if the given entity contains only valid values.
   * @param entity the entity
   * @return true if the given entity contains only valid values
   */
  boolean valid(Entity entity);

  /**
   * Checks if the values in the given entity are valid.
   * Note that by default, if the entity instance does not exist according to
   * {@link Entity#exists()} all values are validated, otherwise only modified values are validated.
   * Use the {@link #STRICT_VALIDATION} configuration value to change the default behaviour.
   * @param entity the entity
   * @throws ValidationException in case of an invalid value
   * @see #STRICT_VALIDATION
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
