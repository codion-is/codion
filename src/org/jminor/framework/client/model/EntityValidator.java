/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.model;

import org.jminor.common.model.valuemap.ValueMapValidator;
import org.jminor.common.model.valuemap.exception.NullValidationException;
import org.jminor.common.model.valuemap.exception.RangeValidationException;
import org.jminor.common.model.valuemap.exception.ValidationException;
import org.jminor.framework.db.provider.EntityDbProvider;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.Property;

import java.util.Collection;

/**
 * Responsible for providing validation for entities.
 */
public interface EntityValidator extends ValueMapValidator<String, Object> {

  /**
   * @return the ID of the entity this validator validates
   */
  String getEntityID();

  /**
   * @return the db provider associated with this validator, null if none is available
   */
  EntityDbProvider getDbProvider();

  /**
   * Validates the values in the given entity
   * @param entity the entity to validate
   * @param action the action requiring validation
   * @throws ValidationException in case the validation fails
   * @see org.jminor.framework.domain.Property#setNullable(boolean)
   * @see org.jminor.framework.Configuration#PERFORM_NULL_VALIDATION
   */
  void validate(final Entity entity, final int action) throws ValidationException;

  /**
   * Validates the given property in the given entity
   * @param entity the entity to validate
   * @param propertyID the ID of the property to validate
   * @param action the action requiring validation
   * @throws ValidationException in case the validation fails
   * @see org.jminor.framework.domain.Property#setNullable(boolean)
   * @see org.jminor.framework.Configuration#PERFORM_NULL_VALIDATION
   */
  void validate(final Entity entity, final String propertyID, final int action) throws ValidationException;

  /**
   * Validates the given Entity objects.
   * @param entities the entities to validate
   * @param action describes the action requiring validation,
   * EntityEditor.INSERT, EntityEditor.UPDATE or EntityEditor.UNKNOWN
   * @throws ValidationException in case the validation fails
   */
  void validate(final Collection<Entity> entities, final int action) throws ValidationException;

  /**
   * Performs a null validation on the given property
   * @param entity the entity
   * @param property the property
   * @param action the action requiring validation
   * @throws NullValidationException in case the proerty value is null and the property is not nullable
   * @see org.jminor.framework.domain.Property#isNullable()
   */
  void performNullValidation(final Entity entity, final Property property, final int action) throws NullValidationException;

  /**
   * Performs a range validation on the given property
   * @param entity the entity
   * @param property the property
   * @throws RangeValidationException in case the value of the given property is outside the legal range
   * @see Property#setMax(double)
   * @see Property#setMin(double)
   */
  void performRangeValidation(final Entity entity, final Property property) throws RangeValidationException;
}
