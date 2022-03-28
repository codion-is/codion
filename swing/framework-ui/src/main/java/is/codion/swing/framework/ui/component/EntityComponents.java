/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.ui.component;

import is.codion.framework.domain.entity.Attribute;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.ForeignKey;
import is.codion.framework.domain.property.ForeignKeyProperty;
import is.codion.framework.domain.property.ItemProperty;
import is.codion.framework.domain.property.Property;
import is.codion.framework.model.EntitySearchModel;
import is.codion.swing.common.model.component.combobox.ItemComboBoxModel;
import is.codion.swing.common.ui.component.BigDecimalFieldBuilder;
import is.codion.swing.common.ui.component.ButtonBuilder;
import is.codion.swing.common.ui.component.CheckBoxBuilder;
import is.codion.swing.common.ui.component.ComboBoxBuilder;
import is.codion.swing.common.ui.component.ComponentBuilder;
import is.codion.swing.common.ui.component.Components;
import is.codion.swing.common.ui.component.DoubleFieldBuilder;
import is.codion.swing.common.ui.component.FormattedTextFieldBuilder;
import is.codion.swing.common.ui.component.IntegerFieldBuilder;
import is.codion.swing.common.ui.component.ItemComboBoxBuilder;
import is.codion.swing.common.ui.component.LabelBuilder;
import is.codion.swing.common.ui.component.LongFieldBuilder;
import is.codion.swing.common.ui.component.TemporalFieldBuilder;
import is.codion.swing.common.ui.component.TemporalInputPanelBuilder;
import is.codion.swing.common.ui.component.TextAreaBuilder;
import is.codion.swing.common.ui.component.TextFieldBuilder;
import is.codion.swing.common.ui.component.TextInputPanelBuilder;
import is.codion.swing.common.ui.component.textfield.TemporalField;
import is.codion.swing.framework.model.SwingEntityComboBoxModel;
import is.codion.swing.framework.ui.EntityComboBox;
import is.codion.swing.framework.ui.EntitySearchField;

import javax.swing.ComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.temporal.Temporal;

import static java.util.Objects.requireNonNull;

/**
 * A factory for {@link is.codion.swing.common.ui.component.ComponentBuilder} instances
 * based on properties for a given entity definition.
 */
public class EntityComponents {

  private final EntityDefinition entityDefinition;

  /**
   * @param entityDefinition the entity definition
   */
  public EntityComponents(EntityDefinition entityDefinition) {
    this.entityDefinition = requireNonNull(entityDefinition);
  }

  /**
   * @return the underlying entity definition
   */
  public final EntityDefinition getEntityDefinition() {
    return entityDefinition;
  }

  /**
   * @param attribute the attribute
   * @return true if {@link #inputComponent(Attribute)} supports the given attribute
   */
  public boolean inputComponentSupported(Attribute<?> attribute) {
    requireNonNull(attribute);
    if (attribute instanceof ForeignKey) {
      return false;
    }
    Property<?> property = entityDefinition.getProperty(attribute);
    if (property instanceof ItemProperty) {
      return true;
    }

    return attribute.isLocalTime() ||
            attribute.isLocalDate() ||
            attribute.isLocalDateTime() ||
            attribute.isOffsetDateTime() ||
            attribute.isString() ||
            attribute.isCharacter() ||
            attribute.isBoolean() ||
            attribute.isInteger() ||
            attribute.isLong() ||
            attribute.isDouble() ||
            attribute.isBigDecimal();
  }

  /**
   * Creates a default input component for the given attribute.
   * @param attribute the attribute for which to create the input component
   * @param <T> the attribute type
   * @param <C> the component type
   * @param <B> the builder type
   * @return the component builder handling input for {@code attribute}
   * @throws IllegalArgumentException in case the attribute type is not supported
   */
  public <T, C extends JComponent, B extends ComponentBuilder<T, C, B>> ComponentBuilder<T, C, B> inputComponent(Attribute<T> attribute) {
    Property<T> property = entityDefinition.getProperty(attribute);
    if (property instanceof ItemProperty) {
      return (ComponentBuilder<T, C, B>) itemComboBox(attribute);
    }
    if (attribute.isLocalTime()) {
      return (ComponentBuilder<T, C, B>) localTimeField((Attribute<LocalTime>) attribute);
    }
    if (attribute.isLocalDate()) {
      return (ComponentBuilder<T, C, B>) localDateField((Attribute<LocalDate>) attribute);
    }
    if (attribute.isLocalDateTime()) {
      return (ComponentBuilder<T, C, B>) localDateTimeField((Attribute<LocalDateTime>) attribute);
    }
    if (attribute.isOffsetDateTime()) {
      return (ComponentBuilder<T, C, B>) offsetDateTimeField((Attribute<OffsetDateTime>) attribute);
    }
    if (attribute.isString() || attribute.isCharacter()) {
      return (ComponentBuilder<T, C, B>) textField(attribute);
    }
    if (attribute.isBoolean()) {
      return (ComponentBuilder<T, C, B>) checkBox((Attribute<Boolean>) attribute);
    }
    if (attribute.isInteger()) {
      return (ComponentBuilder<T, C, B>) integerField((Attribute<Integer>) attribute);
    }
    if (attribute.isLong()) {
      return (ComponentBuilder<T, C, B>) longField((Attribute<Long>) attribute);
    }
    if (attribute.isDouble()) {
      return (ComponentBuilder<T, C, B>) doubleField((Attribute<Double>) attribute);
    }
    if (attribute.isBigDecimal()) {
      return (ComponentBuilder<T, C, B>) bigDecimalField((Attribute<BigDecimal>) attribute);
    }

    throw new IllegalArgumentException("No input component available for attribute: " + attribute + " (type: " + attribute.getTypeClass() + ")");
  }

  /**
   * Creates a builder.
   * @param attribute the attribute
   * @return a builder
   */
  public final CheckBoxBuilder checkBox(Attribute<Boolean> attribute) {
    Property<Boolean> property = entityDefinition.getProperty(attribute);

    return Components.checkBox()
            .toolTipText(property.getDescription())
            .nullable(property.isNullable())
            .caption(property.getCaption())
            .includeCaption(false);
  }

  /**
   * Creates a builder.
   * @param attribute the attribute
   * @param <B> the builder type
   * @return a builder
   */
  public final <B extends ButtonBuilder<Boolean, JToggleButton, B>> ButtonBuilder<Boolean, JToggleButton, B> toggleButton(Attribute<Boolean> attribute) {
    Property<Boolean> property = entityDefinition.getProperty(attribute);

    return (ButtonBuilder<Boolean, JToggleButton, B>) Components.toggleButton()
            .toolTipText(property.getDescription())
            .caption(property.getCaption())
            .includeCaption(false);
  }

  /**
   * Creates a builder.
   * @param attribute the attribute
   * @return a builder
   */
  public final ItemComboBoxBuilder<Boolean> booleanComboBox(Attribute<Boolean> attribute) {
    Property<Boolean> property = entityDefinition.getProperty(attribute);

    return Components.booleanComboBox(ItemComboBoxModel.createBooleanModel())
            .toolTipText(property.getDescription());
  }

  /**
   * Creates a builder.
   * @param foreignKey the foreign key
   * @param comboBoxModel the combo box model
   * @param <B> the builder type
   * @return a builder
   */
  public final <B extends ComboBoxBuilder<Entity, EntityComboBox, B>> ComboBoxBuilder<Entity, EntityComboBox, B> foreignKeyComboBox(ForeignKey foreignKey,
                                                                                                                                    SwingEntityComboBoxModel comboBoxModel) {
    ForeignKeyProperty foreignKeyProperty = entityDefinition.getForeignKeyProperty(foreignKey);

    return (ComboBoxBuilder<Entity, EntityComboBox, B>) EntityComboBox.builder(comboBoxModel)
            .toolTipText(foreignKeyProperty.getDescription());
  }

  /**
   * Creates a builder.
   * @param foreignKey the foreign key
   * @param searchModel the search model
   * @return a builder
   */
  public final EntitySearchField.Builder foreignKeySearchField(ForeignKey foreignKey, EntitySearchModel searchModel) {
    ForeignKeyProperty foreignKeyProperty = entityDefinition.getForeignKeyProperty(foreignKey);

    return EntitySearchField.builder(searchModel)
            .toolTipText(foreignKeyProperty.getDescription() == null ? searchModel.getDescription() : foreignKeyProperty.getDescription());
  }

  /**
   * Creates a builder.
   * @param foreignKey the foreign key
   * @return a builder
   */
  public final LabelBuilder<Entity> foreignKeyLabel(ForeignKey foreignKey) {
    ForeignKeyProperty foreignKeyProperty = entityDefinition.getForeignKeyProperty(foreignKey);

    return Components.<Entity>label()
            .toolTipText(foreignKeyProperty.getDescription());
  }

  /**
   * Creates a builder.
   * @param attribute the attribute
   * @param <T> the attribute type
   * @return a builder
   */
  public final <T> ItemComboBoxBuilder<T> itemComboBox(Attribute<T> attribute) {
    Property<T> property = entityDefinition.getProperty(attribute);
    if (!(property instanceof ItemProperty)) {
      throw new IllegalArgumentException("Property based on '" + property.getAttribute() + "' is not a ItemProperty");
    }

    return Components.itemComboBox(((ItemProperty<T>) property).getItems())
            .toolTipText(property.getDescription())
            .nullable(property.isNullable());
  }

  /**
   * Creates a builder.
   * @param attribute the attribute
   * @param comboBoxModel the combo box model
   * @param <T> the attribute type
   * @param <C> the component type
   * @param <B> the builder type
   * @return a builder
   */
  public final <T, C extends JComboBox<T>, B extends ComboBoxBuilder<T, C, B>> ComboBoxBuilder<T, C, B> comboBox(Attribute<T> attribute,
                                                                                                                 ComboBoxModel<T> comboBoxModel) {
    Property<T> property = entityDefinition.getProperty(attribute);

    return (ComboBoxBuilder<T, C, B>) Components.comboBox(comboBoxModel)
            .toolTipText(property.getDescription());
  }

  /**
   * Creates a builder.
   * @param attribute the attribute
   * @param <T> the attribute type
   * @return a builder
   */
  public final <T extends Temporal> TemporalInputPanelBuilder<T> temporalInputPanel(Attribute<T> attribute) {
    if (!attribute.isTemporal()) {
      throw new IllegalArgumentException("Attribute " + attribute + " is not Temporal");
    }
    Property<T> property = entityDefinition.getProperty(attribute);

    return Components.temporalInputPanel(attribute.getTypeClass(), property.getDateTimePattern())
            .toolTipText(property.getDescription());
  }

  /**
   * Creates a builder.
   * @param attribute the attribute
   * @return a builder
   */
  public final TextInputPanelBuilder textInputPanel(Attribute<String> attribute) {
    Property<String> property = entityDefinition.getProperty(attribute);

    return Components.textInputPanel()
            .toolTipText(property.getDescription())
            .maximumLength(property.getMaximumLength())
            .dialogTitle(property.getCaption());
  }

  /**
   * Creates a builder.
   * @param attribute the attribute
   * @return a builder
   */
  public final TextAreaBuilder textArea(Attribute<String> attribute) {
    Property<String> property = entityDefinition.getProperty(attribute);
    if (!attribute.isString()) {
      throw new IllegalArgumentException("Cannot create a text area for a non-string attribute");
    }

    return Components.textArea()
            .toolTipText(property.getDescription())
            .maximumLength(property.getMaximumLength());
  }

  /**
   * Creates a builder.
   * @param attribute the attribute
   * @param <T> the attribute type
   * @param <C> the text field type
   * @param <B> the builder type
   * @return a builder
   */
  public final <T, C extends JTextField, B extends TextFieldBuilder<T, C, B>> TextFieldBuilder<T, C, B> textField(Attribute<T> attribute) {
    Property<T> property = entityDefinition.getProperty(attribute);

    Class<T> typeClass = attribute.getTypeClass();
    if (typeClass.equals(LocalTime.class)) {
      return (TextFieldBuilder<T, C, B>) localTimeField((Attribute<LocalTime>) attribute)
              .toolTipText(property.getDescription());
    }
    else if (typeClass.equals(LocalDate.class)) {
      return (TextFieldBuilder<T, C, B>) localDateField((Attribute<LocalDate>) attribute)
              .toolTipText(property.getDescription());
    }
    else if (typeClass.equals(LocalDateTime.class)) {
      return (TextFieldBuilder<T, C, B>) localDateTimeField((Attribute<LocalDateTime>) attribute)
              .toolTipText(property.getDescription());
    }
    else if (typeClass.equals(OffsetDateTime.class)) {
      return (TextFieldBuilder<T, C, B>) offsetDateTimeField((Attribute<OffsetDateTime>) attribute)
              .toolTipText(property.getDescription());
    }

    return (TextFieldBuilder<T, C, B>) Components.textField(typeClass)
            .format(property.getFormat())
            .maximumLength(property.getMaximumLength())
            .toolTipText(property.getDescription());
  }

  /**
   * Creates a builder.
   * @param attribute the attribute
   * @return a builder
   */
  public final TemporalFieldBuilder<LocalTime, TemporalField<LocalTime>> localTimeField(Attribute<LocalTime> attribute) {
    Property<LocalTime> property = entityDefinition.getProperty(attribute);

    return Components.localTimeField(property.getDateTimePattern())
            .toolTipText(property.getDescription());
  }

  /**
   * Creates a builder.
   * @param attribute the attribute
   * @return a builder
   */
  public final TemporalFieldBuilder<LocalDate, TemporalField<LocalDate>> localDateField(Attribute<LocalDate> attribute) {
    Property<LocalDate> property = entityDefinition.getProperty(attribute);

    return Components.localDateField(property.getDateTimePattern())
            .toolTipText(property.getDescription());
  }

  /**
   * Creates a builder.
   * @param attribute the attribute
   * @return a builder
   */
  public final TemporalFieldBuilder<LocalDateTime, TemporalField<LocalDateTime>> localDateTimeField(Attribute<LocalDateTime> attribute) {
    Property<LocalDateTime> property = entityDefinition.getProperty(attribute);

    return Components.localDateTimeField(property.getDateTimePattern())
            .toolTipText(property.getDescription());
  }

  /**
   * Creates a builder.
   * @param attribute the attribute
   * @return a builder
   */
  public final TemporalFieldBuilder<OffsetDateTime, TemporalField<OffsetDateTime>> offsetDateTimeField(Attribute<OffsetDateTime> attribute) {
    Property<OffsetDateTime> property = entityDefinition.getProperty(attribute);

    return Components.offsetDateTimeField(property.getDateTimePattern())
            .toolTipText(property.getDescription());
  }

  /**
   * Creates a builder.
   * @param attribute the attribute
   * @return a builder
   */
  public final IntegerFieldBuilder integerField(Attribute<Integer> attribute) {
    Property<Integer> property = entityDefinition.getProperty(attribute);

    return Components.integerField()
            .format(property.getFormat())
            .minimumValue(property.getMinimumValue())
            .maximumValue(property.getMaximumValue())
            .toolTipText(property.getDescription());
  }

  /**
   * Creates a builder.
   * @param attribute the attribute
   * @return a builder
   */
  public final LongFieldBuilder longField(Attribute<Long> attribute) {
    Property<Long> property = entityDefinition.getProperty(attribute);

    return Components.longField()
            .format(property.getFormat())
            .minimumValue(property.getMinimumValue())
            .maximumValue(property.getMaximumValue())
            .toolTipText(property.getDescription());
  }

  /**
   * Creates a builder.
   * @param attribute the attribute
   * @return a builder
   */
  public final DoubleFieldBuilder doubleField(Attribute<Double> attribute) {
    Property<Double> property = entityDefinition.getProperty(attribute);

    return Components.doubleField()
            .format(property.getFormat())
            .minimumValue(property.getMinimumValue())
            .maximumValue(property.getMaximumValue())
            .maximumFractionDigits(property.getMaximumFractionDigits())
            .toolTipText(property.getDescription());
  }

  /**
   * Creates a builder.
   * @param attribute the attribute
   * @return a builder
   */
  public final BigDecimalFieldBuilder bigDecimalField(Attribute<BigDecimal> attribute) {
    Property<BigDecimal> property = entityDefinition.getProperty(attribute);

    return Components.bigDecimalField()
            .format(property.getFormat())
            .minimumValue(property.getMinimumValue())
            .maximumValue(property.getMaximumValue())
            .maximumFractionDigits(property.getMaximumFractionDigits())
            .toolTipText(property.getDescription());
  }

  /**
   * Creates a builder.
   * @param attribute the attribute
   * @return a builder
   */
  public final FormattedTextFieldBuilder formattedTextField(Attribute<String> attribute) {
    Property<String> property = entityDefinition.getProperty(attribute);

    return Components.formattedTextField()
            .toolTipText(property.getDescription());
  }
}
