/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.ui.builder;

import is.codion.common.Configuration;
import is.codion.common.state.StateObserver;
import is.codion.common.value.PropertyValue;
import is.codion.common.value.Value;
import is.codion.framework.domain.entity.Attribute;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.ForeignKey;
import is.codion.framework.domain.property.Property;
import is.codion.framework.domain.property.ValueListProperty;
import is.codion.framework.model.EntitySearchModel;
import is.codion.swing.common.ui.combobox.Completion;
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
   * Specifies whether maximum match or autocomplete is used for comboboxes,
   * {@link Completion#COMPLETION_MODE_MAXIMUM_MATCH} for maximum match
   * and {@link Completion#COMPLETION_MODE_AUTOCOMPLETE} for auto completion.<br>
   * Value type:String<br>
   * Default value: {@link Completion#COMPLETION_MODE_MAXIMUM_MATCH}
   */
  public static final PropertyValue<String> COMBO_BOX_COMPLETION_MODE = Configuration.stringValue("codion.swing.comboBoxCompletionMode", Completion.COMPLETION_MODE_MAXIMUM_MATCH);

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
   * @param value the value to bind to the field
   * @param <T> the attribute type
   * @return the component handling input for {@code attribute}
   * @throws IllegalArgumentException in case the attribute type is not supported
   */
  public <T> JComponent createInputComponent(final Attribute<T> attribute, final Value<T> value) {
    return createInputComponent(attribute, value, null);
  }

  /**
   * @param attribute the attribute for which to create the input component
   * @param value the value to bind to the field
   * @param enabledState the enabled state
   * @param <T> the attribute type
   * @return the component handling input for {@code attribute}
   * @throws IllegalArgumentException in case the attribute type is not supported
   */
  public <T> JComponent createInputComponent(final Attribute<T> attribute, final Value<T> value,
                                             final StateObserver enabledState) {
    if (attribute instanceof ForeignKey) {
      throw new IllegalArgumentException("Use createForeignKeyComboBox() or createForeignKeySearchField() for ForeignKeys");
    }
    final Property<T> property = entityDefinition.getProperty(attribute);
    if (property instanceof ValueListProperty) {
      return valueListComboBoxBuilder(attribute, value)
              .enabledState(enabledState)
              .build();
    }
    if (attribute.isBoolean()) {
      return checkBoxBuilder((Attribute<Boolean>) attribute, (Value<Boolean>) value)
              .enabledState(enabledState)
              .nullable(property.isNullable())
              .includeCaption(false)
              .build();
    }
    if (attribute.isTemporal() || attribute.isNumerical() || attribute.isString() || attribute.isCharacter()) {
      return textFieldBuilder(attribute, value)
              .enabledState(enabledState)
              .updateOn(UpdateOn.KEYSTROKE)
              .build();
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

  public CheckBoxBuilder checkBoxBuilder(final Attribute<Boolean> attribute, final Value<Boolean> value) {
    return new DefaultCheckBoxBuilder(entityDefinition.getProperty(attribute), value);
  }

  public BooleanComboBoxBuilder booleanComboBoxBuilder(final Attribute<Boolean> attribute, final Value<Boolean> value) {
    return new DefaultBooleanComboBoxBuilder(entityDefinition.getProperty(attribute), value);
  }

  public ForeignKeyComboBoxBuilder foreignKeyComboBoxBuilder(final ForeignKey foreignKey, final Value<Entity> value,
                                                             final SwingEntityComboBoxModel comboBoxModel) {
    return new DefaultForeignKeyComboBoxBuilder(entityDefinition.getForeignKeyProperty(foreignKey), value, comboBoxModel);
  }

  public ForeignKeySearchFieldBuilder foreignKeySearchFieldBuilder(final ForeignKey foreignKey, final Value<Entity> value,
                                                                   final EntitySearchModel searchModel) {
    return new DefaultForeignKeySearchFieldBuilder(entityDefinition.getForeignKeyProperty(foreignKey), value, searchModel);
  }

  public ForeignKeyFieldBuilder foreignKeyFieldBuilder(final ForeignKey foreignKey, final Value<Entity> value) {
    return new DefaultForeignKeyFieldBuilder(entityDefinition.getForeignKeyProperty(foreignKey), value);
  }

  public <T> ValueListComboBoxBuilder<T> valueListComboBoxBuilder(final Attribute<T> attribute, final Value<T> value) {
    return new DefaultValueListComboBoxBuilder<>(entityDefinition.getProperty(attribute), value);
  }

  public <T> ComboBoxBuilder<T> comboBoxBuilder(final Attribute<T> attribute, final Value<T> value,
                                                final ComboBoxModel<T> comboBoxModel) {
    return new DefaultComboBoxBuilder<>(entityDefinition.getProperty(attribute), value, comboBoxModel);
  }

  public <T extends Temporal> TemporalInputPanelBuilder<T> temporalInputPanelBuilder(final Attribute<T> attribute, final Value<T> value) {
    return new DefaultTemporalInputPanelBuiler<>(entityDefinition.getProperty(attribute), value);
  }

  public TextInputPanelBuilder textInputPanelBuilder(final Attribute<String> attribute, final Value<String> value) {
    return new DefaultTextInputPanelBuilder(entityDefinition.getProperty(attribute), value);
  }

  public TextAreaBuilder textAreaBuilder(final Attribute<String> attribute, final Value<String> value) {
    return new DefaultTextAreaBuilder(entityDefinition.getProperty(attribute), value);
  }

  public <T> TextFieldBuilder<T> textFieldBuilder(final Attribute<T> attribute, final Value<T> value) {
    return new DefaultTextFieldBuilder<>(entityDefinition.getProperty(attribute), value);
  }

  public FormattedTextFieldBuilder formattedTextFieldBuilder(final Attribute<String> attribute, final Value<String> value) {
    return new DefaultFormattedTextFieldBuilder(entityDefinition.getProperty(attribute), value);
  }
}
