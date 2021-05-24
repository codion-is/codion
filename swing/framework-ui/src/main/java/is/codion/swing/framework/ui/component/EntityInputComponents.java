/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.ui.component;

import is.codion.common.Configuration;
import is.codion.common.state.StateObserver;
import is.codion.common.value.PropertyValue;
import is.codion.framework.domain.entity.Attribute;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.ForeignKey;
import is.codion.framework.domain.property.ForeignKeyProperty;
import is.codion.framework.domain.property.Property;
import is.codion.framework.domain.property.ValueListProperty;
import is.codion.framework.model.EntitySearchModel;
import is.codion.swing.common.ui.component.BooleanComboBoxBuilder;
import is.codion.swing.common.ui.component.CheckBoxBuilder;
import is.codion.swing.common.ui.component.ComboBoxBuilder;
import is.codion.swing.common.ui.component.ComponentBuilders;
import is.codion.swing.common.ui.component.FormattedTextFieldBuilder;
import is.codion.swing.common.ui.component.TemporalInputPanelBuilder;
import is.codion.swing.common.ui.component.TextAreaBuilder;
import is.codion.swing.common.ui.component.TextFieldBuilder;
import is.codion.swing.common.ui.component.TextInputPanelBuilder;
import is.codion.swing.common.ui.component.ValueListComboBoxBuilder;
import is.codion.swing.common.ui.value.ComponentValue;
import is.codion.swing.common.ui.value.UpdateOn;
import is.codion.swing.framework.model.SwingEntityComboBoxModel;

import javax.swing.ComboBoxModel;
import javax.swing.JComponent;
import javax.swing.JLabel;
import java.time.temporal.Temporal;

import static java.util.Objects.requireNonNull;

/**
 * Provides input components for editing entities.
 */
public final class EntityInputComponents {

  /**
   * Specifies the default horizontal alignment used in labels<br>
   * Value type: Integer (JLabel.LEFT, JLabel.RIGHT, JLabel.CENTER)<br>
   * Default value: JLabel.LEFT
   */
  public static final PropertyValue<Integer> LABEL_TEXT_ALIGNMENT = Configuration.integerValue("codion.swing.labelTextAlignment", JLabel.LEFT);

  private static final String ATTRIBUTE_PARAM_NAME = "attribute";

  /**
   * The underlying entity definition
   */
  private final EntityDefinition entityDefinition;

  /**
   * Instantiates a new EntityInputComponents, for creating input
   * components for a single entity type.
   * @param entityDefinition the definition of the entity
   */
  public EntityInputComponents(final EntityDefinition entityDefinition) {
    this.entityDefinition = requireNonNull(entityDefinition);
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
      throw new IllegalArgumentException("Use createForeignKeyComboBox() or createForeignKeySearchField() for ForeignKeys");
    }
    final Property<T> property = entityDefinition.getProperty(attribute);
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

  /**
   * Creates a JLabel with a caption from the given attribute, using the default label text alignment
   * @param attribute the attribute for which to create the label
   * @return a JLabel for the given attribute
   * @see EntityInputComponents#LABEL_TEXT_ALIGNMENT
   */
  public JLabel createLabel(final Attribute<?> attribute) {
    return createLabel(attribute, LABEL_TEXT_ALIGNMENT.get());
  }

  /**
   * Creates a JLabel with a caption from the given attribute
   * @param attribute the attribute for which to create the label
   * @param horizontalAlignment the horizontal text alignment
   * @return a JLabel for the given attribute
   */
  public JLabel createLabel(final Attribute<?> attribute, final int horizontalAlignment) {
    requireNonNull(attribute, ATTRIBUTE_PARAM_NAME);
    final Property<?> property = entityDefinition.getProperty(attribute);
    final JLabel label = new JLabel(property.getCaption(), horizontalAlignment);
    if (property.getMnemonic() != null) {
      label.setDisplayedMnemonic(property.getMnemonic());
    }

    return label;
  }

  /**
   * Creates a builder.
   * @param attribute the attribute
   * @return a builder
   */
  public CheckBoxBuilder checkBoxBuilder(final Attribute<Boolean> attribute) {
    final Property<Boolean> property = entityDefinition.getProperty(attribute);

    return ComponentBuilders.checkBoxBuilder()
            .description(property.getDescription())
            .nullable(property.isNullable())
            .caption(property.getCaption());
  }

  /**
   * Creates a builder.
   * @param attribute the attribute
   * @return a builder
   */
  public BooleanComboBoxBuilder booleanComboBoxBuilder(final Attribute<Boolean> attribute) {
    final Property<Boolean> property = entityDefinition.getProperty(attribute);

    return ComponentBuilders.booleanComboBoxBuilder()
            .description(property.getDescription());
  }

  /**
   * Creates a builder.
   * @param foreignKey the foreign key
   * @param comboBoxModel the combo box model
   * @return a builder
   */
  public ForeignKeyComboBoxBuilder foreignKeyComboBoxBuilder(final ForeignKey foreignKey, final SwingEntityComboBoxModel comboBoxModel) {
    final ForeignKeyProperty foreignKeyProperty = entityDefinition.getForeignKeyProperty(foreignKey);

    return new DefaultForeignKeyComboBoxBuilder(comboBoxModel)
            .description(foreignKeyProperty.getDescription());
  }

  /**
   * Creates a builder.
   * @param foreignKey the foreign key
   * @param searchModel the search model
   * @return a builder
   */
  public ForeignKeySearchFieldBuilder foreignKeySearchFieldBuilder(final ForeignKey foreignKey, final EntitySearchModel searchModel) {
    final ForeignKeyProperty foreignKeyProperty = entityDefinition.getForeignKeyProperty(foreignKey);

    return new DefaultForeignKeySearchFieldBuilder(searchModel)
            .description(foreignKeyProperty.getDescription() == null ? searchModel.getDescription() : foreignKeyProperty.getDescription());
  }

  /**
   * Creates a builder.
   * @param foreignKey the foreign key
   * @return a builder
   */
  public ForeignKeyFieldBuilder foreignKeyFieldBuilder(final ForeignKey foreignKey) {
    final ForeignKeyProperty foreignKeyProperty = entityDefinition.getForeignKeyProperty(foreignKey);

    return new DefaultForeignKeyFieldBuilder()
            .description(foreignKeyProperty.getDescription());
  }

  /**
   * Creates a builder.
   * @param attribute the attribute
   * @param <T> the attribute type
   * @return a builder
   */
  public <T> ValueListComboBoxBuilder<T> valueListComboBoxBuilder(final Attribute<T> attribute) {
    final Property<T> property = entityDefinition.getProperty(attribute);
    if (!(property instanceof ValueListProperty)) {
      throw new IllegalArgumentException("Property based on '" + property.getAttribute() + "' is not a ValueListProperty");
    }

    return ComponentBuilders.valueListComboBoxBuilder(((ValueListProperty<T>) property).getValues())
            .description(property.getDescription())
            .nullable(property.isNullable());
  }

  /**
   * Creates a builder.
   * @param attribute the attribute
   * @param comboBoxModel the combo box model
   * @param <T> the attribute type
   * @return a builder
   */
  public <T> ComboBoxBuilder<T> comboBoxBuilder(final Attribute<T> attribute, final ComboBoxModel<T> comboBoxModel) {
    final Property<T> property = entityDefinition.getProperty(attribute);

    return ComponentBuilders.comboBoxBuilder(attribute.getTypeClass(), comboBoxModel)
            .description(property.getDescription());
  }

  /**
   * Creates a builder.
   * @param attribute the attribute
   * @param <T> the attribute type
   * @return a builder
   */
  public <T extends Temporal> TemporalInputPanelBuilder<T> temporalInputPanelBuilder(final Attribute<T> attribute) {
    if (!attribute.isTemporal()) {
      throw new IllegalArgumentException("Attribute " + attribute + " is not Temporal");
    }
    final Property<T> property = entityDefinition.getProperty(attribute);

    return ComponentBuilders.temporalInputPanelBuiler(attribute.getTypeClass())
            .description(property.getDescription())
            .dateTimePattern(property.getDateTimePattern());
  }

  /**
   * Creates a builder.
   * @param attribute the attribute
   * @return a builder
   */
  public TextInputPanelBuilder textInputPanelBuilder(final Attribute<String> attribute) {
    final Property<String> property = entityDefinition.getProperty(attribute);

    return ComponentBuilders.textInputPanelBuilder()
            .description(property.getDescription())
            .caption(property.getCaption());
  }

  /**
   * Creates a builder.
   * @param attribute the attribute
   * @return a builder
   */
  public TextAreaBuilder textAreaBuilder(final Attribute<String> attribute) {
    final Property<String> property = entityDefinition.getProperty(attribute);
    if (!attribute.isString()) {
      throw new IllegalArgumentException("Cannot create a text area for a non-string attribute");
    }

    return ComponentBuilders.textAreaBuilder()
            .description(property.getDescription());
  }

  /**
   * Creates a builder.
   * @param attribute the attribute
   * @param <T> the attribute type
   * @return a builder
   */
  public <T> TextFieldBuilder<T> textFieldBuilder(final Attribute<T> attribute) {
    final Property<T> property = entityDefinition.getProperty(attribute);

    return ComponentBuilders.textFieldBuilder(attribute.getTypeClass())
            .format(property.getFormat())
            .dateTimePattern(property.getDateTimePattern())
            .minimumValue(property.getMinimumValue())
            .maximumValue(property.getMaximumValue())
            .description(property.getDescription());
  }

  /**
   * Creates a builder.
   * @param attribute the attribute
   * @return a builder
   */
  public FormattedTextFieldBuilder formattedTextFieldBuilder(final Attribute<String> attribute) {
    final Property<String> property = entityDefinition.getProperty(attribute);

    return ComponentBuilders.formattedTextFieldBuilder()
            .description(property.getDescription());
  }
}
