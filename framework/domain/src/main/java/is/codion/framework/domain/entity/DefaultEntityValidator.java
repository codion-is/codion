/*
 * Copyright (c) 2019 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.entity;

import is.codion.framework.domain.entity.exception.ItemValidationException;
import is.codion.framework.domain.entity.exception.LengthValidationException;
import is.codion.framework.domain.entity.exception.NullValidationException;
import is.codion.framework.domain.entity.exception.RangeValidationException;
import is.codion.framework.domain.entity.exception.ValidationException;
import is.codion.framework.domain.property.ColumnProperty;
import is.codion.framework.domain.property.DerivedProperty;
import is.codion.framework.domain.property.ForeignKeyProperty;
import is.codion.framework.domain.property.ItemProperty;
import is.codion.framework.domain.property.Property;

import java.io.Serializable;
import java.text.MessageFormat;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

/**
 * A default {@link EntityValidator} implementation providing null validation for properties marked as not null,
 * item validation for item based properties, range validation for numerical properties with max and/or min values
 * specified and string length validation based on the specified max length.
 * This Validator can be extended to provide further validation.
 * @see Property.Builder#nullable(boolean)
 * @see Property.Builder#valueRange(Number, Number)
 * @see Property.Builder#maximumLength(int)
 */
public class DefaultEntityValidator implements EntityValidator, Serializable {

  private static final long serialVersionUID = 1;

  private static final ResourceBundle MESSAGES = ResourceBundle.getBundle(DefaultEntityValidator.class.getName());

  private static final String ENTITY_PARAM = "entity";
  private static final String ATTRIBUTE_PARAM = "attribute";
  private static final String VALUE_REQUIRED_KEY = "property_value_is_required";
  private static final String INVALID_ITEM_VALUE_KEY = "invalid_item_value";

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
    return requireNonNull(entity).definition().property(attribute).isNullable();
  }

  @Override
  public void validate(Entity entity) throws ValidationException {
    requireNonNull(entity, ENTITY_PARAM);
    List<Attribute<?>> attributes = entity.definition().properties().stream()
            .filter(DefaultEntityValidator::validationRequired)
            .map(Property::attribute)
            .collect(Collectors.toList());
    for (Attribute<?> attribute : attributes) {
      validate(entity, attribute);
    }
  }

  @Override
  public <T> void validate(Entity entity, Attribute<T> attribute) throws ValidationException {
    requireNonNull(entity, ENTITY_PARAM);
    requireNonNull(attribute, ATTRIBUTE_PARAM);
    Property<T> property = entity.definition().property(attribute);
    if (!entity.definition().isForeignKeyAttribute(attribute)) {
      performNullValidation(entity, property);
    }
    if (property instanceof ItemProperty) {
      performItemValidation(entity, (ItemProperty<T>) property);
    }
    if (attribute.isNumerical()) {
      performRangeValidation(entity, (Attribute<Number>) attribute);
    }
    else if (attribute.isString()) {
      performLengthValidation(entity, (Attribute<String>) attribute);
    }
  }

  private <T> void performNullValidation(Entity entity, Property<T> property) throws NullValidationException {
    requireNonNull(entity, ENTITY_PARAM);
    requireNonNull(property, "property");
    Attribute<T> attribute = property.attribute();
    if (!isNullable(entity, attribute) && entity.isNull(attribute)) {
      if ((entity.primaryKey().isNull() || entity.originalPrimaryKey().isNull()) && !(property instanceof ForeignKeyProperty)) {
        //a new entity being inserted, allow null for columns with default values and generated primary key values
        boolean nonKeyColumnPropertyWithoutDefaultValue = isNonKeyColumnPropertyWithoutDefaultValue(property);
        boolean primaryKeyPropertyWithoutAutoGenerate = isNonGeneratedPrimaryKeyProperty(entity.definition(), property);
        if (nonKeyColumnPropertyWithoutDefaultValue || primaryKeyPropertyWithoutAutoGenerate) {
          throw new NullValidationException(attribute,
                  MessageFormat.format(MESSAGES.getString(VALUE_REQUIRED_KEY), property.caption()));
        }
      }
      else {
        throw new NullValidationException(attribute,
                MessageFormat.format(MESSAGES.getString(VALUE_REQUIRED_KEY), property.caption()));
      }
    }
  }

  private <T> void performItemValidation(Entity entity, ItemProperty<T> property) throws ItemValidationException {
    if (entity.isNull(property.attribute()) && isNullable(entity, property.attribute())) {
      return;
    }
    T value = entity.get(property.attribute());
    if (!property.isValid(value)) {
      throw new ItemValidationException(property.attribute(), value, MESSAGES.getString(INVALID_ITEM_VALUE_KEY) + ": " + value);
    }
  }

  private static <T extends Number> void performRangeValidation(Entity entity, Attribute<T> attribute) throws RangeValidationException {
    requireNonNull(entity, ENTITY_PARAM);
    requireNonNull(attribute, ATTRIBUTE_PARAM);
    if (entity.isNull(attribute)) {
      return;
    }

    Property<T> property = entity.definition().property(attribute);
    Number value = entity.get(property.attribute());
    Number minimumValue = property.minimumValue();
    if (minimumValue != null && value.doubleValue() < minimumValue.doubleValue()) {
      throw new RangeValidationException(property.attribute(), value, "'" + property.caption() + "' " +
              MESSAGES.getString("property_value_too_small") + " " + minimumValue);
    }
    Number maximumValue = property.maximumValue();
    if (maximumValue != null && value.doubleValue() > maximumValue.doubleValue()) {
      throw new RangeValidationException(property.attribute(), value, "'" + property.caption() + "' " +
              MESSAGES.getString("property_value_too_large") + " " + maximumValue);
    }
  }

  private static void performLengthValidation(Entity entity, Attribute<String> attribute) throws LengthValidationException {
    requireNonNull(entity, ENTITY_PARAM);
    requireNonNull(attribute, ATTRIBUTE_PARAM);
    if (entity.isNull(attribute)) {
      return;
    }

    Property<?> property = entity.definition().property(attribute);
    int maximumLength = property.maximumLength();
    String value = entity.get(attribute);
    if (maximumLength != -1 && value.length() > maximumLength) {
      throw new LengthValidationException(property.attribute(), value, "'" + property.caption() + "' " +
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
    if (property instanceof DerivedProperty) {
      return false;
    }
    if (property instanceof ColumnProperty && ((ColumnProperty<?>) property).isReadOnly()) {
      return false;
    }

    return true;
  }
}
