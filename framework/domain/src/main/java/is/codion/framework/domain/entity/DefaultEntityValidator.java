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
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

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
  private static final String ATTRIBUTE_PARAM = "attribute";
  private static final String VALUE_REQUIRED_KEY = "property_value_is_required";

  @Override
  public boolean isValid(Entity entity) {
    try {
      validate(entity);
      return true;
    }
    catch (ValidationException e) {
      return false;
    }
  }

  @Override
  public <T> boolean isNullable(Entity entity, Attribute<T> attribute) {
    return requireNonNull(entity).getDefinition().getProperty(attribute).isNullable();
  }

  @Override
  public void validate(Entity entity) throws ValidationException {
    requireNonNull(entity, ENTITY_PARAM);
    List<Attribute<?>> attributes = entity.getDefinition().getProperties().stream()
            .filter(DefaultEntityValidator::validationRequired)
            .map(Property::getAttribute)
            .collect(Collectors.toList());
    for (Attribute<?> attribute : attributes) {
      validate(entity, attribute);
    }
  }

  @Override
  public <T> void validate(Entity entity, Attribute<T> attribute) throws ValidationException {
    requireNonNull(entity, ENTITY_PARAM);
    requireNonNull(attribute, ATTRIBUTE_PARAM);
    if (!entity.getDefinition().isForeignKeyAttribute(attribute)) {
      performNullValidation(entity, attribute);
    }
    if (attribute.isNumerical()) {
      performRangeValidation(entity, (Attribute<Number>) attribute);
    }
    else if (attribute.isString()) {
      performLengthValidation(entity, (Attribute<String>) attribute);
    }
  }

  private <T> void performNullValidation(Entity entity, Attribute<T> attribute) throws NullValidationException {
    requireNonNull(entity, ENTITY_PARAM);
    requireNonNull(attribute, ATTRIBUTE_PARAM);
    Property<T> property = entity.getDefinition().getProperty(attribute);
    if (!isNullable(entity, attribute) && entity.isNull(attribute)) {
      if ((entity.getPrimaryKey().isNull() || entity.getOriginalPrimaryKey().isNull()) && !(property instanceof ForeignKeyProperty)) {
        //a new entity being inserted, allow null for columns with default values and generated primary key values
        boolean nonKeyColumnPropertyWithoutDefaultValue = isNonKeyColumnPropertyWithoutDefaultValue(property);
        boolean primaryKeyPropertyWithoutAutoGenerate = isNonGeneratedPrimaryKeyProperty(entity.getDefinition(), property);
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

  private static <T extends Number> void performRangeValidation(Entity entity, Attribute<T> attribute) throws RangeValidationException {
    requireNonNull(entity, ENTITY_PARAM);
    requireNonNull(attribute, ATTRIBUTE_PARAM);
    if (entity.isNull(attribute)) {
      return;
    }

    Property<T> property = entity.getDefinition().getProperty(attribute);
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

  private static void performLengthValidation(Entity entity, Attribute<String> attribute) throws LengthValidationException {
    requireNonNull(entity, ENTITY_PARAM);
    requireNonNull(attribute, ATTRIBUTE_PARAM);
    if (entity.isNull(attribute)) {
      return;
    }

    Property<?> property = entity.getDefinition().getProperty(attribute);
    int maximumLength = property.getMaximumLength();
    String value = entity.get(attribute);
    if (maximumLength != -1 && value.length() > maximumLength) {
      throw new LengthValidationException(property.getAttribute(), value, "'" + property.getCaption() + "' " +
              MESSAGES.getString("property_value_too_long") + " " + maximumLength + "\n:'" + value + "'");
    }
  }

  private static boolean isNonGeneratedPrimaryKeyProperty(EntityDefinition definition, Property<?> property) {
    return (property instanceof ColumnProperty
            && ((ColumnProperty<?>) property).isPrimaryKeyColumn()) && !definition.isKeyGenerated();
  }

  private static boolean isNonKeyColumnPropertyWithoutDefaultValue(Property<?> property) {
    return property instanceof ColumnProperty && !((ColumnProperty<?>) property).isPrimaryKeyColumn()
            && !((ColumnProperty<?>) property).columnHasDefaultValue();
  }

  private static boolean validationRequired(Property<?> property) {
    return !(property instanceof DerivedProperty || property instanceof SubqueryProperty || property instanceof AuditProperty);
  }
}
