/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.ui.component;

import is.codion.common.Configuration;
import is.codion.common.state.StateObserver;
import is.codion.common.value.PropertyValue;
import is.codion.common.value.Value;
import is.codion.framework.domain.entity.Attribute;
import is.codion.framework.domain.entity.Entity;
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
import is.codion.swing.common.ui.textfield.BigDecimalField;
import is.codion.swing.common.ui.textfield.DoubleField;
import is.codion.swing.common.ui.textfield.IntegerField;
import is.codion.swing.common.ui.textfield.LongField;
import is.codion.swing.common.ui.textfield.SizedDocument;
import is.codion.swing.common.ui.textfield.TemporalField;
import is.codion.swing.common.ui.value.UpdateOn;
import is.codion.swing.framework.model.SwingEntityComboBoxModel;

import javax.swing.ComboBoxModel;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JTextField;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.time.temporal.Temporal;
import java.util.function.Supplier;

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

  /**
   * Creates a builder.
   * @param attribute the attribute
   * @param value the value
   * @return a builder
   */
  public CheckBoxBuilder checkBoxBuilder(final Attribute<Boolean> attribute, final Value<Boolean> value) {
    final Property<Boolean> property = entityDefinition.getProperty(attribute);

    return ComponentBuilders.checkBoxBuilder(value)
            .description(property.getDescription())
            .nullable(property.isNullable())
            .caption(property.getCaption());
  }

  /**
   * Creates a builder.
   * @param attribute the attribute
   * @param value the value
   * @return a builder
   */
  public BooleanComboBoxBuilder booleanComboBoxBuilder(final Attribute<Boolean> attribute, final Value<Boolean> value) {
    final Property<Boolean> property = entityDefinition.getProperty(attribute);

    return ComponentBuilders.booleanComboBoxBuilder(value)
            .description(property.getDescription());
  }

  /**
   * Creates a builder.
   * @param foreignKey the foreign key
   * @param value the value
   * @param comboBoxModel the combo box model
   * @return a builder
   */
  public ForeignKeyComboBoxBuilder foreignKeyComboBoxBuilder(final ForeignKey foreignKey, final Value<Entity> value,
                                                             final SwingEntityComboBoxModel comboBoxModel) {
    final ForeignKeyProperty foreignKeyProperty = entityDefinition.getForeignKeyProperty(foreignKey);

    return new DefaultForeignKeyComboBoxBuilder(value, comboBoxModel)
            .description(foreignKeyProperty.getDescription());
  }

  /**
   * Creates a builder.
   * @param foreignKey the foreign key
   * @param value the value
   * @param searchModel the search model
   * @return a builder
   */
  public ForeignKeySearchFieldBuilder foreignKeySearchFieldBuilder(final ForeignKey foreignKey, final Value<Entity> value,
                                                                   final EntitySearchModel searchModel) {
    final ForeignKeyProperty foreignKeyProperty = entityDefinition.getForeignKeyProperty(foreignKey);

    return new DefaultForeignKeySearchFieldBuilder(value, searchModel)
            .description(foreignKeyProperty.getDescription() == null ? searchModel.getDescription() : foreignKeyProperty.getDescription());
  }

  /**
   * Creates a builder.
   * @param foreignKey the foreign key
   * @param value the value
   * @return a builder
   */
  public ForeignKeyFieldBuilder foreignKeyFieldBuilder(final ForeignKey foreignKey, final Value<Entity> value) {
    final ForeignKeyProperty foreignKeyProperty = entityDefinition.getForeignKeyProperty(foreignKey);

    return new DefaultForeignKeyFieldBuilder(value)
            .description(foreignKeyProperty.getDescription());
  }

  /**
   * Creates a builder.
   * @param attribute the attribute
   * @param value the value
   * @param <T> the attribute type
   * @return a builder
   */
  public <T> ValueListComboBoxBuilder<T> valueListComboBoxBuilder(final Attribute<T> attribute, final Value<T> value) {
    final Property<T> property = entityDefinition.getProperty(attribute);
    if (!(property instanceof ValueListProperty)) {
      throw new IllegalArgumentException("Property based on '" + property.getAttribute() + "' is not a ValueListProperty");
    }

    return ComponentBuilders.valueListComboBoxBuilder(value, ((ValueListProperty<T>) property).getValues())
            .description(property.getDescription())
            .nullable(property.isNullable());
  }

  /**
   * Creates a builder.
   * @param attribute the attribute
   * @param value the value
   * @param comboBoxModel the combo box model
   * @param <T> the attribute type
   * @return a builder
   */
  public <T> ComboBoxBuilder<T> comboBoxBuilder(final Attribute<T> attribute, final Value<T> value,
                                                final ComboBoxModel<T> comboBoxModel) {
    final Property<T> property = entityDefinition.getProperty(attribute);

    return ComponentBuilders.comboBoxBuilder(value, attribute.getTypeClass(), comboBoxModel)
            .description(property.getDescription());
  }

  /**
   * Creates a builder.
   * @param attribute the attribute
   * @param value the value
   * @param <T> the attribute type
   * @return a builder
   */
  public <T extends Temporal> TemporalInputPanelBuilder<T> temporalInputPanelBuilder(final Attribute<T> attribute, final Value<T> value) {
    if (!attribute.isTemporal()) {
      throw new IllegalArgumentException("Attribute " + attribute + " is not Temporal");
    }
    final Property<T> property = entityDefinition.getProperty(attribute);

    final Supplier<TemporalField<T>> supplier = () -> (TemporalField<T>) createTextField(property, attribute.getTypeClass());

    return ComponentBuilders.temporalInputPanelBuiler(value, supplier)
            .description(property.getDescription());
  }

  /**
   * Creates a builder.
   * @param attribute the attribute
   * @param value the value
   * @return a builder
   */
  public TextInputPanelBuilder textInputPanelBuilder(final Attribute<String> attribute, final Value<String> value) {
    final Property<String> property = entityDefinition.getProperty(attribute);

    return ComponentBuilders.textInputPanelBuilder(value)
            .description(property.getDescription())
            .caption(property.getCaption());
  }

  /**
   * Creates a builder.
   * @param attribute the attribute
   * @param value the value
   * @return a builder
   */
  public TextAreaBuilder textAreaBuilder(final Attribute<String> attribute, final Value<String> value) {
    final Property<String> property = entityDefinition.getProperty(attribute);
    if (!attribute.isString()) {
      throw new IllegalArgumentException("Cannot create a text area for a non-string attribute");
    }

    return ComponentBuilders.textAreaBuilder(value)
            .description(property.getDescription());
  }

  /**
   * Creates a builder.
   * @param attribute the attribute
   * @param value the value
   * @param <T> the attribute type
   * @return a builder
   */
  public <T> TextFieldBuilder<T> textFieldBuilder(final Attribute<T> attribute, final Value<T> value) {
    final Property<T> property = entityDefinition.getProperty(attribute);

    final Supplier<JTextField> textFieldSupplier = () -> createTextField(property, attribute.getTypeClass());

    return ComponentBuilders.textFieldBuilder(value, attribute.getTypeClass(), textFieldSupplier)
            .format(property.getFormat())
            .description(property.getDescription());
  }

  /**
   * Creates a builder.
   * @param attribute the attribute
   * @param value the value
   * @return a builder
   */
  public FormattedTextFieldBuilder formattedTextFieldBuilder(final Attribute<String> attribute, final Value<String> value) {
    final Property<String> property = entityDefinition.getProperty(attribute);

    return ComponentBuilders.formattedTextFieldBuilder(value)
            .description(property.getDescription());
  }

  private static JTextField createTextField(final Property<?> property, final Class<?> valueClass) {
    final Attribute<?> attribute = property.getAttribute();
    if (valueClass.equals(Integer.class)) {
      return initializeIntegerField((Property<Integer>) property);
    }
    if (valueClass.equals(Double.class)) {
      return initializeDoubleField((Property<Double>) property);
    }
    if (valueClass.equals(BigDecimal.class)) {
      return initializeBigDecimalField((Property<BigDecimal>) property);
    }
    if (valueClass.equals(Long.class)) {
      return initializeLongField((Property<Long>) property);
    }
    if (Temporal.class.isAssignableFrom(valueClass)) {
      return initializeTemporalField(property.getDateTimePattern(), (Class<Temporal>) attribute.getTypeClass());
    }
    if (valueClass.equals(String.class)) {
      return initializeStringField(property.getMaximumLength());
    }
    if (valueClass.equals(Character.class)) {
      return new JTextField(new SizedDocument(1), "", 1);
    }

    throw new IllegalArgumentException("Creating text fields for type: " + attribute.getTypeClass() + " is not implemented (" + property + ")");
  }

  private static JTextField initializeStringField(final int maximumLength) {
    final SizedDocument sizedDocument = new SizedDocument();
    if (maximumLength > 0) {
      sizedDocument.setMaximumLength(maximumLength);
    }

    return new JTextField(sizedDocument, "", 0);
  }

  private static DoubleField initializeDoubleField(final Property<Double> property) {
    final DoubleField field = new DoubleField((DecimalFormat) cloneFormat((NumberFormat) property.getFormat()));
    if (property.getMinimumValue() != null && property.getMaximumValue() != null) {
      field.setRange(Math.min(property.getMinimumValue(), 0), property.getMaximumValue());
    }

    return field;
  }

  private static BigDecimalField initializeBigDecimalField(final Property<BigDecimal> property) {
    final BigDecimalField field = new BigDecimalField((DecimalFormat) cloneFormat((NumberFormat) property.getFormat()));
    if (property.getMinimumValue() != null && property.getMaximumValue() != null) {
      field.setRange(Math.min(property.getMinimumValue(), 0), property.getMaximumValue());
    }

    return field;
  }

  private static IntegerField initializeIntegerField(final Property<Integer> property) {
    final IntegerField field = new IntegerField(cloneFormat((NumberFormat) property.getFormat()));
    if (property.getMinimumValue() != null && property.getMaximumValue() != null) {
      field.setRange(property.getMinimumValue(), property.getMaximumValue());
    }

    return field;
  }

  private static LongField initializeLongField(final Property<Long> property) {
    final LongField field = new LongField(cloneFormat((NumberFormat) property.getFormat()));
    if (property.getMinimumValue() != null && property.getMaximumValue() != null) {
      field.setRange(property.getMinimumValue(), property.getMaximumValue());
    }

    return field;
  }

  private static TemporalField<Temporal> initializeTemporalField(final String dateTimePattern,
                                                                  final Class<Temporal> valueType) {
    return TemporalField.builder(valueType)
            .dateTimePattern(dateTimePattern)
            .build();
  }

  private static NumberFormat cloneFormat(final NumberFormat format) {
    final NumberFormat cloned = (NumberFormat) format.clone();
    cloned.setGroupingUsed(format.isGroupingUsed());
    cloned.setMaximumIntegerDigits(format.getMaximumIntegerDigits());
    cloned.setMaximumFractionDigits(format.getMaximumFractionDigits());
    cloned.setMinimumFractionDigits(format.getMinimumFractionDigits());
    cloned.setRoundingMode(format.getRoundingMode());
    cloned.setCurrency(format.getCurrency());
    cloned.setParseIntegerOnly(format.isParseIntegerOnly());

    return cloned;
  }
}
