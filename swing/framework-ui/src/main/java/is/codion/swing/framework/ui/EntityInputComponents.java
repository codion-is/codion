/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.ui;

import is.codion.common.Configuration;
import is.codion.common.formats.LocaleDateTimePattern;
import is.codion.common.item.Item;
import is.codion.common.model.combobox.FilteredComboBoxModel;
import is.codion.common.state.StateObserver;
import is.codion.common.value.AbstractValue;
import is.codion.common.value.PropertyValue;
import is.codion.common.value.Value;
import is.codion.framework.domain.entity.Attribute;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.ForeignKey;
import is.codion.framework.domain.property.Property;
import is.codion.framework.domain.property.ValueListProperty;
import is.codion.framework.model.EntitySearchModel;
import is.codion.swing.common.model.checkbox.NullableToggleButtonModel;
import is.codion.swing.common.model.combobox.BooleanComboBoxModel;
import is.codion.swing.common.model.combobox.ItemComboBoxModel;
import is.codion.swing.common.ui.Components;
import is.codion.swing.common.ui.checkbox.NullableCheckBox;
import is.codion.swing.common.ui.combobox.Completion;
import is.codion.swing.common.ui.combobox.SteppedComboBox;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.textfield.BigDecimalField;
import is.codion.swing.common.ui.textfield.DoubleField;
import is.codion.swing.common.ui.textfield.IntegerField;
import is.codion.swing.common.ui.textfield.LongField;
import is.codion.swing.common.ui.textfield.SizedDocument;
import is.codion.swing.common.ui.textfield.TemporalField;
import is.codion.swing.common.ui.textfield.TextFields;
import is.codion.swing.common.ui.textfield.TextFields.ValueContainsLiterals;
import is.codion.swing.common.ui.textfield.TextInputPanel;
import is.codion.swing.common.ui.textfield.TextInputPanel.ButtonFocusable;
import is.codion.swing.common.ui.time.TemporalInputPanel;
import is.codion.swing.common.ui.value.ComponentValues;
import is.codion.swing.common.ui.value.UpdateOn;
import is.codion.swing.framework.model.SwingEntityComboBoxModel;

import javax.swing.ComboBoxModel;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.text.AbstractDocument;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.time.temporal.Temporal;
import java.util.List;

import static is.codion.swing.common.ui.textfield.ParsingDocumentFilter.parsingDocumentFilter;
import static is.codion.swing.common.ui.textfield.StringLengthValidator.stringLengthValidator;
import static java.util.Objects.requireNonNull;

/**
 * Provides input components for editing entities.
 */
public final class EntityInputComponents {

  /**
   * Identifies the completion mode MaximumMatch
   * @see EntityInputComponents#COMBO_BOX_COMPLETION_MODE
   * @see Completion#maximumMatch(JComboBox)
   */
  public static final String COMPLETION_MODE_MAXIMUM_MATCH = "max";

  /**
   * Identifies the completion mode AutoCompletion
   * @see EntityInputComponents#COMBO_BOX_COMPLETION_MODE
   * @see Completion#autoComplete(JComboBox)
   */
  public static final String COMPLETION_MODE_AUTOCOMPLETE = "auto";

  /**
   * No completion.
   * @see EntityInputComponents#COMBO_BOX_COMPLETION_MODE
   * @see Completion#autoComplete(JComboBox)
   */
  public static final String COMPLETION_MODE_NONE = "none";

  /**
   * Specifies whether maximum match or autocomplete is used for comboboxes,
   * {@link #COMPLETION_MODE_MAXIMUM_MATCH} for maximum match
   * and {@link #COMPLETION_MODE_AUTOCOMPLETE} for auto completion.<br>
   * Value type:String<br>
   * Default value: {@link #COMPLETION_MODE_MAXIMUM_MATCH}
   */
  public static final PropertyValue<String> COMBO_BOX_COMPLETION_MODE = Configuration.stringValue("codion.swing.comboBoxCompletionMode", COMPLETION_MODE_MAXIMUM_MATCH);

  /**
   * Specifies the default horizontal alignment used in labels<br>
   * Value type: Integer (JLabel.LEFT, JLabel.RIGHT, JLabel.CENTER)<br>
   * Default value: JLabel.LEFT
   */
  public static final PropertyValue<Integer> LABEL_TEXT_ALIGNMENT = Configuration.integerValue("codion.swing.labelTextAlignment", JLabel.LEFT);

  /**
   * Specifies whether a component should include a caption.
   * Applies to components that have captions, such as JCheckBox.
   */
  public enum IncludeCaption {
    /**
     * Include caption.
     */
    YES,
    /**
     * Don't include caption.
     */
    NO
  }

  /**
   * Specifies whether the contents of a combo box should be sorted.
   */
  public enum Sorted {
    /**
     * Sort contents.
     */
    YES,
    /**
     * Don't sort contents.
     */
    NO
  }

  /**
   * Specifies whether a combo box should be editable.
   */
  public enum Editable {
    /**
     * Combo box should be editable.
     */
    YES,
    /**
     * Combo box should not be editable.
     */
    NO
  }

  /**
   * Specifies whether a {@link TemporalInputPanel} should contain a button for opening a Calendar for input entry.
   * Only applies to temporal values containing a date part, as in, not those that contain time only.
   */
  public enum CalendarButton {
    /**
     * Include a calendar button.
     */
    YES,
    /**
     * Don't include a calendar button.
     */
    NO
  }

  private static final String ATTRIBUTE_PARAM_NAME = "attribute";
  private static final String VALUE_PARAM_NAME = "value";
  private static final String FOREIGN_KEY_PROPERTY_PARAM_NAME = "foreignKeyProperty";
  private static final int BOOLEAN_COMBO_BOX_POPUP_WIDTH = 40;

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
      return createValueListComboBox(attribute, value, enabledState);
    }
    if (attribute.isBoolean()) {
      return property.isNullable() ?
              createNullableCheckBox((Attribute<Boolean>) attribute, (Value<Boolean>) value, enabledState, IncludeCaption.NO) :
              createCheckBox((Attribute<Boolean>) attribute, (Value<Boolean>) value, enabledState, IncludeCaption.NO);
    }
    if (attribute.isTemporal() || attribute.isNumerical() || attribute.isString() || attribute.isCharacter()) {
      return createTextField(attribute, value, UpdateOn.KEYSTROKE, enabledState);
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
   * Creates a JCheckBox based on the given boolean attribute
   * @param attribute the attribute on which value to base the checkbox
   * @param value the value to bind to the field
   * @return a check box based on the given attribute
   * @throws IllegalArgumentException in case the attribute is not a boolean attribute
   */
  public JCheckBox createCheckBox(final Attribute<Boolean> attribute, final Value<Boolean> value) {
    return createCheckBox(attribute, value, null);
  }

  /**
   * Creates a JCheckBox based on the given boolean attribute
   * @param attribute the attribute on which value to base the checkbox
   * @param value the value to bind to the field
   * @param enabledState the state controlling the enabled state of the checkbox
   * @return a check box based on the given attribute
   * @throws IllegalArgumentException in case the attribute is not a boolean attribute
   */
  public JCheckBox createCheckBox(final Attribute<Boolean> attribute, final Value<Boolean> value,
                                  final StateObserver enabledState) {
    return createCheckBox(attribute, value, enabledState, IncludeCaption.YES);
  }

  /**
   * Creates a JCheckBox based on the given boolean attribute
   * @param attribute the attribute on which value to base the checkbox
   * @param value the value to bind to the field
   * @param enabledState the state controlling the enabled state of the checkbox
   * @param includeCaption if yes then the attribute caption is included as the checkbox text
   * @return a check box based on the given attribute
   * @throws IllegalArgumentException in case the attribute is not a boolean attribute
   */
  public JCheckBox createCheckBox(final Attribute<Boolean> attribute, final Value<Boolean> value, final StateObserver enabledState,
                                  final IncludeCaption includeCaption) {
    requireNonNull(attribute, ATTRIBUTE_PARAM_NAME);
    requireNonNull(value, VALUE_PARAM_NAME);

    final Property<Boolean> property = entityDefinition.getProperty(attribute);

    return initializeCheckBox(property, value, enabledState,
            includeCaption == IncludeCaption.YES ? new JCheckBox(property.getCaption()) : new JCheckBox());
  }

  /**
   * Creates a NullableCheckBox based on the given boolean attribute
   * @param attribute the attribute on which value to base the checkbox
   * @param value the value to bind to the field
   * @param enabledState the state controlling the enabled state of the checkbox
   * @param includeCaption if yes then the attribute caption is included as the checkbox text
   * @return a check box based on the given attribute
   * @throws IllegalArgumentException in case the attribute is not a nullable boolean attribute
   */
  public NullableCheckBox createNullableCheckBox(final Attribute<Boolean> attribute, final Value<Boolean> value,
                                                 final StateObserver enabledState, final IncludeCaption includeCaption) {
    requireNonNull(attribute, ATTRIBUTE_PARAM_NAME);
    requireNonNull(value, VALUE_PARAM_NAME);
    final Property<Boolean> property = entityDefinition.getProperty(attribute);
    if (!property.isNullable()) {
      throw new IllegalArgumentException("Nullable boolean attribute required for createNullableCheckBox");
    }

    return (NullableCheckBox) initializeCheckBox(property, value, enabledState,
            new NullableCheckBox(new NullableToggleButtonModel(), includeCaption == IncludeCaption.YES ? property.getCaption() : null));
  }

  /**
   * Creates a combobox containing the values (null, yes, no) based on the given boolean attribute
   * @param attribute the attribute on which to base the combobox
   * @param value the value to bind to the field
   * @return a SteppedComboBox based on the given boolean attribute
   */
  public SteppedComboBox<Item<Boolean>> createBooleanComboBox(final Attribute<Boolean> attribute, final Value<Boolean> value) {
    return createBooleanComboBox(attribute, value, null);
  }

  /**
   * Creates a combobox containing the values (null, yes, no) based on the given boolean attribute
   * @param attribute the attribute on which to base the combobox
   * @param value the value to bind to the field
   * @param enabledState the state controlling the enabled state of the combobox
   * @return a SteppedComboBox based on the given boolean attribute
   */
  public SteppedComboBox<Item<Boolean>> createBooleanComboBox(final Attribute<Boolean> attribute, final Value<Boolean> value,
                                                              final StateObserver enabledState) {
    requireNonNull(attribute, ATTRIBUTE_PARAM_NAME);
    requireNonNull(value, VALUE_PARAM_NAME);
    final BooleanComboBoxModel comboBoxModel = new BooleanComboBoxModel();
    final SteppedComboBox<Item<Boolean>> comboBox = new SteppedComboBox<>(comboBoxModel);
    ComponentValues.itemComboBox(comboBox).link(value);
    linkToEnabledState(enabledState, comboBox);
    comboBox.setToolTipText(entityDefinition.getProperty(attribute).getDescription());
    addComboBoxCompletion(comboBox);
    comboBox.setPopupWidth(BOOLEAN_COMBO_BOX_POPUP_WIDTH);

    return comboBox;
  }

  /**
   * Creates EntityComboBox based on the given foreign key
   * @param foreignKey the foreign key on which entity to base the combobox
   * @param value the value to bind to the field
   * @param comboBoxModel the combo box model
   * @return a SwingEntityComboBox based on the given foreign key
   */
  public EntityComboBox createForeignKeyComboBox(final ForeignKey foreignKey, final Value<Entity> value,
                                                 final SwingEntityComboBoxModel comboBoxModel) {
    return createForeignKeyComboBox(foreignKey, value, comboBoxModel, null);
  }

  /**
   * Creates EntityComboBox based on the given foreign key
   * @param foreignKey the foreign key on which entity to base the combobox
   * @param value the value to bind to the field
   * @param comboBoxModel the combo box model
   * @param enabledState the state controlling the enabled state of the combobox
   * @return a EntityComboBox based on the given foreign key
   */
  public EntityComboBox createForeignKeyComboBox(final ForeignKey foreignKey, final Value<Entity> value,
                                                 final SwingEntityComboBoxModel comboBoxModel, final StateObserver enabledState) {
    requireNonNull(foreignKey, FOREIGN_KEY_PROPERTY_PARAM_NAME);
    requireNonNull(comboBoxModel, "comboBoxModel");
    comboBoxModel.refresh();
    final EntityComboBox comboBox = new EntityComboBox(comboBoxModel);
    ComponentValues.comboBox(comboBox).link(value);
    linkToEnabledState(enabledState, comboBox);
    addComboBoxCompletion(comboBox);
    comboBox.setToolTipText(entityDefinition.getProperty(foreignKey).getDescription());

    return comboBox;
  }

  /**
   * Creates a {@link EntitySearchField} based on the given foreign key
   * @param foreignKey the foreign key on which entity to base the search model
   * @param value the value to bind to the field
   * @param searchModel the {@link EntitySearchModel} to use and to bind with the value
   * @return a search model based on the given foreign key
   */
  public EntitySearchField createForeignKeySearchField(final ForeignKey foreignKey, final Value<Entity> value,
                                                       final EntitySearchModel searchModel) {
    return createForeignKeySearchField(foreignKey, value, searchModel, null);
  }

  /**
   * Creates a {@link EntitySearchField} based on the given foreign key
   * @param foreignKey the foreign key on which entity to base the search model
   * @param value the value to bind to the field
   * @param searchModel the {@link EntitySearchModel} to use and to bind with the value
   * @param enabledState the state controlling the enabled state of the search field
   * @return a search model based on the given foreign key
   */
  public EntitySearchField createForeignKeySearchField(final ForeignKey foreignKey, final Value<Entity> value,
                                                       final EntitySearchModel searchModel, final StateObserver enabledState) {
    requireNonNull(foreignKey, FOREIGN_KEY_PROPERTY_PARAM_NAME);
    requireNonNull(searchModel, "searchModel");
    requireNonNull(value, VALUE_PARAM_NAME);
    final EntitySearchField searchField = new EntitySearchField(searchModel);
    new SearchUIValue(searchField.getModel()).link(value);
    linkToEnabledState(enabledState, searchField);
    final String propertyDescription = entityDefinition.getProperty(foreignKey).getDescription();
    searchField.setToolTipText(propertyDescription == null ? searchModel.getDescription() : propertyDescription);
    TextFields.selectAllOnFocusGained(searchField);

    return searchField;
  }

  /**
   * Creates a combo box based on the values in the given value list attribute
   * @param attribute the attribute
   * @param value the value to bind to the field
   * @param <T> the attribute type
   * @return a combo box based on the given values
   */
  public <T> SteppedComboBox<Item<T>> createValueListComboBox(final Attribute<T> attribute, final Value<T> value) {
    return createValueListComboBox(attribute, value, Sorted.YES, null);
  }

  /**
   * Creates a combo box based on the values in the given value list attribute
   * @param attribute the attribute
   * @param value the value to bind to the field
   * @param enabledState the state controlling the enabled state of the combo box
   * @param <T> the attribute type
   * @return a combo box based on the given values
   */
  public <T> SteppedComboBox<Item<T>> createValueListComboBox(final Attribute<T> attribute, final Value<T> value,
                                                              final StateObserver enabledState) {
    return createValueListComboBox(attribute, value, Sorted.YES, enabledState);
  }

  /**
   * Creates a combo box based on the values in the given value list attribute
   * @param attribute the attribute
   * @param value the value to bind to the field
   * @param sorted if yes then the items are sorted
   * @param <T> the attribute type
   * @return a combo box based on the given values
   */
  public <T> SteppedComboBox<Item<T>> createValueListComboBox(final Attribute<T> attribute, final Value<T> value,
                                                              final Sorted sorted) {
    return createValueListComboBox(attribute, value, sorted, null);
  }

  /**
   * Creates a combo box based on the values in the given value list attribute.
   * If the attribute is nullable and the value list items do not include a null item,
   * one is added to the combo box model.
   * @param attribute the attribute
   * @param value the value to bind to the field
   * @param sorted if yes then the items are sorted
   * @param enabledState the state controlling the enabled state of the combo box
   * @param <T> the attribute type
   * @return a combo box based on the given values
   */
  public <T> SteppedComboBox<Item<T>> createValueListComboBox(final Attribute<T> attribute, final Value<T> value,
                                                              final Sorted sorted, final StateObserver enabledState) {

    final Property<T> property = entityDefinition.getProperty(attribute);
    if (!(property instanceof ValueListProperty)) {
      throw new IllegalArgumentException("Property based on '" + attribute + "' is not a ValueListProperty");
    }
    final ItemComboBoxModel<T> valueListComboBoxModel = createValueListComboBoxModel((ValueListProperty<T>) property, sorted);
    requireNonNull(attribute, ATTRIBUTE_PARAM_NAME);
    requireNonNull(value, VALUE_PARAM_NAME);
    final SteppedComboBox<Item<T>> comboBox = new SteppedComboBox<>(valueListComboBoxModel);
    ComponentValues.itemComboBox(comboBox).link(value);
    linkToEnabledState(enabledState, comboBox);
    comboBox.setToolTipText(property.getDescription());
    addComboBoxCompletion(comboBox);

    return comboBox;
  }

  /**
   * Creates a combo box based on the given combo box model
   * @param attribute the attribute
   * @param value the value to bind to the field
   * @param comboBoxModel the combo box model
   * @param <T> the attribute type
   * @return a combo box based on the given model
   */
  public <T> SteppedComboBox<T> createComboBox(final Attribute<T> attribute, final Value<T> value,
                                               final ComboBoxModel<T> comboBoxModel) {
    return createComboBox(attribute, value, comboBoxModel, null);
  }

  /**
   * Creates a combo box based on the given combo box model
   * @param attribute the attribute
   * @param value the value to bind to the field
   * @param comboBoxModel the combo box model
   * @param enabledState the state controlling the enabled state of the combo box
   * @param <T> the attribute type
   * @return a combo box based on the given model
   */
  public <T> SteppedComboBox<T> createComboBox(final Attribute<T> attribute, final Value<T> value,
                                               final ComboBoxModel<T> comboBoxModel, final StateObserver enabledState) {
    return createComboBox(attribute, value, comboBoxModel, enabledState, Editable.NO);
  }

  /**
   * Creates a combo box based on the given combo box model
   * @param attribute the attribute
   * @param value the value to bind to the field
   * @param comboBoxModel the combo box model
   * @param enabledState the state controlling the enabled state of the combo box
   * @param editable if yes then the combo box is made editable
   * @param <T> the attribute type
   * @return a combo box based on the given model
   */
  public <T> SteppedComboBox<T> createComboBox(final Attribute<T> attribute, final Value<T> value,
                                               final ComboBoxModel<T> comboBoxModel, final StateObserver enabledState,
                                               final Editable editable) {
    requireNonNull(attribute, ATTRIBUTE_PARAM_NAME);
    requireNonNull(value, VALUE_PARAM_NAME);
    final SteppedComboBox<T> comboBox = new SteppedComboBox<>(comboBoxModel);
    if (editable == Editable.YES && !attribute.isString()) {
      throw new IllegalArgumentException("Editable attribute ComboBox is only implemented for String properties");
    }
    comboBox.setEditable(editable == Editable.YES);
    ComponentValues.comboBox(comboBox).link(value);
    linkToEnabledState(enabledState, comboBox);
    comboBox.setToolTipText(entityDefinition.getProperty(attribute).getDescription());

    return comboBox;
  }

  /**
   * Creates a panel with a date input field and a button for opening a date input dialog
   * @param attribute the attribute
   * @param value the value to bind to the field
   * @param updateOn specifies when the underlying value should be updated
   * @param calendarButton if yes then a button for opening a date input dialog is included (only available for LocalDate)
   * @param <T> the attribute type
   * @return a date input panel
   */
  public <T extends Temporal> TemporalInputPanel<T> createTemporalInputPanel(final Attribute<T> attribute, final Value<T> value,
                                                                             final UpdateOn updateOn, final CalendarButton calendarButton) {
    return createTemporalInputPanel(attribute, value, updateOn, calendarButton, null);
  }

  /**
   * Creates a panel with a date input field and a button for opening a date input dialog (if applicable)
   * @param attribute the attribute
   * @param value the value to bind to the field
   * @param updateOn specifies when the underlying value should be updated
   * @param calendarButton if yes then a button for opening a calendar dialog is included
   * @param enabledState the state controlling the enabled state of the panel
   * @param <T> the attribute type
   * @return a date input panel
   */
  public <T extends Temporal> TemporalInputPanel<T> createTemporalInputPanel(final Attribute<T> attribute, final Value<T> value,
                                                                             final UpdateOn updateOn, final CalendarButton calendarButton,
                                                                             final StateObserver enabledState) {
    requireNonNull(attribute, ATTRIBUTE_PARAM_NAME);
    if (!attribute.isTemporal()) {
      throw new IllegalArgumentException("Property " + attribute + " is not a date or time attribute");
    }

    final Property<T> property = entityDefinition.getProperty(attribute);
    final TemporalField<Temporal> temporalField = (TemporalField<Temporal>) createTextField(attribute, enabledState,
            LocaleDateTimePattern.getMask(property.getDateTimePattern()), ValueContainsLiterals.YES);

    ComponentValues.temporalField(temporalField, updateOn).link((Value<Temporal>) value);

    return (TemporalInputPanel<T>) TemporalInputPanel.builder()
              .temporalField(temporalField)
              .calendarButton(calendarButton == CalendarButton.YES)
              .enabledState(enabledState)
              .build();
  }

  /**
   * Creates a panel with a text field and a button for opening a dialog with a text area
   * @param attribute the attribute
   * @param value the value to bind to the field
   * @param updateOn specifies when the underlying value should be updated
   * @param buttonFocusable if yes then the dialog button is focusable
   * @return a text input panel
   */
  public TextInputPanel createTextInputPanel(final Attribute<String> attribute, final Value<String> value,
                                             final UpdateOn updateOn, final ButtonFocusable buttonFocusable) {
    requireNonNull(attribute, ATTRIBUTE_PARAM_NAME);
    requireNonNull(value, VALUE_PARAM_NAME);
    final Property<?> property = entityDefinition.getProperty(attribute);
    final JTextField field = createTextField(attribute, value, updateOn);
    final TextInputPanel panel = new TextInputPanel(field, property.getCaption(), null, buttonFocusable);
    panel.setMaximumLength(property.getMaximumLength());

    return panel;
  }

  /**
   * Creates a text area based on the given attribute
   * @param attribute the attribute
   * @param value the value to bind to the field
   * @param updateOn specifies when the underlying value should be updated
   * @return a text area
   */
  public JTextArea createTextArea(final Attribute<String> attribute, final Value<String> value, final UpdateOn updateOn) {
    return createTextArea(attribute, value, -1, -1, updateOn);
  }

  /**
   * Creates a text area based on the given attribute
   * @param attribute the attribute
   * @param value the value to bind to the field
   * @param rows the number of rows
   * @param columns the number of columns
   * @param updateOn specifies when the underlying value should be updated
   * @return a text area
   */
  public JTextArea createTextArea(final Attribute<String> attribute, final Value<String> value,
                                  final int rows, final int columns, final UpdateOn updateOn) {
    return createTextArea(attribute, value, rows, columns, updateOn, null);
  }

  /**
   * Creates a text area based on the given attribute
   * @param attribute the attribute
   * @param value the value to bind to the field
   * @param rows the number of rows
   * @param columns the number of columns
   * @param updateOn specifies when the underlying value should be updated
   * @param enabledState a state indicating when the text area should be enabled
   * @return a text area
   */
  public JTextArea createTextArea(final Attribute<String> attribute, final Value<String> value,
                                  final int rows, final int columns, final UpdateOn updateOn,
                                  final StateObserver enabledState) {
    requireNonNull(attribute, ATTRIBUTE_PARAM_NAME);
    requireNonNull(value, VALUE_PARAM_NAME);
    if (!attribute.isString()) {
      throw new IllegalArgumentException("Cannot create a text area for a non-string attribute");
    }

    final JTextArea textArea = rows > 0 && columns > 0 ? new JTextArea(rows, columns) : new JTextArea();
    textArea.setLineWrap(true);
    textArea.setWrapStyleWord(true);
    final Property<?> property = entityDefinition.getProperty(attribute);
    if (property.getMaximumLength() > 0) {
      ((AbstractDocument) textArea.getDocument()).setDocumentFilter(
              parsingDocumentFilter(stringLengthValidator(property.getMaximumLength())));
    }
    linkToEnabledState(enabledState, textArea);

    ComponentValues.textComponent(textArea, null, updateOn).link(value);
    textArea.setToolTipText(property.getDescription());

    return textArea;
  }

  /**
   * Creates a text field based on the given attribute
   * @param attribute the attribute
   * @param value the value to bind to the field
   * @param <T> the attribute type
   * @return a text field for the given attribute
   */
  public <T> JTextField createTextField(final Attribute<T> attribute, final Value<T> value) {
    return createTextField(attribute, value, UpdateOn.KEYSTROKE);
  }

  /**
   * Creates a text field based on the given attribute
   * @param attribute the attribute
   * @param value the value to bind to the field
   * @param updateOn specifies when the underlying value should be updated
   * @param <T> the attribute type
   * @return a text field for the given attribute
   */
  public <T> JTextField createTextField(final Attribute<T> attribute, final Value<T> value, final UpdateOn updateOn) {
    return createTextField(attribute, value, updateOn, null);
  }

  /**
   * Creates a text field based on the given attribute
   * @param attribute the attribute
   * @param value the value to bind to the field
   * @param updateOn specifies when the underlying value should be updated
   * @param enabledState the state controlling the enabled state of the panel
   * @param <T> the attribute type
   * @return a text field for the given attribute
   */
  public <T> JTextField createTextField(final Attribute<T> attribute, final Value<T> value,
                                        final UpdateOn updateOn, final StateObserver enabledState) {
    requireNonNull(attribute, ATTRIBUTE_PARAM_NAME);
    requireNonNull(value, VALUE_PARAM_NAME);
    final Property<?> property = entityDefinition.getProperty(attribute);
    final JTextField textField = createTextField(attribute, enabledState, null, null);
    if (attribute.isString()) {
      ComponentValues.textComponent(textField, property.getFormat(), updateOn).link((Value<String>) value);
    }
    else if (attribute.isCharacter()) {
      ComponentValues.characterTextField(textField, updateOn).link((Value<Character>) value);
    }
    else if (attribute.isInteger()) {
      ComponentValues.integerFieldBuilder()
              .component((IntegerField) textField)
              .updateOn(updateOn)
              .build()
              .link((Value<Integer>) value);
    }
    else if (attribute.isDouble()) {
      ComponentValues.doubleFieldBuilder()
              .component((DoubleField) textField)
              .updateOn(updateOn)
              .build()
              .link((Value<Double>) value);
    }
    else if (attribute.isBigDecimal()) {
      ComponentValues.bigDecimalFieldBuilder()
              .component((BigDecimalField) textField)
              .updateOn(updateOn)
              .build()
              .link((Value<BigDecimal>) value);
    }
    else if (attribute.isLong()) {
      ComponentValues.longFieldBuilder()
              .component((LongField) textField)
              .updateOn(updateOn)
              .build()
              .link((Value<Long>) value);
    }
    else if (attribute.isTemporal()) {
      ComponentValues.temporalField((TemporalField<Temporal>) textField, updateOn).link((Value<Temporal>) value);
    }
    else {
      throw new IllegalArgumentException("Text fields not implemented for attribute type: " + attribute);
    }

    return textField;
  }

  /**
   * Creates a masked text field based on the given String attribute
   * @param attribute the attribute
   * @param value the value to bind to the field
   * @param formatMaskString if specified the resulting text field is a JFormattedField with this mask
   * @param valueContainsLiterals specifies whether or not the value should contain any literal characters
   * associated with a the format mask
   * @return a text field for the given attribute
   */
  public JFormattedTextField createMaskedTextField(final Attribute<String> attribute, final Value<String> value, final String formatMaskString,
                                                   final ValueContainsLiterals valueContainsLiterals) {
    return createMaskedTextField(attribute, value, formatMaskString, valueContainsLiterals, UpdateOn.KEYSTROKE);
  }

  /**
   * Creates a masked text field based on the given String attribute
   * @param attribute the attribute
   * @param value the value to bind to the field
   * @param formatMaskString if specified the resulting text field is a JFormattedField with this mask
   * @param valueContainsLiterals specifies whether or not the value should contain any literal characters
   * associated with a the format mask
   * @param updateOn specifies when the underlying value should be updated
   * @return a text field for the given attribute
   */
  public JFormattedTextField createMaskedTextField(final Attribute<String> attribute, final Value<String> value, final String formatMaskString,
                                                   final ValueContainsLiterals valueContainsLiterals, final UpdateOn updateOn) {
    return createMaskedTextField(attribute, value, formatMaskString, valueContainsLiterals, updateOn, null);
  }

  /**
   * Creates a masked text field based on the given String attribute
   * @param attribute the attribute
   * @param value the value to bind to the field
   * @param formatMaskString if specified the resulting text field is a JFormattedField with this mask
   * @param valueContainsLiterals specifies whether or not the value should contain any literal characters
   * associated with a the format mask
   * @param updateOn specifies when the underlying value should be updated
   * @param enabledState the state controlling the enabled state of the panel
   * @return a text field for the given attribute
   */
  public JFormattedTextField createMaskedTextField(final Attribute<String> attribute, final Value<String> value, final String formatMaskString,
                                                   final ValueContainsLiterals valueContainsLiterals, final UpdateOn updateOn,
                                                   final StateObserver enabledState) {
    final JFormattedTextField textField = (JFormattedTextField) createTextField(attribute, enabledState, formatMaskString, valueContainsLiterals);
    ComponentValues.textComponent(textField, null, updateOn).link(value);

    return textField;
  }

  /**
   * Creates a panel containing an EntityComboBox and a button for filtering that combo box based on a foreign key
   * @param entityComboBox the combo box
   * @param foreignKey the foreign key to base the filtering on
   * @param filterButtonFocusable if true then the filter button is focusable
   * @return a panel with a combo box and a button
   */
  public static JPanel createEntityComboBoxFilterPanel(final EntityComboBox entityComboBox, final ForeignKey foreignKey,
                                                       final ButtonFocusable filterButtonFocusable) {
    final Control foreignKeyFilterControl = entityComboBox.createForeignKeyFilterControl(foreignKey);
    if (filterButtonFocusable == ButtonFocusable.YES) {
      return Components.createEastFocusableButtonPanel(entityComboBox, foreignKeyFilterControl);
    }

    return Components.createEastButtonPanel(entityComboBox, foreignKeyFilterControl);
  }

  private JTextField createTextField(final Attribute<?> attribute, final StateObserver enabledState,
                                     final String formatMaskString, final ValueContainsLiterals valueContainsLiterals) {
    final Property<?> property = entityDefinition.getProperty(attribute);
    final JTextField field = createTextField(property, formatMaskString, valueContainsLiterals);
    linkToEnabledState(enabledState, field);
    field.setToolTipText(property.getDescription());
    if (field.getDocument() instanceof SizedDocument) {
      if (attribute.isCharacter()) {
        ((SizedDocument) field.getDocument()).setMaximumLength(1);
      }
      else if (property.getMaximumLength() > 0) {
        ((SizedDocument) field.getDocument()).setMaximumLength(property.getMaximumLength());
      }
    }

    return field;
  }

  private static JTextField createTextField(final Property<?> property, final String formatMaskString,
                                            final ValueContainsLiterals valueContainsLiterals) {
    final Attribute<?> attribute = property.getAttribute();
    if (attribute.isInteger()) {
      return initializeIntegerField((Property<Integer>) property);
    }
    else if (attribute.isDouble()) {
      return initializeDoubleField((Property<Double>) property);
    }
    else if (attribute.isBigDecimal()) {
      return initializeBigDecimalField((Property<BigDecimal>) property);
    }
    else if (attribute.isLong()) {
      return initializeLongField((Property<Long>) property);
    }
    else if (attribute.isTemporal()) {
      return new TemporalField<>((Class<Temporal>) attribute.getTypeClass(), property.getDateTimePattern());
    }
    else if (attribute.isString()) {
      return initializeStringField(formatMaskString, valueContainsLiterals);
    }
    else if (attribute.isCharacter()) {
      return new JTextField(new SizedDocument(1), "", 1);
    }

    throw new IllegalArgumentException("Creating text fields for type: " + attribute.getTypeClass() + " is not implemented (" + property + ")");
  }

  private static JTextField initializeStringField(final String formatMaskString, final ValueContainsLiterals valueContainsLiterals) {
    if (formatMaskString == null) {
      return new JTextField(new SizedDocument(), "", 0);
    }

    return TextFields.createFormattedField(formatMaskString, valueContainsLiterals);
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

  private static JCheckBox initializeCheckBox(final Property<Boolean> property, final Value<Boolean> value,
                                              final StateObserver enabledState, final JCheckBox checkBox) {
    ComponentValues.toggleButton(checkBox).link(value);
    linkToEnabledState(enabledState, checkBox);
    checkBox.setToolTipText(property.getDescription());

    return checkBox;
  }

  private static <T> ItemComboBoxModel<T> createValueListComboBoxModel(final ValueListProperty<T> property, final Sorted sorted) {
    final ItemComboBoxModel<T> model = sorted == Sorted.YES ?
            new ItemComboBoxModel<>(property.getValues()) : new ItemComboBoxModel<>(null, property.getValues());
    final Item<T> nullItem = Item.item(null, FilteredComboBoxModel.COMBO_BOX_NULL_VALUE_ITEM.get());
    if (property.isNullable() && !model.containsItem(nullItem)) {
      model.addItem(nullItem);
      model.setSelectedItem(nullItem);
    }

    return model;
  }

  private static void linkToEnabledState(final StateObserver enabledState, final JComponent component) {
    if (enabledState != null) {
      Components.linkToEnabledState(enabledState, component);
    }
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

  private static void addComboBoxCompletion(final JComboBox<?> comboBox) {
    final String completionMode = COMBO_BOX_COMPLETION_MODE.get();
    switch (completionMode) {
      case COMPLETION_MODE_NONE:
        break;
      case COMPLETION_MODE_AUTOCOMPLETE:
        Completion.autoComplete(comboBox);
        break;
      case COMPLETION_MODE_MAXIMUM_MATCH:
        Completion.maximumMatch(comboBox);
        break;
      default:
        throw new IllegalArgumentException("Unknown completion mode: " + completionMode);
    }
  }

  private static final class SearchUIValue extends AbstractValue<Entity> {
    private final EntitySearchModel searchModel;

    private SearchUIValue(final EntitySearchModel searchModel) {
      this.searchModel = searchModel;
      this.searchModel.addSelectedEntitiesListener(selected -> notifyValueChange());
    }

    @Override
    public Entity get() {
      final List<Entity> selectedEntities = searchModel.getSelectedEntities();
      return selectedEntities.isEmpty() ? null : selectedEntities.iterator().next();
    }

    @Override
    protected void setValue(final Entity value) {
      searchModel.setSelectedEntity(value);
    }
  }
}
