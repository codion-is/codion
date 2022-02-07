/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.ui.component;

import is.codion.framework.domain.entity.Attribute;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.ForeignKey;
import is.codion.framework.domain.property.ForeignKeyProperty;
import is.codion.framework.domain.property.ItemProperty;
import is.codion.framework.domain.property.Property;
import is.codion.framework.model.EntitySearchModel;
import is.codion.swing.common.model.combobox.ItemComboBoxModel;
import is.codion.swing.common.ui.combobox.SteppedComboBox;
import is.codion.swing.common.ui.component.BigDecimalFieldBuilder;
import is.codion.swing.common.ui.component.ButtonBuilder;
import is.codion.swing.common.ui.component.CheckBoxBuilder;
import is.codion.swing.common.ui.component.ComboBoxBuilder;
import is.codion.swing.common.ui.component.Components;
import is.codion.swing.common.ui.component.DoubleFieldBuilder;
import is.codion.swing.common.ui.component.FormattedTextFieldBuilder;
import is.codion.swing.common.ui.component.IntegerFieldBuilder;
import is.codion.swing.common.ui.component.ItemComboBoxBuilder;
import is.codion.swing.common.ui.component.LongFieldBuilder;
import is.codion.swing.common.ui.component.TemporalFieldBuilder;
import is.codion.swing.common.ui.component.TemporalInputPanelBuilder;
import is.codion.swing.common.ui.component.TextAreaBuilder;
import is.codion.swing.common.ui.component.TextFieldBuilder;
import is.codion.swing.common.ui.component.TextInputPanelBuilder;
import is.codion.swing.common.ui.textfield.TemporalField;
import is.codion.swing.framework.model.SwingEntityComboBoxModel;

import javax.swing.ComboBoxModel;
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
 * A factory for {@link is.codion.swing.common.ui.component.ComponentBuilder}.
 */
public class EntityComponents {

  private final EntityDefinition entityDefinition;

  /**
   * @param entityDefinition the entity definition
   */
  public EntityComponents(final EntityDefinition entityDefinition) {
    this.entityDefinition = requireNonNull(entityDefinition);
  }

  /**
   * @return the underlying entity definition
   */
  public final EntityDefinition getEntityDefinition() {
    return entityDefinition;
  }

  /**
   * Creates a builder.
   * @param attribute the attribute
   * @return a builder
   */
  public final CheckBoxBuilder checkBox(final Attribute<Boolean> attribute) {
    final Property<Boolean> property = entityDefinition.getProperty(attribute);

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
  public final <B extends ButtonBuilder<Boolean, JToggleButton, B>> ButtonBuilder<Boolean, JToggleButton, B> toggleButton(final Attribute<Boolean> attribute) {
    final Property<Boolean> property = entityDefinition.getProperty(attribute);

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
  public final ItemComboBoxBuilder<Boolean> booleanComboBox(final Attribute<Boolean> attribute) {
    final Property<Boolean> property = entityDefinition.getProperty(attribute);

    return Components.booleanComboBox(ItemComboBoxModel.createBooleanModel())
            .toolTipText(property.getDescription());
  }

  /**
   * Creates a builder.
   * @param foreignKey the foreign key
   * @param comboBoxModel the combo box model
   * @return a builder
   */
  public final ForeignKeyComboBoxBuilder foreignKeyComboBox(final ForeignKey foreignKey, final SwingEntityComboBoxModel comboBoxModel) {
    final ForeignKeyProperty foreignKeyProperty = entityDefinition.getForeignKeyProperty(foreignKey);

    return new DefaultForeignKeyComboBoxBuilder(comboBoxModel)
            .toolTipText(foreignKeyProperty.getDescription());
  }

  /**
   * Creates a builder.
   * @param foreignKey the foreign key
   * @param searchModel the search model
   * @return a builder
   */
  public final ForeignKeySearchFieldBuilder foreignKeySearchField(final ForeignKey foreignKey, final EntitySearchModel searchModel) {
    final ForeignKeyProperty foreignKeyProperty = entityDefinition.getForeignKeyProperty(foreignKey);

    return new DefaultForeignKeySearchFieldBuilder(searchModel)
            .toolTipText(foreignKeyProperty.getDescription() == null ? searchModel.getDescription() : foreignKeyProperty.getDescription());
  }

  /**
   * Creates a builder.
   * @param foreignKey the foreign key
   * @return a builder
   */
  public final ForeignKeyFieldBuilder foreignKeyField(final ForeignKey foreignKey) {
    final ForeignKeyProperty foreignKeyProperty = entityDefinition.getForeignKeyProperty(foreignKey);

    return new DefaultForeignKeyFieldBuilder()
            .toolTipText(foreignKeyProperty.getDescription());
  }

  /**
   * Creates a builder.
   * @param attribute the attribute
   * @param <T> the attribute type
   * @return a builder
   */
  public final <T> ItemComboBoxBuilder<T> itemComboBox(final Attribute<T> attribute) {
    final Property<T> property = entityDefinition.getProperty(attribute);
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
  public final <T, C extends SteppedComboBox<T>, B extends ComboBoxBuilder<T, C, B>> ComboBoxBuilder<T, C, B> comboBox(final Attribute<T> attribute, final ComboBoxModel<T> comboBoxModel) {
    final Property<T> property = entityDefinition.getProperty(attribute);

    return (ComboBoxBuilder<T, C, B>) Components.comboBox(comboBoxModel)
            .toolTipText(property.getDescription());
  }

  /**
   * Creates a builder.
   * @param attribute the attribute
   * @param <T> the attribute type
   * @return a builder
   */
  public final <T extends Temporal> TemporalInputPanelBuilder<T> temporalInputPanel(final Attribute<T> attribute) {
    if (!attribute.isTemporal()) {
      throw new IllegalArgumentException("Attribute " + attribute + " is not Temporal");
    }
    final Property<T> property = entityDefinition.getProperty(attribute);

    return Components.temporalInputPanel(attribute.getTypeClass())
            .toolTipText(property.getDescription())
            .dateTimePattern(property.getDateTimePattern());
  }

  /**
   * Creates a builder.
   * @param attribute the attribute
   * @return a builder
   */
  public final TextInputPanelBuilder textInputPanel(final Attribute<String> attribute) {
    final Property<String> property = entityDefinition.getProperty(attribute);

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
  public final TextAreaBuilder textArea(final Attribute<String> attribute) {
    final Property<String> property = entityDefinition.getProperty(attribute);
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
  public final <T, C extends JTextField, B extends TextFieldBuilder<T, C, B>> TextFieldBuilder<T, C, B> textField(final Attribute<T> attribute) {
    final Property<T> property = entityDefinition.getProperty(attribute);

    final Class<T> typeClass = attribute.getTypeClass();
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
  public final TemporalFieldBuilder<LocalTime, TemporalField<LocalTime>> localTimeField(final Attribute<LocalTime> attribute) {
    final Property<LocalTime> property = entityDefinition.getProperty(attribute);

    return Components.localTimeField(property.getDateTimePattern())
            .toolTipText(property.getDescription());
  }

  /**
   * Creates a builder.
   * @param attribute the attribute
   * @return a builder
   */
  public final TemporalFieldBuilder<LocalDate, TemporalField<LocalDate>> localDateField(final Attribute<LocalDate> attribute) {
    final Property<LocalDate> property = entityDefinition.getProperty(attribute);

    return Components.localDateField(property.getDateTimePattern())
            .toolTipText(property.getDescription());
  }

  /**
   * Creates a builder.
   * @param attribute the attribute
   * @return a builder
   */
  public final TemporalFieldBuilder<LocalDateTime, TemporalField<LocalDateTime>> localDateTimeField(final Attribute<LocalDateTime> attribute) {
    final Property<LocalDateTime> property = entityDefinition.getProperty(attribute);

    return Components.localDateTimeField(property.getDateTimePattern())
            .toolTipText(property.getDescription());
  }

  /**
   * Creates a builder.
   * @param attribute the attribute
   * @return a builder
   */
  public final TemporalFieldBuilder<OffsetDateTime, TemporalField<OffsetDateTime>> offsetDateTimeField(final Attribute<OffsetDateTime> attribute) {
    final Property<OffsetDateTime> property = entityDefinition.getProperty(attribute);

    return Components.offsetDateTimeField(property.getDateTimePattern())
            .toolTipText(property.getDescription());
  }

  /**
   * Creates a builder.
   * @param attribute the attribute
   * @return a builder
   */
  public final IntegerFieldBuilder integerField(final Attribute<Integer> attribute) {
    final Property<Integer> property = entityDefinition.getProperty(attribute);

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
  public final LongFieldBuilder longField(final Attribute<Long> attribute) {
    final Property<Long> property = entityDefinition.getProperty(attribute);

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
  public final DoubleFieldBuilder doubleField(final Attribute<Double> attribute) {
    final Property<Double> property = entityDefinition.getProperty(attribute);

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
  public final BigDecimalFieldBuilder bigDecimalField(final Attribute<BigDecimal> attribute) {
    final Property<BigDecimal> property = entityDefinition.getProperty(attribute);

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
  public final FormattedTextFieldBuilder formattedTextField(final Attribute<String> attribute) {
    final Property<String> property = entityDefinition.getProperty(attribute);

    return Components.formattedTextField()
            .toolTipText(property.getDescription());
  }
}
