/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.model;

import org.jminor.common.model.Util;
import org.jminor.common.model.valuemap.DefaultValueMapValidator;
import org.jminor.common.model.valuemap.ValueMap;
import org.jminor.common.model.valuemap.exception.NullValidationException;
import org.jminor.common.model.valuemap.exception.RangeValidationException;
import org.jminor.common.model.valuemap.exception.ValidationException;
import org.jminor.framework.Configuration;
import org.jminor.framework.db.provider.EntityDbProvider;
import org.jminor.framework.domain.Entities;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.Property;
import org.jminor.framework.i18n.FrameworkMessages;

import java.util.Collection;

/**
 * User: Björn Darri
 * Date: 26.7.2010
 * Time: 22:40:55
 */
public class DefaultEntityValidator extends DefaultValueMapValidator<String, Object> implements EntityValidator {

  private final String entityID;
  private final EntityDbProvider dbProvider;

  /**
   * Instantiates a new DefaultEntityValidator
   * @param entityID the ID of the entities to validate
   */
  public DefaultEntityValidator(final String entityID) {
    this(entityID, null);
  }

  /**
   * Instantiates a new DefaultEntityValidator
   * @param entityID the ID of the entities to validate
   * @param dbProvider the dbProvider in case the validation requires db access, null if not
   */
  public DefaultEntityValidator(final String entityID, final EntityDbProvider dbProvider) {
    Util.rejectNullValue(entityID, "entityID");
    this.entityID = entityID;
    this.dbProvider = dbProvider;
  }

  public final String getEntityID() {
    return entityID;
  }

  public final EntityDbProvider getDbProvider() {
    return dbProvider;
  }

  /**
   * Returns true if the given property accepts a null value for the given entity,
   * by default this method simply returns <code>property.isNullable()</code>
   * @param valueMap the entity being validated
   * @param key the property ID
   * @return true if the property accepts a null value
   */
  @Override
  public boolean isNullable(final ValueMap<String,Object> valueMap, final String key) {
    return Entities.getProperty(entityID, key).isNullable();
  }

  public void validate(final Entity entity, final int action) throws ValidationException {
    validate((ValueMap<String, Object>) entity, action);
  }

  public final void validate(final Collection<Entity> entities, final int action) throws ValidationException {
    for (final Entity entity : entities) {
      validate(entity, action);
    }
  }

  @Override
  public final void validate(final ValueMap<String, Object> valueMap, final int action) throws ValidationException {
    Util.rejectNullValue(valueMap, "entity");
    for (final Property property : Entities.getProperties(entityID).values()) {
      validate(valueMap, property.getPropertyID(), action);
    }
  }

  @Override
  public final void validate(final ValueMap<String, Object> valueMap, final String key, final int action) throws ValidationException {
    validate((Entity) valueMap, key, action);
  }

  public void validate(final Entity entity, final String propertyID, final int action) throws ValidationException {
    Util.rejectNullValue(entity, "entity");
    final Property property = entity.getProperty(propertyID);
    if (Configuration.getBooleanValue(Configuration.PERFORM_NULL_VALIDATION) && !property.hasParentProperty()) {
      performNullValidation(entity, property, action);
    }
    if (property.isNumerical()) {
      performRangeValidation(entity, property);
    }
  }

  public final void performRangeValidation(final Entity entity, final Property property) throws RangeValidationException {
    if (entity.isValueNull(property.getPropertyID())) {
      return;
    }

    final Double value = property.isDouble() ? (Double) entity.getValue(property.getPropertyID())
            : (Integer) entity.getValue(property.getPropertyID());
    if (value < (property.getMin() == null ? Double.NEGATIVE_INFINITY : property.getMin())) {
      throw new RangeValidationException(property.getPropertyID(), value, "'" + property + "' " +
              FrameworkMessages.get(FrameworkMessages.PROPERTY_VALUE_TOO_SMALL) + " " + property.getMin());
    }
    if (value > (property.getMax() == null ? Double.POSITIVE_INFINITY : property.getMax())) {
      throw new RangeValidationException(property.getPropertyID(), value, "'" + property + "' " +
              FrameworkMessages.get(FrameworkMessages.PROPERTY_VALUE_TOO_LARGE) + " " + property.getMax());
    }
  }

  public final void performNullValidation(final Entity entity, final Property property, final int action) throws NullValidationException {
    if (!isNullable(entity, property.getPropertyID()) && entity.isValueNull(property.getPropertyID())) {
      if (action == INSERT) {
        final boolean columnPropertyWithoutDefaultValue = property instanceof Property.ColumnProperty && !((Property.ColumnProperty) property).columnHasDefaultValue();
        final boolean primaryKeyPropertyWithoutAutoGenerate = property instanceof Property.PrimaryKeyProperty && !Entities.isPrimaryKeyAutoGenerated(entityID);
        if (property instanceof Property.ForeignKeyProperty || columnPropertyWithoutDefaultValue || primaryKeyPropertyWithoutAutoGenerate) {
          throw new NullValidationException(property.getPropertyID(),
                  FrameworkMessages.get(FrameworkMessages.PROPERTY_VALUE_IS_REQUIRED) + ": " + property);
        }
      }
      else {
        throw new NullValidationException(property.getPropertyID(),
                FrameworkMessages.get(FrameworkMessages.PROPERTY_VALUE_IS_REQUIRED) + ": " + property);

      }
    }
  }
}
