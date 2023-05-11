/*
 * Copyright (c) 2008 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
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
import is.codion.swing.common.ui.component.ComponentBuilder;
import is.codion.swing.common.ui.component.Components;
import is.codion.swing.common.ui.component.button.ButtonBuilder;
import is.codion.swing.common.ui.component.button.CheckBoxBuilder;
import is.codion.swing.common.ui.component.combobox.ComboBoxBuilder;
import is.codion.swing.common.ui.component.combobox.ItemComboBoxBuilder;
import is.codion.swing.common.ui.component.label.LabelBuilder;
import is.codion.swing.common.ui.component.text.MaskedTextFieldBuilder;
import is.codion.swing.common.ui.component.text.NumberField;
import is.codion.swing.common.ui.component.text.TemporalField;
import is.codion.swing.common.ui.component.text.TemporalInputPanel;
import is.codion.swing.common.ui.component.text.TextAreaBuilder;
import is.codion.swing.common.ui.component.text.TextFieldBuilder;
import is.codion.swing.common.ui.component.text.TextInputPanel;
import is.codion.swing.framework.model.EntityComboBoxModel;
import is.codion.swing.framework.ui.EntityComboBox;
import is.codion.swing.framework.ui.EntitySearchField;
import is.codion.swing.framework.ui.icons.FrameworkIcons;

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

import static is.codion.swing.common.model.component.combobox.ItemComboBoxModel.booleanItemComboBoxModel;
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
  public final EntityDefinition entityDefinition() {
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
    Property<?> property = entityDefinition.property(attribute);
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
            attribute.isShort() ||
            attribute.isInteger() ||
            attribute.isLong() ||
            attribute.isDouble() ||
            attribute.isBigDecimal();
  }

  /**
   * Returns a {@link ComponentBuilder} instance for a default input component for the given attribute.
   * @param attribute the attribute for which to create the input component
   * @param <T> the attribute type
   * @param <C> the component type
   * @param <B> the builder type
   * @return the component builder handling input for {@code attribute}
   * @throws IllegalArgumentException in case the attribute type is not supported
   */
  public <T, C extends JComponent, B extends ComponentBuilder<T, C, B>> ComponentBuilder<T, C, B> component(Attribute<T> attribute) {
    Property<T> property = entityDefinition.property(attribute);
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
    if (attribute.isShort()) {
      return (ComponentBuilder<T, C, B>) shortField((Attribute<Short>) attribute);
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

    throw new IllegalArgumentException("No input component available for attribute: " + attribute + " (type: " + attribute.valueClass() + ")");
  }

  /**
   * Creates a CheckBox builder based on the given attribute.
   * @param attribute the attribute
   * @return a JCheckBox builder
   */
  public final CheckBoxBuilder checkBox(Attribute<Boolean> attribute) {
    Property<Boolean> property = entityDefinition.property(attribute);

    return Components.checkBox()
            .toolTipText(property.description())
            .nullable(property.isNullable())
            .caption(property.caption())
            .includeCaption(false);
  }

  /**
   * Creates a ToggleButton builder based on the given attribute.
   * @param attribute the attribute
   * @param <B> the builder type
   * @return a JToggleButton builder
   */
  public final <B extends ButtonBuilder<Boolean, JToggleButton, B>> ButtonBuilder<Boolean, JToggleButton, B> toggleButton(Attribute<Boolean> attribute) {
    Property<Boolean> property = entityDefinition.property(attribute);

    return (ButtonBuilder<Boolean, JToggleButton, B>) Components.toggleButton()
            .toolTipText(property.description())
            .caption(property.caption())
            .includeCaption(false);
  }

  /**
   * Creates a boolean ComboBox builder based on the given attribute.
   * @param attribute the attribute
   * @return a boolean JComboBox builder
   */
  public final ItemComboBoxBuilder<Boolean> booleanComboBox(Attribute<Boolean> attribute) {
    Property<Boolean> property = entityDefinition.property(attribute);

    return Components.booleanComboBox(booleanItemComboBoxModel())
            .toolTipText(property.description());
  }

  /**
   * Creates a foreign key ComboBox builder based on the given foreign key.
   * @param foreignKey the foreign key
   * @param comboBoxModel the combo box model
   * @param <B> the builder type
   * @return a foreign key JComboBox builder
   */
  public final <B extends ComboBoxBuilder<Entity, EntityComboBox, B>> ComboBoxBuilder<Entity, EntityComboBox, B> foreignKeyComboBox(ForeignKey foreignKey,
                                                                                                                                    EntityComboBoxModel comboBoxModel) {
    ForeignKeyProperty foreignKeyProperty = entityDefinition.foreignKeyProperty(foreignKey);

    return (ComboBoxBuilder<Entity, EntityComboBox, B>) EntityComboBox.builder(comboBoxModel)
            .toolTipText(foreignKeyProperty.description());
  }

  /**
   * Creates a foreign key search field builder based on the given foreign key.
   * @param foreignKey the foreign key
   * @param searchModel the search model
   * @return a foreign key {@link EntitySearchField} builder
   */
  public final EntitySearchField.Builder foreignKeySearchField(ForeignKey foreignKey, EntitySearchModel searchModel) {
    ForeignKeyProperty foreignKeyProperty = entityDefinition.foreignKeyProperty(foreignKey);

    return EntitySearchField.builder(searchModel)
            .toolTipText(foreignKeyProperty.description() == null ? searchModel.getDescription() : foreignKeyProperty.description());
  }

  /**
   * Creates foreign key text field builder for the given foreign key, read-only and non-focusable.
   * @param foreignKey the foreign key
   * @param <B> the builder type
   * @return a foreign key JTextField builder
   */
  public final <B extends TextFieldBuilder<Entity, JTextField, B>> TextFieldBuilder<Entity, JTextField, B> foreignKeyTextField(ForeignKey foreignKey) {
    ForeignKeyProperty foreignKeyProperty = entityDefinition.foreignKeyProperty(foreignKey);

    return (TextFieldBuilder<Entity, JTextField, B>) Components.textField(Entity.class)
            .toolTipText(foreignKeyProperty.description())
            .format(new EntityReadOnlyFormat())
            .editable(false)
            .focusable(false);
  }

  /**
   * Creates a foreign key label builder based on the given foreign key.
   * @param foreignKey the foreign key
   * @return a foreign key JLabel builder
   */
  public final LabelBuilder<Entity> foreignKeyLabel(ForeignKey foreignKey) {
    ForeignKeyProperty foreignKeyProperty = entityDefinition.foreignKeyProperty(foreignKey);

    return Components.<Entity>label()
            .toolTipText(foreignKeyProperty.description());
  }

  /**
   * Creates a JComboBox builder based on the given attribute.
   * Note that the attribute must be associated with a {@link ItemProperty}.
   * @param attribute the attribute
   * @param <T> the attribute type
   * @return an {@link is.codion.common.item.Item} based JComboBox builder
   * @throws IllegalArgumentException in case the given attribute is not associated with a {@link ItemProperty}
   */
  public final <T> ItemComboBoxBuilder<T> itemComboBox(Attribute<T> attribute) {
    Property<T> property = entityDefinition.property(attribute);
    if (!(property instanceof ItemProperty)) {
      throw new IllegalArgumentException("Property based on '" + property.attribute() + "' is not a ItemProperty");
    }

    return Components.itemComboBox(((ItemProperty<T>) property).items())
            .toolTipText(property.description())
            .nullable(property.isNullable());
  }

  /**
   * Creates a JComboBox builder based on the given attribute.
   * @param attribute the attribute
   * @param comboBoxModel the combo box model
   * @param <T> the attribute type
   * @param <C> the component type
   * @param <B> the builder type
   * @return a JComboBox builder
   */
  public final <T, C extends JComboBox<T>, B extends ComboBoxBuilder<T, C, B>> ComboBoxBuilder<T, C, B> comboBox(Attribute<T> attribute,
                                                                                                                 ComboBoxModel<T> comboBoxModel) {
    Property<T> property = entityDefinition.property(attribute);

    return (ComboBoxBuilder<T, C, B>) Components.comboBox(comboBoxModel)
            .toolTipText(property.description());
  }

  /**
   * Creates a {@link TemporalInputPanel} builder based on the given attribute.
   * @param attribute the attribute
   * @param <T> the attribute type
   * @return a {@link TemporalInputPanel} builder
   */
  public final <T extends Temporal> TemporalInputPanel.Builder<T> temporalInputPanel(Attribute<T> attribute) {
    return temporalInputPanel(attribute, entityDefinition.property(attribute).dateTimePattern());
  }

  /**
   * Creates a {@link TemporalInputPanel} builder based on the given attribute.
   * @param attribute the attribute
   * @param dateTimePattern the date time pattern
   * @param <T> the attribute type
   * @return a {@link TemporalInputPanel} builder
   */
  public final <T extends Temporal> TemporalInputPanel.Builder<T> temporalInputPanel(Attribute<T> attribute, String dateTimePattern) {
    return Components.temporalInputPanel(attribute.valueClass(), dateTimePattern)
            .toolTipText(entityDefinition.property(attribute).description())
            .buttonIcon(FrameworkIcons.instance().calendar());
  }

  /**
   * Creates a {@link TextInputPanel} builder based on the given attribute.
   * @param attribute the attribute
   * @return a {@link TextInputPanel} builder
   */
  public final TextInputPanel.Builder textInputPanel(Attribute<String> attribute) {
    Property<String> property = entityDefinition.property(attribute);

    return Components.textInputPanel()
            .toolTipText(property.description())
            .maximumLength(property.maximumLength())
            .dialogTitle(property.caption());
  }

  /**
   * Creates a TextArea builder based on the given attribute.
   * @param attribute the attribute
   * @return a JTextArea builder
   */
  public final TextAreaBuilder textArea(Attribute<String> attribute) {
    Property<String> property = entityDefinition.property(attribute);
    if (!attribute.isString()) {
      throw new IllegalArgumentException("Cannot create a text area for a non-string attribute");
    }

    return Components.textArea()
            .toolTipText(property.description())
            .maximumLength(property.maximumLength());
  }

  /**
   * Creates a text field builder based on the given attribute.
   * @param attribute the attribute
   * @param <T> the attribute type
   * @param <C> the text field type
   * @param <B> the builder type
   * @return a JTextField builder
   */
  public final <T, C extends JTextField, B extends TextFieldBuilder<T, C, B>> TextFieldBuilder<T, C, B> textField(Attribute<T> attribute) {
    Property<T> property = entityDefinition.property(attribute);

    Class<T> valueClass = attribute.valueClass();
    if (valueClass.equals(LocalTime.class)) {
      return (TextFieldBuilder<T, C, B>) localTimeField((Attribute<LocalTime>) attribute)
              .toolTipText(property.description());
    }
    else if (valueClass.equals(LocalDate.class)) {
      return (TextFieldBuilder<T, C, B>) localDateField((Attribute<LocalDate>) attribute)
              .toolTipText(property.description());
    }
    else if (valueClass.equals(LocalDateTime.class)) {
      return (TextFieldBuilder<T, C, B>) localDateTimeField((Attribute<LocalDateTime>) attribute)
              .toolTipText(property.description());
    }
    else if (valueClass.equals(OffsetDateTime.class)) {
      return (TextFieldBuilder<T, C, B>) offsetDateTimeField((Attribute<OffsetDateTime>) attribute)
              .toolTipText(property.description());
    }

    return (TextFieldBuilder<T, C, B>) Components.textField(valueClass)
            .format(property.format())
            .maximumLength(property.maximumLength())
            .toolTipText(property.description());
  }

  /**
   * Creates a {@link TemporalField} builder based on the given attribute.
   * @param attribute the attribute
   * @return a {@link TemporalField} builder
   */
  public final TemporalField.Builder<LocalTime> localTimeField(Attribute<LocalTime> attribute) {
    return localTimeField(attribute, entityDefinition.property(attribute).dateTimePattern());
  }

  /**
   * Creates a {@link TemporalField} builder based on the given attribute.
   * @param attribute the attribute
   * @param dateTimePattern the date time pattern
   * @return a {@link TemporalField} builder
   */
  public final TemporalField.Builder<LocalTime> localTimeField(Attribute<LocalTime> attribute, String dateTimePattern) {
    return Components.localTimeField(dateTimePattern)
            .toolTipText(entityDefinition.property(attribute).description());
  }

  /**
   * Creates a {@link TemporalField} builder based on the given attribute.
   * @param attribute the attribute
   * @return a {@link TemporalField} builder
   */
  public final TemporalField.Builder<LocalDate> localDateField(Attribute<LocalDate> attribute) {
    return localDateField(attribute, entityDefinition.property(attribute).dateTimePattern());
  }

  /**
   * Creates a {@link TemporalField} builder based on the given attribute.
   * @param attribute the attribute
   * @param dateTimePattern the date time pattern
   * @return a {@link TemporalField} builder
   */
  public final TemporalField.Builder<LocalDate> localDateField(Attribute<LocalDate> attribute, String dateTimePattern) {
    return Components.localDateField(dateTimePattern)
            .toolTipText(entityDefinition.property(attribute).description());
  }

  /**
   * Creates a {@link TemporalField} builder based on the given attribute.
   * @param attribute the attribute
   * @return a {@link TemporalField} builder
   */
  public final TemporalField.Builder<LocalDateTime> localDateTimeField(Attribute<LocalDateTime> attribute) {
    return localDateTimeField(attribute, entityDefinition.property(attribute).dateTimePattern());
  }

  /**
   * Creates a {@link TemporalField} builder based on the given attribute.
   * @param attribute the attribute
   * @param dateTimePattern the date time pattern
   * @return a {@link TemporalField} builder
   */
  public final TemporalField.Builder<LocalDateTime> localDateTimeField(Attribute<LocalDateTime> attribute, String dateTimePattern) {
    return Components.localDateTimeField(dateTimePattern)
            .toolTipText(entityDefinition.property(attribute).description());
  }

  /**
   * Creates a {@link TemporalField} builder based on the given attribute.
   * @param attribute the attribute
   * @return a {@link TemporalField} builder
   */
  public final TemporalField.Builder<OffsetDateTime> offsetDateTimeField(Attribute<OffsetDateTime> attribute) {
    return offsetDateTimeField(attribute, entityDefinition.property(attribute).dateTimePattern());
  }

  /**
   * Creates a {@link TemporalField} builder based on the given attribute.
   * @param attribute the attribute
   * @param dateTimePattern the date time pattern
   * @return a {@link TemporalField} builder
   */
  public final TemporalField.Builder<OffsetDateTime> offsetDateTimeField(Attribute<OffsetDateTime> attribute, String dateTimePattern) {
    return Components.offsetDateTimeField(dateTimePattern)
            .toolTipText(entityDefinition.property(attribute).description());
  }

  /**
   * Creates a {@link TemporalField} builder based on the given attribute.
   * @param attribute the attribute
   * @param <T> the temporal type
   * @return a {@link TemporalField} builder
   */
  public final <T extends Temporal> TemporalField.Builder<T> temporalField(Attribute<T> attribute) {
    return temporalField(attribute, entityDefinition.property(attribute).dateTimePattern());
  }

  /**
   * Creates a {@link TemporalField} builder based on the given attribute.
   * @param attribute the attribute
   * @param dateTimePattern the date time pattern
   * @param <T> the temporal type
   * @return a {@link TemporalField} builder
   */
  public final <T extends Temporal> TemporalField.Builder<T> temporalField(Attribute<T> attribute, String dateTimePattern) {
    Property<T> property = entityDefinition.property(attribute);

    return Components.temporalField(property.attribute().valueClass(), dateTimePattern)
            .toolTipText(property.description());
  }

  /**
   * Creates a {@link NumberField} builder based on the given attribute.
   * @param attribute the attribute
   * @return a {@link NumberField} builder
   */
  public final NumberField.Builder<Short> shortField(Attribute<Short> attribute) {
    Property<Short> property = entityDefinition.property(attribute);

    return Components.shortField()
            .format(property.format())
            .minimumValue(property.minimumValue())
            .maximumValue(property.maximumValue())
            .toolTipText(property.description());
  }

  /**
   * Creates a {@link NumberField} builder based on the given attribute.
   * @param attribute the attribute
   * @return a {@link NumberField} builder
   */
  public final NumberField.Builder<Integer> integerField(Attribute<Integer> attribute) {
    Property<Integer> property = entityDefinition.property(attribute);

    return Components.integerField()
            .format(property.format())
            .minimumValue(property.minimumValue())
            .maximumValue(property.maximumValue())
            .toolTipText(property.description());
  }

  /**
   * Creates a {@link NumberField} builder based on the given attribute.
   * @param attribute the attribute
   * @return a {@link NumberField} builder
   */
  public final NumberField.Builder<Long> longField(Attribute<Long> attribute) {
    Property<Long> property = entityDefinition.property(attribute);

    return Components.longField()
            .format(property.format())
            .minimumValue(property.minimumValue())
            .maximumValue(property.maximumValue())
            .toolTipText(property.description());
  }

  /**
   * Creates a {@link NumberField} builder based on the given attribute.
   * @param attribute the attribute
   * @return a {@link NumberField} builder
   */
  public final NumberField.Builder<Double> doubleField(Attribute<Double> attribute) {
    Property<Double> property = entityDefinition.property(attribute);

    return Components.doubleField()
            .format(property.format())
            .minimumValue(property.minimumValue())
            .maximumValue(property.maximumValue())
            .maximumFractionDigits(property.maximumFractionDigits())
            .toolTipText(property.description());
  }

  /**
   * Creates a {@link NumberField} builder based on the given attribute.
   * @param attribute the attribute
   * @return a {@link NumberField} builder
   */
  public final NumberField.Builder<BigDecimal> bigDecimalField(Attribute<BigDecimal> attribute) {
    Property<BigDecimal> property = entityDefinition.property(attribute);

    return Components.bigDecimalField()
            .format(property.format())
            .minimumValue(property.minimumValue())
            .maximumValue(property.maximumValue())
            .maximumFractionDigits(property.maximumFractionDigits())
            .toolTipText(property.description());
  }

  /**
   * Creates a masked text field builder based on the given attribute.
   * @param attribute the attribute
   * @return a JFormattedTextField builder
   */
  public final MaskedTextFieldBuilder maskedTextField(Attribute<String> attribute) {
    Property<String> property = entityDefinition.property(attribute);

    return Components.maskedTextField()
            .toolTipText(property.description());
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
