/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.domain;

import org.jminor.common.model.valuemap.ValueMapValidator;
import org.jminor.common.model.valuemap.exception.NullValidationException;
import org.jminor.common.model.valuemap.exception.RangeValidationException;
import org.jminor.common.model.valuemap.exception.ValidationException;

import java.util.Collection;

/**
 * User: Björn Darri
 * Date: 26.7.2010
 * Time: 21:16:29
 */
public interface EntityValidator extends ValueMapValidator<String, Object> {

  String getEntityID();

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

  void validate(final Entity entity, final int action) throws ValidationException;

  void performNullValidation(final Entity entity, final Property property, final int action) throws NullValidationException;

  void performRangeValidation(final Entity entity, final Property property) throws RangeValidationException;
}
