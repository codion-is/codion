/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.ui.component;

import is.codion.common.state.StateObserver;
import is.codion.framework.domain.entity.Attribute;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.ForeignKey;
import is.codion.framework.domain.property.ItemProperty;
import is.codion.framework.domain.property.Property;
import is.codion.swing.common.ui.component.ComponentValue;

import javax.swing.JComponent;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;

/**
 * Provides default input components for editing entities.
 */
public final class EntityInputComponents {

  private final EntityComponents components;

  /**
   * Instantiates a new EntityInputComponents, for creating input
   * components for a single entity type.
   * @param entityDefinition the definition of the entity
   */
  public EntityInputComponents(final EntityDefinition entityDefinition) {
    this.components = new EntityComponents(entityDefinition);
  }

  /**
   * @return the {@link EntityComponents} instance.
   */
  public EntityComponents getComponents() {
    return components;
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
    final Property<T> property = components.getEntityDefinition().getProperty(attribute);
    if (property instanceof ItemProperty) {
      return (ComponentValue<T, C>) components.itemComboBox(attribute)
              .enabledState(enabledState)
              .buildComponentValue();
    }
    if (attribute.isLocalTime()) {
      return (ComponentValue<T, C>) components.localTimeField((Attribute<LocalTime>) attribute)
              .enabledState(enabledState)
              .buildComponentValue();
    }
    if (attribute.isLocalDate()) {
      return (ComponentValue<T, C>) components.localDateField((Attribute<LocalDate>) attribute)
              .enabledState(enabledState)
              .buildComponentValue();
    }
    if (attribute.isLocalDateTime()) {
      return (ComponentValue<T, C>) components.localDateTimeField((Attribute<LocalDateTime>) attribute)
              .enabledState(enabledState)
              .buildComponentValue();
    }
    if (attribute.isOffsetDateTime()) {
      return (ComponentValue<T, C>) components.offsetDateTimeField((Attribute<OffsetDateTime>) attribute)
              .enabledState(enabledState)
              .buildComponentValue();
    }
    if (attribute.isString() || attribute.isCharacter()) {
      return (ComponentValue<T, C>) components.textField(attribute)
              .enabledState(enabledState)
              .buildComponentValue();
    }
    if (attribute.isBoolean()) {
      return (ComponentValue<T, C>) components.checkBox((Attribute<Boolean>) attribute)
              .enabledState(enabledState)
              .buildComponentValue();
    }
    if (attribute.isInteger()) {
      return (ComponentValue<T, C>) components.integerField((Attribute<Integer>) attribute)
              .enabledState(enabledState)
              .buildComponentValue();
    }
    if (attribute.isLong()) {
      return (ComponentValue<T, C>) components.longField((Attribute<Long>) attribute)
              .enabledState(enabledState)
              .buildComponentValue();
    }
    if (attribute.isDouble()) {
      return (ComponentValue<T, C>) components.doubleField((Attribute<Double>) attribute)
              .enabledState(enabledState)
              .buildComponentValue();
    }
    if (attribute.isBigDecimal()) {
      return (ComponentValue<T, C>) components.bigDecimalField((Attribute<BigDecimal>) attribute)
              .enabledState(enabledState)
              .buildComponentValue();
    }

    throw new IllegalArgumentException("No input component available for attribute: " + attribute + " (type: " + attribute.getTypeClass() + ")");
  }
}
