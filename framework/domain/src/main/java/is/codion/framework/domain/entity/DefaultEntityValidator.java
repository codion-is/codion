/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.entity;

import is.codion.framework.domain.entity.exception.LengthValidationException;
import is.codion.framework.domain.entity.exception.NullValidationException;
import is.codion.framework.domain.entity.exception.RangeValidationException;
import is.codion.framework.domain.entity.exception.ValidationException;
import is.codion.framework.domain.property.AuditProperty;
import is.codion.framework.domain.property.ColumnProperty;
import is.codion.framework.domain.property.DerivedProperty;
import is.codion.framework.domain.property.ForeignKeyProperty;
import is.codion.framework.domain.property.Property;
import is.codion.framework.domain.property.SubqueryProperty;

import java.io.Serializable;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

/**
 * A default {@link EntityValidator} implementation providing null validation for properties marked as not null,
 * range validation for numerical properties with max and/or min values specified and string length validation
 * based on the specified max length.
 * This Validator can be extended to provide further validation.
 * @see Property.Builder#nullable(boolean)
 * @see Property.Builder#range(double, double)
 * @see Property.Builder#maximumLength(int)
 */
public class DefaultEntityValidator implements EntityValidator, Serializable {

  private static final long serialVersionUID = 1;

  private static final ResourceBundle MESSAGES = ResourceBundle.getBundle(DefaultEntityValidator.class.getName());

  private static final String ENTITY_PARAM = "entity";
  private static final String PROPERTY_PARAM = "property";
  private static final String VALUE_REQUIRED_KEY = "property_value_is_required";

  private boolean performNullValidation = true;

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
    catch (ValidationException e) {
      return false;
    }
  }

  @Override
  public <T> boolean isNullable(final Entity entity, final Property<T> property) {
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
    List<Property<?>> properties = definition.getProperties().stream()
            .filter(DefaultEntityValidator::validationRequired)
            .collect(Collectors.toList());
    for (final Property<?> property : properties) {
      validate(entity, definition, property);
    }
  }

  @Override
  public <T> void validate(final Entity entity, final EntityDefinition definition, final Property<T> property) throws ValidationException {
    Objects.requireNonNull(entity, ENTITY_PARAM);
    Objects.requireNonNull(property, PROPERTY_PARAM);
    if (performNullValidation && !definition.isForeignKeyAttribute(property.getAttribute())) {
      performNullValidation(entity, definition, property);
    }
    if (property.getAttribute().isNumerical()) {
      performRangeValidation(entity, (Property<Number>) property);
    }
    else if (property.getAttribute().isString()) {
      performLengthValidation(entity, (Property<String>) property);
    }
  }

  @Override
  public final <T extends Number> void performRangeValidation(final Entity entity, final Property<T> property) throws RangeValidationException {
    Objects.requireNonNull(entity, ENTITY_PARAM);
    Objects.requireNonNull(property, PROPERTY_PARAM);
    if (entity.isNull(property.getAttribute())) {
      return;
    }

    Number value = entity.get(property.getAttribute());
    if (value.doubleValue() < (property.getMinimumValue() == null ? Double.NEGATIVE_INFINITY : property.getMinimumValue())) {
      throw new RangeValidationException(property.getAttribute(), value, "'" + property.getCaption() + "' " +
              MESSAGES.getString("property_value_too_small") + " " + property.getMinimumValue());
    }
    if (value.doubleValue() > (property.getMaximumValue() == null ? Double.POSITIVE_INFINITY : property.getMaximumValue())) {
      throw new RangeValidationException(property.getAttribute(), value, "'" + property.getCaption() + "' " +
              MESSAGES.getString("property_value_too_large") + " " + property.getMaximumValue());
    }
  }

  @Override
  public final <T> void performNullValidation(final Entity entity, final EntityDefinition definition,
                                              final Property<T> property) throws NullValidationException {
    Objects.requireNonNull(entity, ENTITY_PARAM);
    Objects.requireNonNull(property, PROPERTY_PARAM);
    if (!isNullable(entity, property) && entity.isNull(property.getAttribute())) {
      if ((entity.getPrimaryKey().isNull() || entity.getOriginalPrimaryKey().isNull()) && !(property instanceof ForeignKeyProperty)) {
        //a new entity being inserted, allow null for columns with default values and generated primary key values
        boolean nonKeyColumnPropertyWithoutDefaultValue = isNonKeyColumnPropertyWithoutDefaultValue(property);
        boolean primaryKeyPropertyWithoutAutoGenerate = isNonGeneratedPrimaryKeyProperty(definition, property);
        if (nonKeyColumnPropertyWithoutDefaultValue || primaryKeyPropertyWithoutAutoGenerate) {
          throw new NullValidationException(property.getAttribute(),
                  MessageFormat.format(MESSAGES.getString(VALUE_REQUIRED_KEY), property.getCaption()));
        }
      }
      else {
        throw new NullValidationException(property.getAttribute(),
                MessageFormat.format(MESSAGES.getString(VALUE_REQUIRED_KEY), property.getCaption()));
      }
    }
  }

  @Override
  public final void performLengthValidation(final Entity entity, final Property<String> property) throws LengthValidationException {
    Objects.requireNonNull(entity, ENTITY_PARAM);
    Objects.requireNonNull(property, PROPERTY_PARAM);
    if (entity.isNull(property.getAttribute())) {
      return;
    }

    int maximumLength = property.getMaximumLength();
    String value = entity.get(property.getAttribute());
    if (maximumLength != -1 && value.length() > maximumLength) {
      throw new LengthValidationException(property.getAttribute(), value, "'" + property.getCaption() + "' " +
              MESSAGES.getString("property_value_too_long") + " " + maximumLength + "\n:'" + value + "'");
    }
  }

  private static boolean isNonGeneratedPrimaryKeyProperty(final EntityDefinition definition, final Property<?> property) {
    return (property instanceof ColumnProperty
            && ((ColumnProperty<?>) property).isPrimaryKeyColumn()) && !definition.isKeyGenerated();
  }

  private static boolean isNonKeyColumnPropertyWithoutDefaultValue(final Property<?> property) {
    return property instanceof ColumnProperty && !((ColumnProperty<?>) property).isPrimaryKeyColumn()
            && !((ColumnProperty<?>) property).columnHasDefaultValue();
  }

  private static boolean validationRequired(final Property<?> property) {
    return !(property instanceof DerivedProperty || property instanceof SubqueryProperty || property instanceof AuditProperty);
  }
}
