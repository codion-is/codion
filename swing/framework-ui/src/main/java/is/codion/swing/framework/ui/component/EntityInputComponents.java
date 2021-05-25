/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.ui.component;

import is.codion.common.state.StateObserver;
import is.codion.framework.domain.entity.Attribute;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.ForeignKey;
import is.codion.framework.domain.property.Property;
import is.codion.framework.domain.property.ValueListProperty;
import is.codion.swing.common.ui.value.ComponentValue;

import javax.swing.JComponent;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;

/**
 * Provides input components for editing entities.
 */
public final class EntityInputComponents extends EntityComponentBuilders {

  /**
   * Instantiates a new EntityInputComponents, for creating input
   * components for a single entity type.
   * @param entityDefinition the definition of the entity
   */
  public EntityInputComponents(final EntityDefinition entityDefinition) {
    super(entityDefinition);
  }

  /**
   * @param attribute the attribute for which to create the input component
   * @param <T> the attribute type
   * @param <C> the component type
   * @return the component handling input for {@code attribute}
   * @throws IllegalArgumentException in case the attribute type is not supported
   */
  public <T, C extends JComponent> ComponentValue<T, C> createInputComponent(final Attribute<T> attribute) {
    return createInputComponent(attribute, null);
  }

  /**
   * @param <T> the attribute type
   * @param <C> the component type
   * @param attribute the attribute for which to create the input component
   * @param enabledState the enabled state
   * @return the component handling input for {@code attribute}
   * @throws IllegalArgumentException in case the attribute type is not supported
   */
  public <T, C extends JComponent> ComponentValue<T, C> createInputComponent(final Attribute<T> attribute,
                                                                             final StateObserver enabledState) {
    if (attribute instanceof ForeignKey) {
      throw new IllegalArgumentException("ForeignKeys are not supported");
    }
    final Property<T> property = getEntityDefinition().getProperty(attribute);
    if (property instanceof ValueListProperty) {
      return (ComponentValue<T, C>) valueListComboBoxBuilder(attribute)
              .enabledState(enabledState)
              .buildComponentValue();
    }
    if (attribute.isLocalTime()) {
      return (ComponentValue<T, C>) localTimeFieldBuilder((Attribute<LocalTime>) attribute)
              .dateTimePattern(property.getDateTimePattern())
              .enabledState(enabledState)
              .buildComponentValue();
    }
    if (attribute.isLocalDate()) {
      return (ComponentValue<T, C>) localDateFieldBuilder((Attribute<LocalDate>) attribute)
              .dateTimePattern(property.getDateTimePattern())
              .enabledState(enabledState)
              .buildComponentValue();
    }
    if (attribute.isLocalDateTime()) {
      return (ComponentValue<T, C>) localDateTimeFieldBuilder((Attribute<LocalDateTime>) attribute)
              .dateTimePattern(property.getDateTimePattern())
              .enabledState(enabledState)
              .buildComponentValue();
    }
    if (attribute.isOffsetDateTime()) {
      return (ComponentValue<T, C>) offsetDateTimeFieldBuilder((Attribute<OffsetDateTime>) attribute)
              .dateTimePattern(property.getDateTimePattern())
              .enabledState(enabledState)
              .buildComponentValue();
    }
    if (attribute.isString() || attribute.isCharacter()) {
      return (ComponentValue<T, C>) textFieldBuilder(attribute)
              .enabledState(enabledState)
              .buildComponentValue();
    }
    if (attribute.isBoolean()) {
      return (ComponentValue<T, C>) checkBoxBuilder((Attribute<Boolean>) attribute)
              .enabledState(enabledState)
              .nullable(property.isNullable())
              .buildComponentValue();
    }
    if (attribute.isInteger()) {
      return (ComponentValue<T, C>) integerFieldBuilder((Attribute<Integer>) attribute)
              .enabledState(enabledState)
              .minimumValue(property.getMinimumValue())
              .maximumValue(property.getMaximumValue())
              .maximumLength(property.getMaximumLength())
              .buildComponentValue();

    }
    if (attribute.isLong()) {
      return (ComponentValue<T, C>) longFieldBuilder((Attribute<Long>) attribute)
              .enabledState(enabledState)
              .minimumValue(property.getMinimumValue())
              .maximumValue(property.getMaximumValue())
              .maximumLength(property.getMaximumLength())
              .buildComponentValue();

    }
    if (attribute.isDouble()) {
      return (ComponentValue<T, C>) doubleFieldBuilder((Attribute<Double>) attribute)
              .enabledState(enabledState)
              .minimumValue(property.getMinimumValue())
              .maximumValue(property.getMaximumValue())
              .maximumFractionDigits(property.getMaximumFractionDigits())
              .maximumLength(property.getMaximumLength())
              .buildComponentValue();

    }
    if (attribute.isBigDecimal()) {
      return (ComponentValue<T, C>) bigDecimalFieldBuilder((Attribute<BigDecimal>) attribute)
              .enabledState(enabledState)
              .minimumValue(property.getMinimumValue())
              .maximumValue(property.getMaximumValue())
              .maximumFractionDigits(property.getMaximumFractionDigits())
              .maximumLength(property.getMaximumLength())
              .buildComponentValue();
    }

    throw new IllegalArgumentException("No input component available for attribute: " + attribute + " (type: " + attribute.getTypeClass() + ")");
  }
}
