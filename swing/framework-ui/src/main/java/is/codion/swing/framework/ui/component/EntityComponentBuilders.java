/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
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
import is.codion.swing.common.ui.component.BigDecimalFieldBuilder;
import is.codion.swing.common.ui.component.BooleanComboBoxBuilder;
import is.codion.swing.common.ui.component.CheckBoxBuilder;
import is.codion.swing.common.ui.component.ComboBoxBuilder;
import is.codion.swing.common.ui.component.ComponentBuilders;
import is.codion.swing.common.ui.component.DoubleFieldBuilder;
import is.codion.swing.common.ui.component.FormattedTextFieldBuilder;
import is.codion.swing.common.ui.component.IntegerFieldBuilder;
import is.codion.swing.common.ui.component.ItemComboBoxBuilder;
import is.codion.swing.common.ui.component.LocalDateFieldBuilder;
import is.codion.swing.common.ui.component.LocalDateTimeFieldBuilder;
import is.codion.swing.common.ui.component.LocalTimeFieldBuilder;
import is.codion.swing.common.ui.component.LongFieldBuilder;
import is.codion.swing.common.ui.component.OffsetDateTimeFieldBuilder;
import is.codion.swing.common.ui.component.TemporalInputPanelBuilder;
import is.codion.swing.common.ui.component.TextAreaBuilder;
import is.codion.swing.common.ui.component.TextFieldBuilder;
import is.codion.swing.common.ui.component.TextInputPanelBuilder;
import is.codion.swing.common.ui.component.ToggleButtonBuilder;
import is.codion.swing.framework.model.SwingEntityComboBoxModel;

import javax.swing.ComboBoxModel;
import javax.swing.JTextField;
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
public class EntityComponentBuilders {

  private final EntityDefinition entityDefinition;

  /**
   * @param entityDefinition the entity definition
   */
  public EntityComponentBuilders(final EntityDefinition entityDefinition) {
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

    return ComponentBuilders.checkBox()
            .description(property.getDescription())
            .nullable(property.isNullable())
            .caption(property.getCaption())
            .includeCaption(false);
  }

  /**
   * Creates a builder.
   * @param attribute the attribute
   * @return a builder
   */
  public final ToggleButtonBuilder toggleButton(final Attribute<Boolean> attribute) {
    final Property<Boolean> property = entityDefinition.getProperty(attribute);

    return ComponentBuilders.toggleButton()
            .description(property.getDescription())
            .caption(property.getCaption())
            .includeCaption(false);
  }

  /**
   * Creates a builder.
   * @param attribute the attribute
   * @return a builder
   */
  public final BooleanComboBoxBuilder booleanComboBox(final Attribute<Boolean> attribute) {
    final Property<Boolean> property = entityDefinition.getProperty(attribute);

    return ComponentBuilders.booleanComboBox(ItemComboBoxModel.createBooleanModel())
            .description(property.getDescription());
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
            .description(foreignKeyProperty.getDescription());
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
            .description(foreignKeyProperty.getDescription() == null ? searchModel.getDescription() : foreignKeyProperty.getDescription());
  }

  /**
   * Creates a builder.
   * @param foreignKey the foreign key
   * @return a builder
   */
  public final ForeignKeyFieldBuilder foreignKeyField(final ForeignKey foreignKey) {
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
  public final <T> ItemComboBoxBuilder<T> itemComboBox(final Attribute<T> attribute) {
    final Property<T> property = entityDefinition.getProperty(attribute);
    if (!(property instanceof ItemProperty)) {
      throw new IllegalArgumentException("Property based on '" + property.getAttribute() + "' is not a ItemProperty");
    }

    return ComponentBuilders.itemComboBox(((ItemProperty<T>) property).getValues())
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
  public final <T> ComboBoxBuilder<T> comboBox(final Attribute<T> attribute, final ComboBoxModel<T> comboBoxModel) {
    final Property<T> property = entityDefinition.getProperty(attribute);

    return ComponentBuilders.comboBox(attribute.getTypeClass(), comboBoxModel)
            .description(property.getDescription());
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

    return ComponentBuilders.temporalInputPanel(attribute.getTypeClass())
            .description(property.getDescription())
            .dateTimePattern(property.getDateTimePattern());
  }

  /**
   * Creates a builder.
   * @param attribute the attribute
   * @return a builder
   */
  public final TextInputPanelBuilder textInputPanel(final Attribute<String> attribute) {
    final Property<String> property = entityDefinition.getProperty(attribute);

    return ComponentBuilders.textInputPanel()
            .description(property.getDescription())
            .maximumLength(property.getMaximumLength())
            .caption(property.getCaption());
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

    return ComponentBuilders.textArea()
            .description(property.getDescription())
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
              .description(property.getDescription());
    }
    else if (typeClass.equals(LocalDate.class)) {
      return (TextFieldBuilder<T, C, B>) localDateField((Attribute<LocalDate>) attribute)
              .description(property.getDescription());
    }
    else if (typeClass.equals(LocalDateTime.class)) {
      return (TextFieldBuilder<T, C, B>) localDateTimeField((Attribute<LocalDateTime>) attribute)
              .description(property.getDescription());
    }
    else if (typeClass.equals(OffsetDateTime.class)) {
      return (TextFieldBuilder<T, C, B>) offsetDateTimeField((Attribute<OffsetDateTime>) attribute)
              .description(property.getDescription());
    }

    return (TextFieldBuilder<T, C, B>) ComponentBuilders.textField(typeClass)
            .format(property.getFormat())
            .maximumLength(property.getMaximumLength())
            .description(property.getDescription());
  }

  /**
   * Creates a builder.
   * @param attribute the attribute
   * @return a builder
   */
  public final LocalTimeFieldBuilder localTimeField(final Attribute<LocalTime> attribute) {
    final Property<LocalTime> property = entityDefinition.getProperty(attribute);

    return ComponentBuilders.localTimeField(property.getDateTimePattern())
            .description(property.getDescription());
  }

  /**
   * Creates a builder.
   * @param attribute the attribute
   * @return a builder
   */
  public final LocalDateFieldBuilder localDateField(final Attribute<LocalDate> attribute) {
    final Property<LocalDate> property = entityDefinition.getProperty(attribute);

    return ComponentBuilders.localDateField(property.getDateTimePattern())
            .description(property.getDescription());
  }

  /**
   * Creates a builder.
   * @param attribute the attribute
   * @return a builder
   */
  public final LocalDateTimeFieldBuilder localDateTimeField(final Attribute<LocalDateTime> attribute) {
    final Property<LocalDateTime> property = entityDefinition.getProperty(attribute);

    return ComponentBuilders.localDateTimeField(property.getDateTimePattern())
            .description(property.getDescription());
  }

  /**
   * Creates a builder.
   * @param attribute the attribute
   * @return a builder
   */
  public final OffsetDateTimeFieldBuilder offsetDateTimeField(final Attribute<OffsetDateTime> attribute) {
    final Property<OffsetDateTime> property = entityDefinition.getProperty(attribute);

    return ComponentBuilders.offsetDateTimeField(property.getDateTimePattern())
            .description(property.getDescription());
  }

  /**
   * Creates a builder.
   * @param attribute the attribute
   * @return a builder
   */
  public final IntegerFieldBuilder integerField(final Attribute<Integer> attribute) {
    final Property<Integer> property = entityDefinition.getProperty(attribute);

    return ComponentBuilders.integerField()
            .format(property.getFormat())
            .maximumLength(property.getMaximumLength())
            .minimumValue(property.getMinimumValue())
            .maximumValue(property.getMaximumValue())
            .description(property.getDescription());
  }

  /**
   * Creates a builder.
   * @param attribute the attribute
   * @return a builder
   */
  public final LongFieldBuilder longField(final Attribute<Long> attribute) {
    final Property<Long> property = entityDefinition.getProperty(attribute);

    return ComponentBuilders.longField()
            .format(property.getFormat())
            .maximumLength(property.getMaximumLength())
            .minimumValue(property.getMinimumValue())
            .maximumValue(property.getMaximumValue())
            .description(property.getDescription());
  }

  /**
   * Creates a builder.
   * @param attribute the attribute
   * @return a builder
   */
  public final DoubleFieldBuilder doubleField(final Attribute<Double> attribute) {
    final Property<Double> property = entityDefinition.getProperty(attribute);

    return ComponentBuilders.doubleField()
            .format(property.getFormat())
            .maximumLength(property.getMaximumLength())
            .minimumValue(property.getMinimumValue())
            .maximumValue(property.getMaximumValue())
            .maximumFractionDigits(property.getMaximumFractionDigits())
            .description(property.getDescription());
  }

  /**
   * Creates a builder.
   * @param attribute the attribute
   * @return a builder
   */
  public final BigDecimalFieldBuilder bigDecimalField(final Attribute<BigDecimal> attribute) {
    final Property<BigDecimal> property = entityDefinition.getProperty(attribute);

    return ComponentBuilders.bigDecimalField()
            .format(property.getFormat())
            .maximumLength(property.getMaximumLength())
            .minimumValue(property.getMinimumValue())
            .maximumValue(property.getMaximumValue())
            .maximumFractionDigits(property.getMaximumFractionDigits())
            .description(property.getDescription());
  }

  /**
   * Creates a builder.
   * @param attribute the attribute
   * @return a builder
   */
  public final FormattedTextFieldBuilder formattedTextField(final Attribute<String> attribute) {
    final Property<String> property = entityDefinition.getProperty(attribute);

    return ComponentBuilders.formattedTextField()
            .description(property.getDescription());
  }
}
