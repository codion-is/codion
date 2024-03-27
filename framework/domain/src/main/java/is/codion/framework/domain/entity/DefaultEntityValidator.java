/*
 * This file is part of Codion.
 *
 * Codion is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Codion is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Codion.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (c) 2019 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.framework.domain.entity;

import is.codion.framework.domain.entity.attribute.Attribute;
import is.codion.framework.domain.entity.attribute.AttributeDefinition;
import is.codion.framework.domain.entity.attribute.Column;
import is.codion.framework.domain.entity.attribute.ColumnDefinition;
import is.codion.framework.domain.entity.attribute.ForeignKeyDefinition;
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

	private final boolean strictValidation;

	/**
	 * Instantiates a new DefaultEntityValidator
	 * @see #STRICT_VALIDATION
	 */
	public DefaultEntityValidator() {
		this(STRICT_VALIDATION.get());
	}

	/**
	 * Instantiates a new DefaultEntityValidator
	 * @param strictValidation true if strict validation should be performed
	 * @see #STRICT_VALIDATION
	 */
	public DefaultEntityValidator(boolean strictValidation) {
		this.strictValidation = strictValidation;
	}

	@Override
	public boolean valid(Entity entity) {
		try {
			validate(entity);
			return true;
		}
		catch (ValidationException e) {
			return false;
		}
	}

	@Override
	public <T> boolean nullable(Entity entity, Attribute<T> attribute) {
		return requireNonNull(entity).definition().attributes().definition(attribute).nullable();
	}

	@Override
	public void validate(Entity entity) throws ValidationException {
		requireNonNull(entity, ENTITY_PARAM);
		List<Attribute<?>> attributes = entity.definition().attributes().definitions().stream()
						.filter(definition -> validationRequired(entity, definition))
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
		AttributeDefinition<T> definition = entity.definition().attributes().definition(attribute);
		if (!(attribute instanceof Column) || !entity.definition().foreignKeys().foreignKeyColumn((Column<?>) attribute)) {
			performNullValidation(entity, definition);
		}
		if (!definition.items().isEmpty()) {
			performItemValidation(entity, definition);
		}
		if (attribute.type().isNumerical()) {
			performRangeValidation(entity, (Attribute<Number>) attribute);
		}
		else if (attribute.type().isString()) {
			performLengthValidation(entity, (Attribute<String>) attribute);
		}
	}

	private boolean validationRequired(Entity entity, AttributeDefinition<?> definition) {
		if (definition.derived()) {
			return false;
		}
		if (definition instanceof ColumnDefinition && ((ColumnDefinition<?>) definition).readOnly()) {
			return false;
		}
		if (!entity.exists() || strictValidation) {
			// validate all values when inserting or when strict validation is enabled
			return true;
		}

		// only validate modified values when updating
		return entity.modified(definition.attribute());
	}

	private <T> void performNullValidation(Entity entity, AttributeDefinition<T> attributeDefinition) throws NullValidationException {
		requireNonNull(entity, ENTITY_PARAM);
		requireNonNull(attributeDefinition, "attributeDefinition");
		Attribute<T> attribute = attributeDefinition.attribute();
		if (!nullable(entity, attribute) && entity.isNull(attribute)) {
			if ((entity.primaryKey().isNull() || entity.originalPrimaryKey().isNull())
							&& !(attributeDefinition instanceof ForeignKeyDefinition)) {
				//a new entity being inserted, allow null for columns with default values and generated primary key values
				boolean nonKeyColumnWithoutDefaultValue = isNonKeyColumnWithoutDefaultValue(attributeDefinition);
				boolean primaryKeyColumnWithoutAutoGenerate = isNonGeneratedPrimaryKeyColumn(entity.definition(), attributeDefinition);
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

	private <T> void performItemValidation(Entity entity, AttributeDefinition<T> attributeDefinition) throws ItemValidationException {
		if (entity.isNull(attributeDefinition.attribute()) && nullable(entity, attributeDefinition.attribute())) {
			return;
		}
		T value = entity.get(attributeDefinition.attribute());
		if (!attributeDefinition.validItem(value)) {
			throw new ItemValidationException(attributeDefinition.attribute(), value, MESSAGES.getString(INVALID_ITEM_VALUE_KEY) + ": " + value);
		}
	}

	private static <T extends Number> void performRangeValidation(Entity entity, Attribute<T> attribute) throws RangeValidationException {
		requireNonNull(entity, ENTITY_PARAM);
		requireNonNull(attribute, ATTRIBUTE_PARAM);
		if (entity.isNull(attribute)) {
			return;
		}

		AttributeDefinition<T> definition = entity.definition().attributes().definition(attribute);
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

		AttributeDefinition<?> definition = entity.definition().attributes().definition(attribute);
		int maximumLength = definition.maximumLength();
		String value = entity.get(attribute);
		if (maximumLength != -1 && value.length() > maximumLength) {
			throw new LengthValidationException(definition.attribute(), value, "'" + definition.caption() + "' " +
							MESSAGES.getString("value_too_long") + " " + maximumLength + "\n:'" + value + "'");
		}
	}

	private static boolean isNonGeneratedPrimaryKeyColumn(EntityDefinition definition, AttributeDefinition<?> attributeDefinition) {
		return (attributeDefinition instanceof ColumnDefinition
						&& ((ColumnDefinition<?>) attributeDefinition).primaryKey())
						&& !definition.primaryKey().generated();
	}

	private static boolean isNonKeyColumnWithoutDefaultValue(AttributeDefinition<?> definition) {
		return definition instanceof ColumnDefinition
						&& !((ColumnDefinition<?>) definition).primaryKey()
						&& !((ColumnDefinition<?>) definition).columnHasDefaultValue();
	}
}
