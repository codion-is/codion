/*
 * Copyright (c) 2008 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
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
import is.codion.swing.common.ui.component.ComponentBuilder;
import is.codion.swing.common.ui.component.Components;
import is.codion.swing.common.ui.component.LabelBuilder;
import is.codion.swing.common.ui.component.button.ButtonBuilder;
import is.codion.swing.common.ui.component.button.CheckBoxBuilder;
import is.codion.swing.common.ui.component.combobox.ComboBoxBuilder;
import is.codion.swing.common.ui.component.combobox.ItemComboBoxBuilder;
import is.codion.swing.common.ui.component.text.MaskedTextFieldBuilder;
import is.codion.swing.common.ui.component.text.NumberField;
import is.codion.swing.common.ui.component.text.TemporalField;
import is.codion.swing.common.ui.component.text.TemporalInputPanel;
import is.codion.swing.common.ui.component.text.TextAreaBuilder;
import is.codion.swing.common.ui.component.text.TextFieldBuilder;
import is.codion.swing.common.ui.component.text.TextInputPanel;
import is.codion.swing.framework.model.SwingEntityComboBoxModel;
import is.codion.swing.framework.ui.EntityComboBox;
import is.codion.swing.framework.ui.EntitySearchField;

import javax.swing.ComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import java.math.BigDecimal;
import java.text.FieldPosition;
import java.text.Format;
import java.text.ParsePosition;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.temporal.Temporal;

import static java.util.Objects.requireNonNull;

/**
 * A factory for {@link is.codion.swing.common.ui.component.ComponentBuilder} instances
 * based on attributes from a given entity definition.
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
   * @return true if {@link #component(Attribute)} supports the given attribute
   */
  public boolean supports(Attribute<?> attribute) {
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
  public <T, C extends JComponent, B extends ComponentBuilder<T, C, B>> ComponentBuilder<T, C, B> component(Attribute<T> attribute) {
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
   * Creates a CheckBox builder based on the given attribute.
   * @param attribute the attribute
   * @return a CheckBox builder
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
   * Creates a ToggleButton builder based on the given attribute.
   * @param attribute the attribute
   * @param <B> the builder type
   * @return a ToggleButton builder
   */
  public final <B extends ButtonBuilder<Boolean, JToggleButton, B>> ButtonBuilder<Boolean, JToggleButton, B> toggleButton(Attribute<Boolean> attribute) {
    Property<Boolean> property = entityDefinition.getProperty(attribute);

    return (ButtonBuilder<Boolean, JToggleButton, B>) Components.toggleButton()
            .toolTipText(property.getDescription())
            .caption(property.getCaption())
            .includeCaption(false);
  }

  /**
   * Creates a boolean ComboBox builder based on the given attribute.
   * @param attribute the attribute
   * @return a boolean ComboBox builder
   */
  public final ItemComboBoxBuilder<Boolean> booleanComboBox(Attribute<Boolean> attribute) {
    Property<Boolean> property = entityDefinition.getProperty(attribute);

    return Components.booleanComboBox(ItemComboBoxModel.createBooleanModel())
            .toolTipText(property.getDescription());
  }

  /**
   * Creates a foreign key ComboBox builder based on the given foreign key.
   * @param foreignKey the foreign key
   * @param comboBoxModel the combo box model
   * @param <B> the builder type
   * @return a foreign key ComboBox builder
   */
  public final <B extends ComboBoxBuilder<Entity, EntityComboBox, B>> ComboBoxBuilder<Entity, EntityComboBox, B> foreignKeyComboBox(ForeignKey foreignKey,
                                                                                                                                    SwingEntityComboBoxModel comboBoxModel) {
    ForeignKeyProperty foreignKeyProperty = entityDefinition.getForeignKeyProperty(foreignKey);

    return (ComboBoxBuilder<Entity, EntityComboBox, B>) EntityComboBox.builder(comboBoxModel)
            .toolTipText(foreignKeyProperty.getDescription());
  }

  /**
   * Creates a foreign key search field builder based on the given foreign key.
   * @param foreignKey the foreign key
   * @param searchModel the search model
   * @return a foreign key search field builder
   */
  public final EntitySearchField.Builder foreignKeySearchField(ForeignKey foreignKey, EntitySearchModel searchModel) {
    ForeignKeyProperty foreignKeyProperty = entityDefinition.getForeignKeyProperty(foreignKey);

    return EntitySearchField.builder(searchModel)
            .toolTipText(foreignKeyProperty.getDescription() == null ? searchModel.getDescription() : foreignKeyProperty.getDescription());
  }

  /**
   * Creates foreign key text field builder for the given foreign key, read-only and non-focusable.
   * @param foreignKey the foreign key
   * @param <B> the builder type
   * @return a foreign key text field builder
   */
  public final <B extends TextFieldBuilder<Entity, JTextField, B>> TextFieldBuilder<Entity, JTextField, B> foreignKeyTextField(ForeignKey foreignKey) {
    ForeignKeyProperty foreignKeyProperty = entityDefinition.getForeignKeyProperty(foreignKey);

    return (TextFieldBuilder<Entity, JTextField, B>) Components.textField(Entity.class)
            .toolTipText(foreignKeyProperty.getDescription())
            .format(new EntityReadOnlyFormat())
            .editable(false)
            .focusable(false);
  }

  /**
   * Creates a foreign key label builder based on the given foreign key.
   * @param foreignKey the foreign key
   * @return a foreign key label builder
   */
  public final LabelBuilder<Entity> foreignKeyLabel(ForeignKey foreignKey) {
    ForeignKeyProperty foreignKeyProperty = entityDefinition.getForeignKeyProperty(foreignKey);

    return Components.<Entity>label()
            .toolTipText(foreignKeyProperty.getDescription());
  }

  /**
   * Creates a ComboBox builder based on the given attribute.
   * Note that the attribute must be associated with a {@link ItemProperty}.
   * @param attribute the attribute
   * @param <T> the attribute type
   * @return an item ComboBox builder
   * @throws IllegalArgumentException in case the given attribute is not associated with a {@link ItemProperty}
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
   * Creates a ComboBox builder based on the given attribute.
   * @param attribute the attribute
   * @param comboBoxModel the combo box model
   * @param <T> the attribute type
   * @param <C> the component type
   * @param <B> the builder type
   * @return a ComboBox builder
   */
  public final <T, C extends JComboBox<T>, B extends ComboBoxBuilder<T, C, B>> ComboBoxBuilder<T, C, B> comboBox(Attribute<T> attribute,
                                                                                                                 ComboBoxModel<T> comboBoxModel) {
    Property<T> property = entityDefinition.getProperty(attribute);

    return (ComboBoxBuilder<T, C, B>) Components.comboBox(comboBoxModel)
            .toolTipText(property.getDescription());
  }

  /**
   * Creates a {@link TemporalInputPanel} builder based on the given attribute.
   * @param attribute the attribute
   * @param <T> the attribute type
   * @return a {@link TemporalInputPanel} builder
   */
  public final <T extends Temporal> TemporalInputPanel.Builder<T> temporalInputPanel(Attribute<T> attribute) {
    return temporalInputPanel(attribute, entityDefinition.getProperty(attribute).getDateTimePattern());
  }

  /**
   * Creates a {@link TemporalInputPanel} builder based on the given attribute.
   * @param attribute the attribute
   * @param dateTimePattern the date time pattern
   * @param <T> the attribute type
   * @return a {@link TemporalInputPanel} builder
   */
  public final <T extends Temporal> TemporalInputPanel.Builder<T> temporalInputPanel(Attribute<T> attribute, String dateTimePattern) {
    return Components.temporalInputPanel(attribute.getTypeClass(), dateTimePattern)
            .toolTipText(entityDefinition.getProperty(attribute).getDescription());
  }

  /**
   * Creates a {@link TextInputPanel} builder based on the given attribute.
   * @param attribute the attribute
   * @return a {@link TextInputPanel} builder
   */
  public final TextInputPanel.Builder textInputPanel(Attribute<String> attribute) {
    Property<String> property = entityDefinition.getProperty(attribute);

    return Components.textInputPanel()
            .toolTipText(property.getDescription())
            .maximumLength(property.getMaximumLength())
            .dialogTitle(property.getCaption());
  }

  /**
   * Creates a TextArea builder based on the given attribute.
   * @param attribute the attribute
   * @return a TextArea builder
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
   * Creates a text field builder based on the given attribute.
   * @param attribute the attribute
   * @param <T> the attribute type
   * @param <C> the text field type
   * @param <B> the builder type
   * @return a text field builder
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
   * Creates a {@link TemporalField} builder based on the given attribute.
   * @param attribute the attribute
   * @return a {@link TemporalField} builder
   */
  public final TemporalField.Builder<LocalTime> localTimeField(Attribute<LocalTime> attribute) {
    return localTimeField(attribute, entityDefinition.getProperty(attribute).getDateTimePattern());
  }

  /**
   * Creates a {@link TemporalField} builder based on the given attribute.
   * @param attribute the attribute
   * @param dateTimePattern the date time pattern
   * @return a {@link TemporalField} builder
   */
  public final TemporalField.Builder<LocalTime> localTimeField(Attribute<LocalTime> attribute, String dateTimePattern) {
    return Components.localTimeField(dateTimePattern)
            .toolTipText(entityDefinition.getProperty(attribute).getDescription());
  }

  /**
   * Creates a {@link TemporalField} builder based on the given attribute.
   * @param attribute the attribute
   * @return a {@link TemporalField} builder
   */
  public final TemporalField.Builder<LocalDate> localDateField(Attribute<LocalDate> attribute) {
    return localDateField(attribute, entityDefinition.getProperty(attribute).getDateTimePattern());
  }

  /**
   * Creates a {@link TemporalField} builder based on the given attribute.
   * @param attribute the attribute
   * @param dateTimePattern the date time pattern
   * @return a {@link TemporalField} builder
   */
  public final TemporalField.Builder<LocalDate> localDateField(Attribute<LocalDate> attribute, String dateTimePattern) {
    return Components.localDateField(dateTimePattern)
            .toolTipText(entityDefinition.getProperty(attribute).getDescription());
  }

  /**
   * Creates a {@link TemporalField} builder based on the given attribute.
   * @param attribute the attribute
   * @return a {@link TemporalField} builder
   */
  public final TemporalField.Builder<LocalDateTime> localDateTimeField(Attribute<LocalDateTime> attribute) {
    return localDateTimeField(attribute, entityDefinition.getProperty(attribute).getDateTimePattern());
  }

  /**
   * Creates a {@link TemporalField} builder based on the given attribute.
   * @param attribute the attribute
   * @param dateTimePattern the date time pattern
   * @return a {@link TemporalField} builder
   */
  public final TemporalField.Builder<LocalDateTime> localDateTimeField(Attribute<LocalDateTime> attribute, String dateTimePattern) {
    return Components.localDateTimeField(dateTimePattern)
            .toolTipText(entityDefinition.getProperty(attribute).getDescription());
  }

  /**
   * Creates a {@link TemporalField} builder based on the given attribute.
   * @param attribute the attribute
   * @return a {@link TemporalField} builder
   */
  public final TemporalField.Builder<OffsetDateTime> offsetDateTimeField(Attribute<OffsetDateTime> attribute) {
    return offsetDateTimeField(attribute, entityDefinition.getProperty(attribute).getDateTimePattern());
  }

  /**
   * Creates a {@link TemporalField} builder based on the given attribute.
   * @param attribute the attribute
   * @param dateTimePattern the date time pattern
   * @return a {@link TemporalField} builder
   */
  public final TemporalField.Builder<OffsetDateTime> offsetDateTimeField(Attribute<OffsetDateTime> attribute, String dateTimePattern) {
    return Components.offsetDateTimeField(dateTimePattern)
            .toolTipText(entityDefinition.getProperty(attribute).getDescription());
  }

  /**
   * Creates a {@link TemporalField} builder based on the given attribute.
   * @param attribute the attribute
   * @param <T> the temporal type
   * @return a {@link TemporalField} builder
   */
  public final <T extends Temporal> TemporalField.Builder<T> temporalField(Attribute<T> attribute) {
    return temporalField(attribute, entityDefinition.getProperty(attribute).getDateTimePattern());
  }

  /**
   * Creates a {@link TemporalField} builder based on the given attribute.
   * @param attribute the attribute
   * @param dateTimePattern the date time pattern
   * @param <T> the temporal type
   * @return a {@link TemporalField} builder
   */
  public final <T extends Temporal> TemporalField.Builder<T> temporalField(Attribute<T> attribute, String dateTimePattern) {
    Property<T> property = entityDefinition.getProperty(attribute);

    return Components.temporalField(property.getAttribute().getTypeClass(), dateTimePattern)
            .toolTipText(property.getDescription());
  }

  /**
   * Creates {@link NumberField} builder based on the given attribute.
   * @param attribute the attribute
   * @return a {@link NumberField} builder
   */
  public final NumberField.Builder<Integer> integerField(Attribute<Integer> attribute) {
    Property<Integer> property = entityDefinition.getProperty(attribute);

    return Components.integerField()
            .format(property.getFormat())
            .minimumValue(property.getMinimumValue())
            .maximumValue(property.getMaximumValue())
            .toolTipText(property.getDescription());
  }

  /**
   * Creates {@link NumberField} builder based on the given attribute.
   * @param attribute the attribute
   * @return a {@link NumberField} builder
   */
  public final NumberField.Builder<Long> longField(Attribute<Long> attribute) {
    Property<Long> property = entityDefinition.getProperty(attribute);

    return Components.longField()
            .format(property.getFormat())
            .minimumValue(property.getMinimumValue())
            .maximumValue(property.getMaximumValue())
            .toolTipText(property.getDescription());
  }

  /**
   * Creates {@link NumberField} builder based on the given attribute.
   * @param attribute the attribute
   * @return a {@link NumberField} builder
   */
  public final NumberField.Builder<Double> doubleField(Attribute<Double> attribute) {
    Property<Double> property = entityDefinition.getProperty(attribute);

    return Components.doubleField()
            .format(property.getFormat())
            .minimumValue(property.getMinimumValue())
            .maximumValue(property.getMaximumValue())
            .maximumFractionDigits(property.getMaximumFractionDigits())
            .toolTipText(property.getDescription());
  }

  /**
   * Creates {@link NumberField} builder based on the given attribute.
   * @param attribute the attribute
   * @return a {@link NumberField} builder
   */
  public final NumberField.Builder<BigDecimal> bigDecimalField(Attribute<BigDecimal> attribute) {
    Property<BigDecimal> property = entityDefinition.getProperty(attribute);

    return Components.bigDecimalField()
            .format(property.getFormat())
            .minimumValue(property.getMinimumValue())
            .maximumValue(property.getMaximumValue())
            .maximumFractionDigits(property.getMaximumFractionDigits())
            .toolTipText(property.getDescription());
  }

  /**
   * Creates masked text field builder based on the given attribute.
   * @param attribute the attribute
   * @return a masked text field builder
   */
  public final MaskedTextFieldBuilder maskedTextField(Attribute<String> attribute) {
    Property<String> property = entityDefinition.getProperty(attribute);

    return Components.maskedTextField()
            .toolTipText(property.getDescription());
  }

  private static final class EntityReadOnlyFormat extends Format {

    @Override
    public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
      toAppendTo.append(obj == null ? "" : obj.toString());
      return toAppendTo;
    }

    @Override
    public Object parseObject(String source, ParsePosition pos) {
      return null;
    }
  }
}
