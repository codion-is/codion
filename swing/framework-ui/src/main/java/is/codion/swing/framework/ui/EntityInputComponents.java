/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.ui;

import is.codion.common.Configuration;
import is.codion.common.DateFormats;
import is.codion.common.item.Item;
import is.codion.common.item.Items;
import is.codion.common.state.StateObserver;
import is.codion.common.value.AbstractValue;
import is.codion.common.value.Nullable;
import is.codion.common.value.PropertyValue;
import is.codion.common.value.Value;
import is.codion.framework.domain.attribute.EntityAttribute;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.property.ColumnProperty;
import is.codion.framework.domain.property.ForeignKeyProperty;
import is.codion.framework.domain.property.Property;
import is.codion.framework.domain.property.ValueListProperty;
import is.codion.framework.model.EntityEditModel;
import is.codion.framework.model.EntityLookupModel;
import is.codion.swing.common.model.checkbox.NullableToggleButtonModel;
import is.codion.swing.common.model.combobox.BooleanComboBoxModel;
import is.codion.swing.common.model.combobox.ItemComboBoxModel;
import is.codion.swing.common.ui.Components;
import is.codion.swing.common.ui.checkbox.NullableCheckBox;
import is.codion.swing.common.ui.combobox.AutoCompletion;
import is.codion.swing.common.ui.combobox.MaximumMatch;
import is.codion.swing.common.ui.combobox.SteppedComboBox;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.textfield.DecimalField;
import is.codion.swing.common.ui.textfield.IntegerField;
import is.codion.swing.common.ui.textfield.LengthDocumentFilter;
import is.codion.swing.common.ui.textfield.LongField;
import is.codion.swing.common.ui.textfield.SizedDocument;
import is.codion.swing.common.ui.textfield.TextFields;
import is.codion.swing.common.ui.textfield.TextFields.ValueContainsLiterals;
import is.codion.swing.common.ui.textfield.TextInputPanel;
import is.codion.swing.common.ui.textfield.TextInputPanel.ButtonFocusable;
import is.codion.swing.common.ui.time.LocalDateInputPanel;
import is.codion.swing.common.ui.time.LocalDateTimeInputPanel;
import is.codion.swing.common.ui.time.LocalTimeInputPanel;
import is.codion.swing.common.ui.time.TemporalInputPanel;
import is.codion.swing.common.ui.time.TemporalInputPanel.CalendarButton;
import is.codion.swing.common.ui.value.BooleanValues;
import is.codion.swing.common.ui.value.NumericalValues;
import is.codion.swing.common.ui.value.SelectedValues;
import is.codion.swing.common.ui.value.TemporalValues;
import is.codion.swing.common.ui.value.TextValues;
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
import java.sql.Types;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.time.temporal.Temporal;
import java.util.List;

import static java.util.Objects.requireNonNull;

/**
 * Provides input components for editing entities.
 */
public final class EntityInputComponents {

  /**
   * Identifies the completion mode MaximumMatch
   * @see EntityInputComponents#COMBO_BOX_COMPLETION_MODE
   */
  public static final String COMPLETION_MODE_MAXIMUM_MATCH = "max";

  /**
   * Specifies whether maximum match or autocomplete is used for comboboxes,
   * {@link #COMPLETION_MODE_MAXIMUM_MATCH} for {@link MaximumMatch}
   * and {@link #COMPLETION_MODE_AUTOCOMPLETE} for {@link AutoCompletion}.<br>
   * Value type:String<br>
   * Default value: {@link #COMPLETION_MODE_MAXIMUM_MATCH}
   */
  public static final PropertyValue<String> COMBO_BOX_COMPLETION_MODE = Configuration.stringValue("codion.swing.comboBoxCompletionMode", COMPLETION_MODE_MAXIMUM_MATCH);

  /**
   * Identifies the completion mode AutoCompletion
   * @see EntityInputComponents#COMBO_BOX_COMPLETION_MODE
   */
  public static final String COMPLETION_MODE_AUTOCOMPLETE = "auto";
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

  private static final String PROPERTY_PARAM_NAME = "property";
  private static final String VALUE_PARAM_NAME = "value";
  private static final String FOREIGN_KEY_PROPERTY_PARAM_NAME = "foreignKeyProperty";
  private static final int BOOLEAN_COMBO_BOX_POPUP_WIDTH = 40;

  private EntityInputComponents() {}

  /**
   * @param property the property for which to create the input component
   * @param value the value to bind to the field
   * @param <T> the property type
   * @return the component handling input for {@code property}
   */
  public static <T> JComponent createInputComponent(final Property<T> property, final Value<T> value) {
    return createInputComponent(property, value, null);
  }

  /**
   * @param property the property for which to create the input component
   * @param value the value to bind to the field
   * @param enabledState the enabled state
   * @param <T> the property type
   * @return the component handling input for {@code property}
   */
  public static <T> JComponent createInputComponent(final Property<T> property, final Value<T> value,
                                                    final StateObserver enabledState) {
    if (property instanceof ForeignKeyProperty) {
      throw new IllegalArgumentException("Use createForeignKeyComboBox() or createForeignKeyLookupField() for ForeignKeyProperties");
    }
    if (property instanceof ValueListProperty) {
      return createValueListComboBox((ValueListProperty<T>) property, value, enabledState);
    }
    switch (property.getType()) {
      case Types.BOOLEAN:
        return property.isNullable() ?
                createNullableCheckBox((Property<Boolean>) property, (Value<Boolean>) value, enabledState, IncludeCaption.NO) :
                createCheckBox((Property<Boolean>) property, (Value<Boolean>) value, enabledState, IncludeCaption.NO);
      case Types.DATE:
      case Types.TIMESTAMP:
      case Types.TIME:
      case Types.DOUBLE:
      case Types.DECIMAL:
      case Types.INTEGER:
      case Types.BIGINT:
      case Types.CHAR:
      case Types.VARCHAR:
        return createTextField(property, value, null, UpdateOn.KEYSTROKE, enabledState);
      case Types.BLOB:
      default:
        throw new IllegalArgumentException("No input component available for property: " +
                property + " (type: " + property.getType() + ")");
    }
  }

  /**
   * Creates a JLabel with a caption from the given property, using the default label text alignment
   * @param property the property for which to create the label
   * @return a JLabel for the given property
   * @see EntityInputComponents#LABEL_TEXT_ALIGNMENT
   */
  public static JLabel createLabel(final Property<?> property) {
    return createLabel(property, LABEL_TEXT_ALIGNMENT.get());
  }

  /**
   * Creates a JLabel with a caption from the given property
   * @param property the property for which to create the label
   * @param horizontalAlignment the horizontal text alignment
   * @return a JLabel for the given property
   */
  public static JLabel createLabel(final Property<?> property, final int horizontalAlignment) {
    requireNonNull(property, PROPERTY_PARAM_NAME);
    final JLabel label = new JLabel(property.getCaption(), horizontalAlignment);
    if (property.getMnemonic() != null) {
      label.setDisplayedMnemonic(property.getMnemonic());
    }

    return label;
  }

  /**
   * Creates a JCheckBox based on the given boolean property
   * @param property the property on which value to base the checkbox
   * @param value the value to bind to the field
   * @return a check box based on the given property
   * @throws IllegalArgumentException in case the property is not a boolean property
   */
  public static JCheckBox createCheckBox(final Property<Boolean> property, final Value<Boolean> value) {
    return createCheckBox(property, value, null);
  }

  /**
   * Creates a JCheckBox based on the given boolean property
   * @param property the property on which value to base the checkbox
   * @param value the value to bind to the field
   * @param enabledState the state controlling the enabled state of the checkbox
   * @return a check box based on the given property
   * @throws IllegalArgumentException in case the property is not a boolean property
   */
  public static JCheckBox createCheckBox(final Property<Boolean> property, final Value<Boolean> value,
                                         final StateObserver enabledState) {
    return createCheckBox(property, value, enabledState, IncludeCaption.YES);
  }

  /**
   * Creates a JCheckBox based on the given boolean property
   * @param property the property on which value to base the checkbox
   * @param value the value to bind to the field
   * @param enabledState the state controlling the enabled state of the checkbox
   * @param includeCaption if yes then the property caption is included as the checkbox text
   * @return a check box based on the given property
   * @throws IllegalArgumentException in case the property is not a boolean property
   */
  public static JCheckBox createCheckBox(final Property<Boolean> property, final Value<Boolean> value, final StateObserver enabledState,
                                         final IncludeCaption includeCaption) {
    requireNonNull(property, PROPERTY_PARAM_NAME);
    requireNonNull(value, VALUE_PARAM_NAME);
    if (!property.getAttribute().isBoolean()) {
      throw new IllegalArgumentException("Boolean property required for createCheckBox");
    }

    return initializeCheckBox(property, value, enabledState,
            includeCaption == IncludeCaption.YES ? new JCheckBox(property.getCaption()) : new JCheckBox());
  }

  /**
   * Creates a NullableCheckBox based on the given boolean property
   * @param property the property on which value to base the checkbox
   * @param value the value to bind to the field
   * @param enabledState the state controlling the enabled state of the checkbox
   * @param includeCaption if yes then the property caption is included as the checkbox text
   * @return a check box based on the given property
   * @throws IllegalArgumentException in case the property is not a nullable boolean property
   */
  public static NullableCheckBox createNullableCheckBox(final Property<Boolean> property, final Value<Boolean> value, final StateObserver enabledState,
                                                        final IncludeCaption includeCaption) {
    requireNonNull(property, PROPERTY_PARAM_NAME);
    requireNonNull(value, VALUE_PARAM_NAME);
    if (!property.getAttribute().isBoolean() || !property.isNullable()) {
      throw new IllegalArgumentException("Nullable boolean property required for createNullableCheckBox");
    }

    return (NullableCheckBox) initializeCheckBox(property, value, enabledState,
            new NullableCheckBox(new NullableToggleButtonModel(), includeCaption == IncludeCaption.YES ? property.getCaption() : null));
  }

  /**
   * Creates a combobox containing the values (null, yes, no) based on the given boolean property
   * @param property the property on which to base the combobox
   * @param value the value to bind to the field
   * @return a SteppedComboBox based on the given boolean property
   */
  public static SteppedComboBox<Item<Boolean>> createBooleanComboBox(final Property<Boolean> property, final Value<Boolean> value) {
    return createBooleanComboBox(property, value, null);
  }

  /**
   * Creates a combobox containing the values (null, yes, no) based on the given boolean property
   * @param property the property on which to base the combobox
   * @param value the value to bind to the field
   * @param enabledState the state controlling the enabled state of the combobox
   * @return a SteppedComboBox based on the given boolean property
   */
  public static SteppedComboBox<Item<Boolean>> createBooleanComboBox(final Property<Boolean> property, final Value<Boolean> value,
                                                                     final StateObserver enabledState) {
    final SteppedComboBox<Item<Boolean>> box = createComboBox(property, value, new BooleanComboBoxModel(), enabledState);
    box.setPopupWidth(BOOLEAN_COMBO_BOX_POPUP_WIDTH);

    return box;
  }

  /**
   * Creates EntityComboBox based on the given foreign key property
   * @param foreignKeyProperty the foreign key property on which entity to base the combobox
   * @param value the value to bind to the field
   * @param comboBoxModel the combo box model
   * @return a SwingEntityComboBox based on the given foreign key property
   */
  public static EntityComboBox createForeignKeyComboBox(final ForeignKeyProperty foreignKeyProperty,
                                                        final Value<Entity> value, final SwingEntityComboBoxModel comboBoxModel) {
    return createForeignKeyComboBox(foreignKeyProperty, value, comboBoxModel, null);
  }

  /**
   * Creates EntityComboBox based on the given foreign key property
   * @param foreignKeyProperty the foreign key property on which entity to base the combobox
   * @param value the value to bind to the field
   * @param comboBoxModel the combo box model
   * @param enabledState the state controlling the enabled state of the combobox
   * @return a EntityComboBox based on the given foreign key property
   */
  public static EntityComboBox createForeignKeyComboBox(final ForeignKeyProperty foreignKeyProperty,
                                                        final Value<Entity> value, final SwingEntityComboBoxModel comboBoxModel,
                                                        final StateObserver enabledState) {
    requireNonNull(foreignKeyProperty, FOREIGN_KEY_PROPERTY_PARAM_NAME);
    requireNonNull(comboBoxModel, "comboBoxModel");
    comboBoxModel.refresh();
    final EntityComboBox comboBox = new EntityComboBox(comboBoxModel);
    value.link(SelectedValues.selectedValue(comboBox));
    linkToEnabledState(enabledState, comboBox);
    addComboBoxCompletion(comboBox);
    comboBox.setToolTipText(foreignKeyProperty.getDescription());

    return comboBox;
  }

  /**
   * Creates a EntityLookupField based on the given foreign key property
   * @param foreignKeyProperty the foreign key property on which entity to base the lookup model
   * @param value the value to bind to the field
   * @param lookupModel the {@link EntityLookupModel} to use and to bind with the value
   * @return a lookup model based on the given foreign key property
   */
  public static EntityLookupField createForeignKeyLookupField(final ForeignKeyProperty foreignKeyProperty,
                                                              final Value<Entity> value, final EntityLookupModel lookupModel) {
    return createForeignKeyLookupField(foreignKeyProperty, value, lookupModel, null);
  }

  /**
   * Creates a EntityLookupField based on the given foreign key property
   * @param foreignKeyProperty the foreign key property on which entity to base the lookup model
   * @param value the value to bind to the field
   * @param lookupModel the {@link EntityLookupModel} to use and to bind with the value
   * @param enabledState the state controlling the enabled state of the lookup field
   * @return a lookup model based on the given foreign key property
   */
  public static EntityLookupField createForeignKeyLookupField(final ForeignKeyProperty foreignKeyProperty, final Value<Entity> value,
                                                              final EntityLookupModel lookupModel, final StateObserver enabledState) {
    requireNonNull(foreignKeyProperty, FOREIGN_KEY_PROPERTY_PARAM_NAME);
    requireNonNull(lookupModel, "lookupModel");
    requireNonNull(value, VALUE_PARAM_NAME);
    final EntityLookupField lookupField = new EntityLookupField(lookupModel);
    value.link(new LookupUIValue(lookupField.getModel()));
    linkToEnabledState(enabledState, lookupField);
    lookupField.setToolTipText(foreignKeyProperty.getDescription());
    TextFields.selectAllOnFocusGained(lookupField);

    return lookupField;
  }

  /**
   * Creates a combo box based on the values in the given value list property
   * @param property the property
   * @param value the value to bind to the field
   * @return a combo box based on the given values
   */
  public static SteppedComboBox createValueListComboBox(final ValueListProperty property, final Value value) {
    return createValueListComboBox(property, value, Sorted.YES, null);
  }

  /**
   * Creates a combo box based on the values in the given value list property
   * @param property the property
   * @param value the value to bind to the field
   * @param enabledState the state controlling the enabled state of the combo box
   * @return a combo box based on the given values
   */
  public static SteppedComboBox createValueListComboBox(final ValueListProperty property, final Value value,
                                                        final StateObserver enabledState) {
    return createValueListComboBox(property, value, Sorted.YES, enabledState);
  }

  /**
   * Creates a combo box based on the values in the given value list property
   * @param property the property
   * @param value the value to bind to the field
   * @param sorted if yes then the items are sorted
   * @return a combo box based on the given values
   */
  public static SteppedComboBox createValueListComboBox(final ValueListProperty property, final Value value,
                                                        final Sorted sorted) {
    return createValueListComboBox(property, value, sorted, null);
  }

  /**
   * Creates a combo box based on the values in the given value list property.
   * If the property is nullable and the value list items do not include a null item,
   * one is added to the combo box model.
   * @param property the property
   * @param value the value to bind to the field
   * @param sorted if yes then the items are sorted
   * @param enabledState the state controlling the enabled state of the combo box
   * @return a combo box based on the given values
   */
  public static SteppedComboBox createValueListComboBox(final ValueListProperty property, final Value value,
                                                        final Sorted sorted, final StateObserver enabledState) {
    final SteppedComboBox comboBox = createComboBox(property, value,
            createValueListComboBoxModel(property, sorted), enabledState);
    addComboBoxCompletion(comboBox);

    return comboBox;
  }

  /**
   * Creates a combo box based on the given combo box model
   * @param property the property
   * @param value the value to bind to the field
   * @param model the combo box model
   * @param enabledState the state controlling the enabled state of the combo box
   * @return a combo box based on the given model
   */
  public static SteppedComboBox createComboBox(final Property property, final Value value,
                                               final ComboBoxModel model, final StateObserver enabledState) {
    return createComboBox(property, value, model, enabledState, Editable.NO);
  }

  /**
   * Creates a combo box based on the given combo box model
   * @param property the property
   * @param value the value to bind to the field
   * @param model the combo box model
   * @param enabledState the state controlling the enabled state of the combo box
   * @param editable if yes then the combo box is made editable
   * @return a combo box based on the given model
   */
  public static SteppedComboBox createComboBox(final Property property, final Value value,
                                               final ComboBoxModel model, final StateObserver enabledState,
                                               final Editable editable) {
    requireNonNull(property, PROPERTY_PARAM_NAME);
    requireNonNull(value, VALUE_PARAM_NAME);
    final SteppedComboBox comboBox = new SteppedComboBox(model);
    if (editable == Editable.YES && !property.getAttribute().isString()) {
      throw new IllegalArgumentException("Editable property ComboBox is only implemented for String properties");
    }
    comboBox.setEditable(editable == Editable.YES);
    value.link(SelectedValues.selectedValue(comboBox));
    linkToEnabledState(enabledState, comboBox);
    comboBox.setToolTipText(property.getDescription());

    return comboBox;
  }

  /**
   * Creates a panel with a date input field and a button for opening a date input dialog
   * @param property the property
   * @param value the value to bind to the field
   * @param updateOn specifies when the underlying value should be updated
   * @param calendarButton if yes then a button for opening a date input dialog is included (only available for LocalDate)
   * @param <T> the property type
   * @return a date input panel
   */
  public static <T extends Temporal> TemporalInputPanel<T> createTemporalInputPanel(final Property<T> property, final Value<T> value,
                                                                                    final UpdateOn updateOn, final CalendarButton calendarButton) {
    return createTemporalInputPanel(property, value, updateOn, calendarButton, null);
  }

  /**
   * Creates a panel with a date input field and a button for opening a date input dialog (if applicable)
   * @param property the property
   * @param value the value to bind to the field
   * @param updateOn specifies when the underlying value should be updated
   * @param calendarButton if yes then a button for opening a calendar dialog is included
   * @param enabledState the state controlling the enabled state of the panel
   * @param <T> the property type
   * @return a date input panel
   */
  public static <T extends Temporal> TemporalInputPanel<T> createTemporalInputPanel(final Property<T> property, final Value<T> value,
                                                                                    final UpdateOn updateOn, final CalendarButton calendarButton,
                                                                                    final StateObserver enabledState) {
    requireNonNull(property, PROPERTY_PARAM_NAME);
    if (!property.getAttribute().isTemporal()) {
      throw new IllegalArgumentException("Property " + property + " is not a date or time property");
    }

    final String formatString = property.getDateTimeFormatPattern();
    final JFormattedTextField field = (JFormattedTextField) createTextField(property, value,
            DateFormats.getDateMask(formatString), updateOn, enabledState);
    if (property.getAttribute().isDate()) {
      return (TemporalInputPanel<T>) new LocalDateInputPanel(field, formatString, calendarButton, enabledState);
    }
    else if (property.getAttribute().isTimestamp()) {
      return (TemporalInputPanel<T>) new LocalDateTimeInputPanel(field, formatString, calendarButton, enabledState);
    }
    else if (property.getAttribute().isTime()) {
      return (TemporalInputPanel<T>) new LocalTimeInputPanel(field, formatString, enabledState);
    }

    throw new IllegalArgumentException("Can not create a date input panel for a non-date property");
  }

  /**
   * Creates a panel with a text field and a button for opening a dialog with a text area
   * @param property the property
   * @param value the value to bind to the field
   * @param updateOn specifies when the underlying value should be updated
   * @param buttonFocusable if yes then the dialog button is focusable
   * @return a text input panel
   */
  public static TextInputPanel createTextInputPanel(final Property property, final Value<String> value,
                                                    final UpdateOn updateOn, final ButtonFocusable buttonFocusable) {
    requireNonNull(property, PROPERTY_PARAM_NAME);
    requireNonNull(value, VALUE_PARAM_NAME);
    final JTextField field = createTextField(property, value, null, updateOn);
    final TextInputPanel panel = new TextInputPanel(field, property.getCaption(), null, buttonFocusable);
    panel.setMaxLength(property.getMaximumLength());

    return panel;
  }

  /**
   * Creates a text area based on the given property
   * @param property the property
   * @param value the value to bind to the field
   * @param updateOn specifies when the underlying value should be updated
   * @return a text area
   */
  public static JTextArea createTextArea(final Property property, final Value<String> value, final UpdateOn updateOn) {
    return createTextArea(property, value, -1, -1, updateOn);
  }

  /**
   * Creates a text area based on the given property
   * @param property the property
   * @param value the value to bind to the field
   * @param rows the number of rows
   * @param columns the number of columns
   * @param updateOn specifies when the underlying value should be updated
   * @return a text area
   */
  public static JTextArea createTextArea(final Property property, final Value<String> value,
                                         final int rows, final int columns, final UpdateOn updateOn) {
    return createTextArea(property, value, rows, columns, updateOn, null);
  }

  /**
   * Creates a text area based on the given property
   * @param property the property
   * @param value the value to bind to the field
   * @param rows the number of rows
   * @param columns the number of columns
   * @param updateOn specifies when the underlying value should be updated
   * @param enabledState a state indicating when the text area should be enabled
   * @return a text area
   */
  public static JTextArea createTextArea(final Property property, final Value<String> value,
                                         final int rows, final int columns, final UpdateOn updateOn,
                                         final StateObserver enabledState) {
    requireNonNull(property, PROPERTY_PARAM_NAME);
    requireNonNull(value, VALUE_PARAM_NAME);
    if (!property.getAttribute().isString()) {
      throw new IllegalArgumentException("Cannot create a text area for a non-string property");
    }

    final JTextArea textArea = rows > 0 && columns > 0 ? new JTextArea(rows, columns) : new JTextArea();
    textArea.setLineWrap(true);
    textArea.setWrapStyleWord(true);
    if (property.getMaximumLength() > 0) {
      ((AbstractDocument) textArea.getDocument()).setDocumentFilter(new LengthDocumentFilter(property.getMaximumLength()));
    }
    linkToEnabledState(enabledState, textArea);

    value.link(TextValues.textValue(textArea, null, updateOn));
    textArea.setToolTipText(property.getDescription());

    return textArea;
  }

  /**
   * Creates a text field based on the given property
   * @param property the property
   * @param value the value to bind to the field
   * @return a text field for the given property
   */
  public static JTextField createTextField(final Property property, final Value value) {
    return createTextField(property, value, null, UpdateOn.KEYSTROKE);
  }

  /**
   * Creates a text field based on the given property
   * @param property the property
   * @param value the value to bind to the field
   * @param formatMaskString if specified the resulting text field is a JFormattedField with this mask
   * @param updateOn specifies when the underlying value should be updated
   * @return a text field for the given property
   */
  public static JTextField createTextField(final Property property, final Value value,
                                           final String formatMaskString, final UpdateOn updateOn) {
    return createTextField(property, value, formatMaskString, updateOn, null);
  }

  /**
   * Creates a text field based on the given property
   * @param property the property
   * @param value the value to bind to the field
   * @param formatMaskString if specified the resulting text field is a JFormattedField with this mask
   * @param updateOn specifies when the underlying value should be updated
   * @param enabledState the state controlling the enabled state of the panel
   * @return a text field for the given property
   */
  public static JTextField createTextField(final Property property, final Value value,
                                           final String formatMaskString, final UpdateOn updateOn,
                                           final StateObserver enabledState) {
    return createTextField(property, value, formatMaskString, updateOn, enabledState, ValueContainsLiterals.NO);
  }

  /**
   * Creates a text field based on the given property
   * @param property the property
   * @param value the value to bind to the field
   * @param formatMaskString if specified the resulting text field is a JFormattedField with this mask
   * @param updateOn specifies when the underlying value should be updated
   * @param enabledState the state controlling the enabled state of the panel
   * @param valueContainsLiterals specifies whether or not the value should contain any literal characters
   * associated with a the format mask
   * @return a text field for the given property
   */
  public static JTextField createTextField(final Property property, final Value value, final String formatMaskString,
                                           final UpdateOn updateOn, final StateObserver enabledState,
                                           final ValueContainsLiterals valueContainsLiterals) {
    requireNonNull(property, PROPERTY_PARAM_NAME);
    requireNonNull(value, VALUE_PARAM_NAME);
    final JTextField textField = createTextField(property, enabledState, formatMaskString, valueContainsLiterals);
    if (property.getAttribute().isString()) {
      value.link(TextValues.textValue(textField, property.getFormat(), updateOn));
    }
    else if (property.getAttribute().isInteger()) {
      value.link(NumericalValues.integerValue((IntegerField) textField, Nullable.YES, updateOn));
    }
    else if (property.getAttribute().isDouble()) {
      value.link(NumericalValues.doubleValue((DecimalField) textField, Nullable.YES, updateOn));
    }
    else if (property.getAttribute().isBigDecimal()) {
      value.link(NumericalValues.bigDecimalValue((DecimalField) textField, updateOn));
    }
    else if (property.getAttribute().isLong()) {
      value.link(NumericalValues.longValue((LongField) textField, Nullable.YES, updateOn));
    }
    else if (property.getAttribute().isDate()) {
      value.link(TemporalValues.localDateValue((JFormattedTextField) textField, property.getDateTimeFormatPattern(), updateOn));
    }
    else if (property.getAttribute().isTime()) {
      value.link(TemporalValues.localTimeValue((JFormattedTextField) textField, property.getDateTimeFormatPattern(), updateOn));
    }
    else if (property.getAttribute().isTimestamp()) {
      value.link(TemporalValues.localDateTimeValue((JFormattedTextField) textField, property.getDateTimeFormatPattern(), updateOn));
    }
    else {
      throw new IllegalArgumentException("Not a text based property: " + property);
    }

    return textField;
  }

  /**
   * Creates a combo box based on the values of the given property
   * @param property the property
   * @param value the value to bind to the field
   * @param comboBoxModel the combo box model
   * @param enabledState the state controlling the enabled state of the panel
   * @param <T> the property type
   * @return a combo box based on the property values
   */
  public static <T> SteppedComboBox<T> createPropertyComboBox(final ColumnProperty<T> property, final Value<T> value,
                                                              final ComboBoxModel<T> comboBoxModel, final StateObserver enabledState) {
    return createPropertyComboBox(property, value, comboBoxModel, enabledState, Editable.NO);
  }

  /**
   * Creates a combo box based on the values of the given property
   * @param property the property
   * @param value the value to bind to the field
   * @param comboBoxModel the combo box model
   * @param enabledState the state controlling the enabled state of the panel
   * @param editable if yes then the combo box will be editable
   * @param <T> the property type
   * @return a combo box based on the property values
   */
  public static <T> SteppedComboBox<T> createPropertyComboBox(final ColumnProperty<T> property, final Value<T> value,
                                                              final ComboBoxModel<T> comboBoxModel, final StateObserver enabledState,
                                                              final Editable editable) {
    final SteppedComboBox<T> comboBox = createComboBox(property, value, comboBoxModel, enabledState, editable);
    if (editable == Editable.NO) {
      addComboBoxCompletion(comboBox);
    }

    return comboBox;
  }

  /**
   * Creates a panel containing an EntityComboBox and a button for filtering that combo box based on a foreign key
   * @param entityComboBox the combo box
   * @param foreignKeyAttribute the foreign key to base the filtering on
   * @param filterButtonTakesFocus if true then the filter button is focusable
   * @return a panel with a combo box and a button
   */
  public static JPanel createEntityComboBoxFilterPanel(final EntityComboBox entityComboBox, final EntityAttribute foreignKeyAttribute,
                                                       final boolean filterButtonTakesFocus) {
    final Control foreignKeyFilterControl = entityComboBox.createForeignKeyFilterControl(foreignKeyAttribute);
    if (filterButtonTakesFocus) {
      return Components.createEastFocusableButtonPanel(entityComboBox, foreignKeyFilterControl);
    }

    return Components.createEastButtonPanel(entityComboBox, foreignKeyFilterControl);
  }

  private static JTextField createTextField(final Property<?> property, final StateObserver enabledState,
                                            final String formatMaskString, final ValueContainsLiterals valueContainsLiterals) {
    final JTextField field = createTextField(property, formatMaskString, valueContainsLiterals);
    linkToEnabledState(enabledState, field);
    field.setToolTipText(property.getDescription());
    if (property.getMaximumLength() > 0 && field.getDocument() instanceof SizedDocument) {
      ((SizedDocument) field.getDocument()).setMaxLength(property.getMaximumLength());
    }

    return field;
  }

  private static JTextField createTextField(final Property<?> property, final String formatMaskString,
                                            final ValueContainsLiterals valueContainsLiterals) {
    if (property.getAttribute().isInteger()) {
      return initializeIntField(property);
    }
    else if (property.getAttribute().isDecimal()) {
      return initializeDecimalField(property);
    }
    else if (property.getAttribute().isLong()) {
      return initializeLongField(property);
    }
    else if (property.getAttribute().isTemporal()) {
      return TextFields.createFormattedField(DateFormats.getDateMask(property.getDateTimeFormatPattern()));
    }
    else if (property.getAttribute().isString()) {
      return initializeStringField(formatMaskString, valueContainsLiterals);
    }

    throw new IllegalArgumentException("Creating text fields for property type: " + property.getType() + " is not implemented");
  }

  private static JTextField initializeStringField(final String formatMaskString, final ValueContainsLiterals valueContainsLiterals) {
    if (formatMaskString == null) {
      return new JTextField(new SizedDocument(), "", 0);
    }

    return TextFields.createFormattedField(formatMaskString, valueContainsLiterals);
  }

  private static JTextField initializeDecimalField(final Property<?> property) {
    final DecimalField field = new DecimalField((DecimalFormat) cloneFormat((NumberFormat) property.getFormat()));
    if (property.getMinimumValue() != null && property.getMaximumValue() != null) {
      field.setRange(Math.min(property.getMinimumValue(), 0), property.getMaximumValue());
    }

    return field;
  }

  private static JTextField initializeIntField(final Property<?> property) {
    final IntegerField field = new IntegerField(cloneFormat((NumberFormat) property.getFormat()));
    if (property.getMinimumValue() != null && property.getMaximumValue() != null) {
      field.setRange(property.getMinimumValue(), property.getMaximumValue());
    }

    return field;
  }

  private static JTextField initializeLongField(final Property<?> property) {
    final LongField field = new LongField(cloneFormat((NumberFormat) property.getFormat()));
    if (property.getMinimumValue() != null && property.getMaximumValue() != null) {
      field.setRange(property.getMinimumValue(), property.getMaximumValue());
    }

    return field;
  }

  private static JCheckBox initializeCheckBox(final Property<?> property, final Value<Boolean> value,
                                              final StateObserver enabledState, final JCheckBox checkBox) {
    value.link(BooleanValues.booleanButtonModelValue(checkBox.getModel()));
    linkToEnabledState(enabledState, checkBox);
    checkBox.setToolTipText(property.getDescription());

    return checkBox;
  }

  private static <T> ItemComboBoxModel<T> createValueListComboBoxModel(final ValueListProperty<T> property, final Sorted sorted) {
    final ItemComboBoxModel<T> model = sorted == Sorted.YES ?
            new ItemComboBoxModel<>(property.getValues()) : new ItemComboBoxModel<>(null, property.getValues());
    if (property.isNullable() && !model.containsItem(Items.item(null))) {
      model.addItem(Items.item(null, EntityEditModel.COMBO_BOX_NULL_VALUE_ITEM.get()));
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
      case COMPLETION_MODE_AUTOCOMPLETE:
        AutoCompletion.enable(comboBox);
        break;
      case COMPLETION_MODE_MAXIMUM_MATCH:
        MaximumMatch.enable(comboBox);
        break;
      default:
        throw new IllegalArgumentException("Unknown completion mode: " + completionMode);
    }
  }

  private static final class LookupUIValue extends AbstractValue<Entity> {
    private final EntityLookupModel lookupModel;

    private LookupUIValue(final EntityLookupModel lookupModel) {
      this.lookupModel = lookupModel;
      this.lookupModel.addSelectedEntitiesListener(selected -> notifyValueChange());
    }

    @Override
    public void set(final Entity value) {
      lookupModel.setSelectedEntity(value);
    }

    @Override
    public Entity get() {
      final List<Entity> selectedEntities = lookupModel.getSelectedEntities();
      return selectedEntities.isEmpty() ? null : selectedEntities.iterator().next();
    }

    @Override
    public boolean isNullable() {
      return true;
    }
  }
}
