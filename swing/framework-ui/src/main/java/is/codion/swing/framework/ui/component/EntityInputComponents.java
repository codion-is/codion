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
import is.codion.swing.common.ui.value.UpdateOn;

import javax.swing.JComponent;

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
    if (attribute.isBoolean()) {
      return (ComponentValue<T, C>) checkBoxBuilder((Attribute<Boolean>) attribute)
              .enabledState(enabledState)
              .nullable(property.isNullable())
              .buildComponentValue();
    }
    if (attribute.isTemporal() || attribute.isNumerical() || attribute.isString() || attribute.isCharacter()) {
      return (ComponentValue<T, C>) textFieldBuilder(attribute)
              .enabledState(enabledState)
              .updateOn(UpdateOn.KEYSTROKE)
              .buildComponentValue();
    }

    throw new IllegalArgumentException("No input component available for attribute: " + attribute + " (type: " + attribute.getTypeClass() + ")");
  }
}
