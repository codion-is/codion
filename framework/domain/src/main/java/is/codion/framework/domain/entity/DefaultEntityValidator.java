/*
 * Copyright (c) 2019 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.entity;

import is.codion.framework.domain.entity.attribute.Attribute;
import is.codion.framework.domain.entity.attribute.AttributeDefinition;
import is.codion.framework.domain.entity.attribute.Column;
import is.codion.framework.domain.entity.attribute.ColumnDefinition;
import is.codion.framework.domain.entity.attribute.ForeignKeyDefinition;
import is.codion.framework.domain.entity.attribute.ItemColumnDefinition;
import is.codion.framework.domain.entity.exception.ItemValidationException;
import is.codion.framework.domain.entity.exception.LengthValidationException;
import is.codion.framework.domain.entity.exception.NullValidationException;
import is.codion.framework.domain.entity.exception.RangeValidationException;
import is.codion.framework.domain.entity.exception.ValidationException;

import java.io.Serializable;
import java.text.MessageFormat;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

/**
 * A default {@link EntityValidator} implementation providing null validation for attributes marked as not null,
 * item validation for item based attributes, range validation for numerical attributes with max and/or min values
 * specified and string length validation based on the specified max length.
 * This Validator can be extended to provide further validation.
 * @see AttributeDefinition.Builder#nullable(boolean)
 * @see AttributeDefinition.Builder#valueRange(Number, Number)
 * @see AttributeDefinition.Builder#maximumLength(int)
 */
public class DefaultEntityValidator implements EntityValidator, Serializable {

  private static final long serialVersionUID = 1;

  private static final ResourceBundle MESSAGES = ResourceBundle.getBundle(DefaultEntityValidator.class.getName());

  private static final String ENTITY_PARAM = "entity";
  private static final String ATTRIBUTE_PARAM = "attribute";
  private static final String VALUE_REQUIRED_KEY = "value_is_required";
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
    return requireNonNull(entity).entityDefinition().attributeDefinition(attribute).isNullable();
  }

  @Override
  public void validate(Entity entity) throws ValidationException {
    requireNonNull(entity, ENTITY_PARAM);
    List<Attribute<?>> attributes = entity.entityDefinition().attributeDefinitions().stream()
            .filter(DefaultEntityValidator::validationRequired)
            .map(AttributeDefinition::attribute)
            .collect(Collectors.toList());
    for (Attribute<?> attribute : attributes) {
      validate(entity, attribute);
    }
  }

  @Override
  public <T> void validate(Entity entity, Attribute<T> attribute) throws ValidationException {
    requireNonNull(entity, ENTITY_PARAM);
    requireNonNull(attribute, ATTRIBUTE_PARAM);
    AttributeDefinition<T> definition = entity.entityDefinition().attributeDefinition(attribute);
    if (!(attribute instanceof Column) || !entity.entityDefinition().isForeignKeyColumn((Column<?>) attribute)) {
      performNullValidation(entity, definition);
    }
    if (definition instanceof ItemColumnDefinition) {
      performItemValidation(entity, (ItemColumnDefinition<T>) definition);
    }
    if (attribute.type().isNumerical()) {
      performRangeValidation(entity, (Attribute<Number>) attribute);
    }
    else if (attribute.type().isString()) {
      performLengthValidation(entity, (Attribute<String>) attribute);
    }
  }

  private <T> void performNullValidation(Entity entity, AttributeDefinition<T> attributeDefinition) throws NullValidationException {
    requireNonNull(entity, ENTITY_PARAM);
    requireNonNull(attributeDefinition, "attributeDefinition");
    Attribute<T> attribute = attributeDefinition.attribute();
    if (!isNullable(entity, attribute) && entity.isNull(attribute)) {
      if ((entity.primaryKey().isNull() || entity.originalPrimaryKey().isNull()) && !(attributeDefinition instanceof ForeignKeyDefinition)) {
        //a new entity being inserted, allow null for columns with default values and generated primary key values
        boolean nonKeyColumnWithoutDefaultValue = isNonKeyColumnWithoutDefaultValue(attributeDefinition);
        boolean primaryKeyColumnWithoutAutoGenerate = isNonGeneratedPrimaryKeyColumn(entity.entityDefinition(), attributeDefinition);
        if (nonKeyColumnWithoutDefaultValue || primaryKeyColumnWithoutAutoGenerate) {
          throw new NullValidationException(attribute,
                  MessageFormat.format(MESSAGES.getString(VALUE_REQUIRED_KEY), attributeDefinition.caption()));
        }
      }
      else {
        throw new NullValidationException(attribute,
                MessageFormat.format(MESSAGES.getString(VALUE_REQUIRED_KEY), attributeDefinition.caption()));
      }
    }
  }

  private <T> void performItemValidation(Entity entity, ItemColumnDefinition<T> columnDefinition) throws ItemValidationException {
    if (entity.isNull(columnDefinition.attribute()) && isNullable(entity, columnDefinition.attribute())) {
      return;
    }
    T value = entity.get(columnDefinition.attribute());
    if (!columnDefinition.isValid(value)) {
      throw new ItemValidationException(columnDefinition.attribute(), value, MESSAGES.getString(INVALID_ITEM_VALUE_KEY) + ": " + value);
    }
  }

  private static <T extends Number> void performRangeValidation(Entity entity, Attribute<T> attribute) throws RangeValidationException {
    requireNonNull(entity, ENTITY_PARAM);
    requireNonNull(attribute, ATTRIBUTE_PARAM);
    if (entity.isNull(attribute)) {
      return;
    }

    AttributeDefinition<T> definition = entity.entityDefinition().attributeDefinition(attribute);
    Number value = entity.get(definition.attribute());
    Number minimumValue = definition.minimumValue();
    if (minimumValue != null && value.doubleValue() < minimumValue.doubleValue()) {
      throw new RangeValidationException(definition.attribute(), value, "'" + definition.caption() + "' " +
              MESSAGES.getString("value_too_small") + " " + minimumValue);
    }
    Number maximumValue = definition.maximumValue();
    if (maximumValue != null && value.doubleValue() > maximumValue.doubleValue()) {
      throw new RangeValidationException(definition.attribute(), value, "'" + definition.caption() + "' " +
              MESSAGES.getString("value_too_large") + " " + maximumValue);
    }
  }

  private static void performLengthValidation(Entity entity, Attribute<String> attribute) throws LengthValidationException {
    requireNonNull(entity, ENTITY_PARAM);
    requireNonNull(attribute, ATTRIBUTE_PARAM);
    if (entity.isNull(attribute)) {
      return;
    }

    AttributeDefinition<?> definition = entity.entityDefinition().attributeDefinition(attribute);
    int maximumLength = definition.maximumLength();
    String value = entity.get(attribute);
    if (maximumLength != -1 && value.length() > maximumLength) {
      throw new LengthValidationException(definition.attribute(), value, "'" + definition.caption() + "' " +
              MESSAGES.getString("value_too_long") + " " + maximumLength + "\n:'" + value + "'");
    }
  }

  private static boolean isNonGeneratedPrimaryKeyColumn(EntityDefinition definition, AttributeDefinition<?> attributeDefinition) {
    return (attributeDefinition instanceof ColumnDefinition
            && ((ColumnDefinition<?>) attributeDefinition).isPrimaryKeyColumn())
            && !definition.isKeyGenerated();
  }

  private static boolean isNonKeyColumnWithoutDefaultValue(AttributeDefinition<?> definition) {
    return definition instanceof ColumnDefinition
            && !((ColumnDefinition<?>) definition).isPrimaryKeyColumn()
            && !((ColumnDefinition<?>) definition).columnHasDefaultValue();
  }

  private static boolean validationRequired(AttributeDefinition<?> definition) {
    if (definition.isDerived()) {
      return false;
    }
    if (definition instanceof ColumnDefinition && ((ColumnDefinition<?>) definition).isReadOnly()) {
      return false;
    }

    return true;
  }
}
