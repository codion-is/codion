/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.framework.ui;

import org.jminor.common.Configuration;
import org.jminor.common.DateFormats;
import org.jminor.common.state.StateObserver;
import org.jminor.common.value.AbstractValue;
import org.jminor.common.value.PropertyValue;
import org.jminor.common.value.Value;
import org.jminor.common.value.Values;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.property.ColumnProperty;
import org.jminor.framework.domain.property.ForeignKeyProperty;
import org.jminor.framework.domain.property.Property;
import org.jminor.framework.domain.property.ValueListProperty;
import org.jminor.framework.model.EntityLookupModel;
import org.jminor.swing.common.model.checkbox.NullableToggleButtonModel;
import org.jminor.swing.common.model.combobox.BooleanComboBoxModel;
import org.jminor.swing.common.model.combobox.ItemComboBoxModel;
import org.jminor.swing.common.ui.LocalDateInputPanel;
import org.jminor.swing.common.ui.LocalDateTimeInputPanel;
import org.jminor.swing.common.ui.LocalTimeInputPanel;
import org.jminor.swing.common.ui.TemporalInputPanel;
import org.jminor.swing.common.ui.TextInputPanel;
import org.jminor.swing.common.ui.UiUtil;
import org.jminor.swing.common.ui.ValueLinks;
import org.jminor.swing.common.ui.checkbox.NullableCheckBox;
import org.jminor.swing.common.ui.combobox.AutoCompletion;
import org.jminor.swing.common.ui.combobox.MaximumMatch;
import org.jminor.swing.common.ui.combobox.SteppedComboBox;
import org.jminor.swing.common.ui.textfield.DecimalField;
import org.jminor.swing.common.ui.textfield.DocumentSizeFilter;
import org.jminor.swing.common.ui.textfield.IntegerField;
import org.jminor.swing.common.ui.textfield.LongField;
import org.jminor.swing.common.ui.textfield.SizedDocument;
import org.jminor.swing.framework.model.SwingEntityComboBoxModel;

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
  public static final PropertyValue<String> COMBO_BOX_COMPLETION_MODE = Configuration.stringValue("jminor.swing.comboBoxCompletionMode", COMPLETION_MODE_MAXIMUM_MATCH);

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
  public static final PropertyValue<Integer> LABEL_TEXT_ALIGNMENT = Configuration.integerValue("jminor.swing.labelTextAlignment", JLabel.LEFT);
  private static final String PROPERTY_PARAM_NAME = "property";
  private static final String VALUE_PARAM_NAME = "value";
  private static final String FOREIGN_KEY_PROPERTY_PARAM_NAME = "foreignKeyProperty";
  private static final int BOOLEAN_COMBO_BOX_POPUP_WIDTH = 40;

  private EntityInputComponents() {}

  /**
   * @param property the property for which to create the input component
   * @param value the value to bind to the field
   * @return the component handling input for {@code property}
   */
  public static JComponent createInputComponent(final Property property, final Value value) {
    return createInputComponent(property, value, null);
  }

  /**
   * @param property the property for which to create the input component
   * @param value the value to bind to the field
   * @param enabledState the enabled state
   * @return the component handling input for {@code property}
   */
  public static JComponent createInputComponent(final Property property, final Value value,
                                                final StateObserver enabledState) {
    if (property instanceof ForeignKeyProperty) {
      throw new IllegalArgumentException("Use createForeignKeyComboBox() or createForeignKeyLookupField() for ForeignKeyProperties");
    }
    if (property instanceof ValueListProperty) {
      return createValueListComboBox((ValueListProperty) property, value, enabledState);
    }
    switch (property.getType()) {
      case Types.BOOLEAN:
        return property.isNullable() ?
                createNullableCheckBox(property, value, enabledState, false) :
                createCheckBox(property, value, enabledState, false);
      case Types.DATE:
      case Types.TIMESTAMP:
      case Types.TIME:
      case Types.DOUBLE:
      case Types.DECIMAL:
      case Types.INTEGER:
      case Types.BIGINT:
      case Types.CHAR:
      case Types.VARCHAR:
        return createTextField(property, value, null, true, enabledState);
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
  public static JLabel createLabel(final Property property) {
    return createLabel(property, LABEL_TEXT_ALIGNMENT.get());
  }

  /**
   * Creates a JLabel with a caption from the given property
   * @param property the property for which to create the label
   * @param horizontalAlignment the horizontal text alignment
   * @return a JLabel for the given property
   */
  public static JLabel createLabel(final Property property, final int horizontalAlignment) {
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
  public static JCheckBox createCheckBox(final Property property, final Value value) {
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
  public static JCheckBox createCheckBox(final Property property, final Value value,
                                         final StateObserver enabledState) {
    return createCheckBox(property, value, enabledState, true);
  }

  /**
   * Creates a JCheckBox based on the given boolean property
   * @param property the property on which value to base the checkbox
   * @param value the value to bind to the field
   * @param enabledState the state controlling the enabled state of the checkbox
   * @param includeCaption if true then the property caption is included as the checkbox text
   * @return a check box based on the given property
   * @throws IllegalArgumentException in case the property is not a boolean property
   */
  public static JCheckBox createCheckBox(final Property property, final Value value,
                                         final StateObserver enabledState, final boolean includeCaption) {
    requireNonNull(property, PROPERTY_PARAM_NAME);
    requireNonNull(value, VALUE_PARAM_NAME);
    if (!property.isBoolean()) {
      throw new IllegalArgumentException("Boolean property required for createCheckBox");
    }

    return initializeCheckBox(property, value, enabledState,
            includeCaption ? new JCheckBox(property.getCaption()) : new JCheckBox());
  }

  /**
   * Creates a NullableCheckBox based on the given boolean property
   * @param property the property on which value to base the checkbox
   * @param value the value to bind to the field
   * @param enabledState the state controlling the enabled state of the checkbox
   * @param includeCaption if true then the property caption is included as the checkbox text
   * @return a check box based on the given property
   * @throws IllegalArgumentException in case the property is not a nullable boolean property
   */
  public static NullableCheckBox createNullableCheckBox(final Property property, final Value value,
                                                        final StateObserver enabledState, final boolean includeCaption) {
    requireNonNull(property, PROPERTY_PARAM_NAME);
    requireNonNull(value, VALUE_PARAM_NAME);
    if (!property.isBoolean() || !property.isNullable()) {
      throw new IllegalArgumentException("Nullable boolean property required for createNullableCheckBox");
    }

    return (NullableCheckBox) initializeCheckBox(property, value, enabledState,
            new NullableCheckBox(new NullableToggleButtonModel(), includeCaption ? property.getCaption() : null));
  }

  /**
   * Creates a combobox containing the values (null, yes, no) based on the given boolean property
   * @param property the property on which to base the combobox
   * @param value the value to bind to the field
   * @return a SteppedComboBox based on the given boolean property
   */
  public static SteppedComboBox createBooleanComboBox(final Property property, final Value value) {
    return createBooleanComboBox(property, value, null);
  }

  /**
   * Creates a combobox containing the values (null, yes, no) based on the given boolean property
   * @param property the property on which to base the combobox
   * @param value the value to bind to the field
   * @param enabledState the state controlling the enabled state of the combobox
   * @return a SteppedComboBox based on the given boolean property
   */
  public static SteppedComboBox createBooleanComboBox(final Property property, final Value value,
                                                      final StateObserver enabledState) {
    final SteppedComboBox box = createComboBox(property, value, new BooleanComboBoxModel(), enabledState);
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
                                                        final Value value, final SwingEntityComboBoxModel comboBoxModel) {
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
                                                        final Value value, final SwingEntityComboBoxModel comboBoxModel,
                                                        final StateObserver enabledState) {
    requireNonNull(foreignKeyProperty, FOREIGN_KEY_PROPERTY_PARAM_NAME);
    requireNonNull(comboBoxModel, "comboBoxModel");
    comboBoxModel.refresh();
    final EntityComboBox comboBox = new EntityComboBox(comboBoxModel);
    ValueLinks.selectedItemValueLink(comboBox, value);
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
                                                              final Value value, final EntityLookupModel lookupModel) {
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
  public static EntityLookupField createForeignKeyLookupField(final ForeignKeyProperty foreignKeyProperty, final Value value,
                                                              final EntityLookupModel lookupModel, final StateObserver enabledState) {
    requireNonNull(foreignKeyProperty, FOREIGN_KEY_PROPERTY_PARAM_NAME);
    requireNonNull(lookupModel, "lookupModel");
    final EntityLookupField lookupField = new EntityLookupField(lookupModel);
    Values.link(value, new LookupUIValue(lookupField.getModel()));
    linkToEnabledState(enabledState, lookupField);
    lookupField.setToolTipText(foreignKeyProperty.getDescription());
    UiUtil.selectAllOnFocusGained(lookupField);

    return lookupField;
  }

  /**
   * Creates a combo box based on the values in the given value list property
   * @param property the property
   * @param value the value to bind to the field
   * @return a combo box based on the given values
   */
  public static SteppedComboBox createValueListComboBox(final ValueListProperty property, final Value value) {
    return createValueListComboBox(property, value, true, null);
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
    return createValueListComboBox(property, value, true, enabledState);
  }

  /**
   * Creates a combo box based on the values in the given value list property
   * @param property the property
   * @param value the value to bind to the field
   * @param sortItems if true then the items are sorted
   * @return a combo box based on the given values
   */
  public static SteppedComboBox createValueListComboBox(final ValueListProperty property, final Value value,
                                                        final boolean sortItems) {
    return createValueListComboBox(property, value, sortItems, null);
  }

  /**
   * Creates a combo box based on the values in the given value list property
   * @param property the property
   * @param value the value to bind to the field
   * @param sortItems if true then the items are sorted
   * @param enabledState the state controlling the enabled state of the combo box
   * @return a combo box based on the given values
   */
  public static SteppedComboBox createValueListComboBox(final ValueListProperty property, final Value value,
                                                        final boolean sortItems, final StateObserver enabledState) {
    final ItemComboBoxModel model = sortItems ?
            new ItemComboBoxModel(property.getValues()) : new ItemComboBoxModel(null, property.getValues());
    final SteppedComboBox comboBox = createComboBox(property, value, model, enabledState);
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
    return createComboBox(property, value, model, enabledState, false);
  }

  /**
   * Creates a combo box based on the given combo box model
   * @param property the property
   * @param value the value to bind to the field
   * @param model the combo box model
   * @param enabledState the state controlling the enabled state of the combo box
   * @param editable if true then the combo box is made editable
   * @return a combo box based on the given model
   */
  public static SteppedComboBox createComboBox(final Property property, final Value value,
                                               final ComboBoxModel model, final StateObserver enabledState,
                                               final boolean editable) {
    requireNonNull(property, PROPERTY_PARAM_NAME);
    requireNonNull(value, VALUE_PARAM_NAME);
    final SteppedComboBox comboBox = new SteppedComboBox(model);
    comboBox.setEditable(editable);
    ValueLinks.selectedItemValueLink(comboBox, value);
    linkToEnabledState(enabledState, comboBox);
    comboBox.setToolTipText(property.getDescription());

    return comboBox;
  }

  /**
   * Creates a panel with a date input field and a button for opening a date input dialog
   * @param property the property
   * @param value the value to bind to the field
   * @param updateOnKeystroke if true then the value is updated on each keystroke, otherwise on focus lost
   * @param includeButton if true then a button for opening a date input dialog is included (only available for LocalDate)
   * @return a date input panel
   */
  public static TemporalInputPanel createTemporalInputPanel(final Property property, final Value value,
                                                            final boolean updateOnKeystroke, final boolean includeButton) {
    return createTemporalInputPanel(property, value, updateOnKeystroke, includeButton, null);
  }

  /**
   * Creates a panel with a date input field and a button for opening a date input dialog (if applicable)
   * @param property the property
   * @param value the value to bind to the field
   * @param updateOnKeystroke if true then the value is updated on each keystroke, otherwise on focus lost
   * @param includeCalendarButton if true then a button for opening a calendar dialog is included
   * @param enabledState the state controlling the enabled state of the panel
   * @return a date input panel
   */
  public static TemporalInputPanel createTemporalInputPanel(final Property property, final Value value,
                                                            final boolean updateOnKeystroke, final boolean includeCalendarButton,
                                                            final StateObserver enabledState) {
    requireNonNull(property, PROPERTY_PARAM_NAME);
    if (!property.isTemporal()) {
      throw new IllegalArgumentException("Property " + property + " is not a date or time property");
    }

    final String formatString = property.getDateTimeFormatPattern();
    final JFormattedTextField field = (JFormattedTextField) createTextField(property, value,
            DateFormats.getDateMask(formatString), updateOnKeystroke, enabledState);
    if (property.isDate()) {
      return new LocalDateInputPanel(field, formatString, includeCalendarButton, enabledState);
    }
    else if (property.isTimestamp()) {
      return new LocalDateTimeInputPanel(field, formatString, includeCalendarButton, enabledState);
    }
    else if (property.isTime()) {
      return new LocalTimeInputPanel(field, formatString, enabledState);
    }

    throw new IllegalArgumentException("Can not create a date input panel for a non-date property");
  }

  /**
   * Creates a panel with a text field and a button for opening a dialog with a text area
   * @param property the property
   * @param value the value to bind to the field
   * @param updateOnKeystroke if true then the value is updated on each keystroke, otherwise on focus lost
   * @param buttonFocusable if true then the dialog button is focusable
   * @return a text input panel
   */
  public static TextInputPanel createTextInputPanel(final Property property, final Value value,
                                                    final boolean updateOnKeystroke, final boolean buttonFocusable) {
    requireNonNull(property, PROPERTY_PARAM_NAME);
    requireNonNull(value, VALUE_PARAM_NAME);
    final JTextField field = createTextField(property, value, null, updateOnKeystroke);
    final TextInputPanel panel = new TextInputPanel(field, property.getCaption(), null, buttonFocusable);
    panel.setMaxLength(property.getMaxLength());

    return panel;
  }

  /**
   * Creates a text area based on the given property
   * @param property the property
   * @param value the value to bind to the field
   * @param updateOnKeystroke if true then the value is updated on each keystroke, otherwise on focus lost
   * @return a text area
   */
  public static JTextArea createTextArea(final Property property, final Value value,
                                         final boolean updateOnKeystroke) {
    return createTextArea(property, value, -1, -1, updateOnKeystroke);
  }

  /**
   * Creates a text area based on the given property
   * @param property the property
   * @param value the value to bind to the field
   * @param rows the number of rows
   * @param columns the number of columns
   * @param updateOnKeystroke if true then the value is updated on each keystroke, otherwise on focus lost
   * @return a text area
   */
  public static JTextArea createTextArea(final Property property, final Value value,
                                         final int rows, final int columns, final boolean updateOnKeystroke) {
    return createTextArea(property, value, rows, columns, updateOnKeystroke, null);
  }

  /**
   * Creates a text area based on the given property
   * @param property the property
   * @param value the value to bind to the field
   * @param rows the number of rows
   * @param columns the number of columns
   * @param updateOnKeystroke if true then the value is updated on each keystroke, otherwise on focus lost
   * @param enabledState a state indicating when the text area should be enabled
   * @return a text area
   */
  public static JTextArea createTextArea(final Property property, final Value value,
                                         final int rows, final int columns, final boolean updateOnKeystroke,
                                         final StateObserver enabledState) {
    requireNonNull(property, PROPERTY_PARAM_NAME);
    requireNonNull(value, VALUE_PARAM_NAME);
    if (!property.isString()) {
      throw new IllegalArgumentException("Cannot create a text area for a non-string property");
    }

    final JTextArea textArea = rows > 0 && columns > 0 ? new JTextArea(rows, columns) : new JTextArea();
    textArea.setLineWrap(true);
    textArea.setWrapStyleWord(true);
    if (property.getMaxLength() > 0) {
      ((AbstractDocument) textArea.getDocument()).setDocumentFilter(new DocumentSizeFilter(property.getMaxLength()));
    }
    linkToEnabledState(enabledState, textArea);

    ValueLinks.textValueLink(textArea, value, null, updateOnKeystroke);
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
    return createTextField(property, value, null, true);
  }

  /**
   * Creates a text field based on the given property
   * @param property the property
   * @param value the value to bind to the field
   * @param formatMaskString if specified the resulting text field is a JFormattedField with this mask
   * @param updateOnKeystroke if true then the value is updated on each keystroke, otherwise on focus lost
   * @return a text field for the given property
   */
  public static JTextField createTextField(final Property property, final Value value,
                                           final String formatMaskString, final boolean updateOnKeystroke) {
    return createTextField(property, value, formatMaskString, updateOnKeystroke, null);
  }

  /**
   * Creates a text field based on the given property
   * @param property the property
   * @param value the value to bind to the field
   * @param formatMaskString if specified the resulting text field is a JFormattedField with this mask
   * @param updateOnKeystroke if true then the value is updated on each keystroke, otherwise on focus lost
   * @param enabledState the state controlling the enabled state of the panel
   * @return a text field for the given property
   */
  public static JTextField createTextField(final Property property, final Value value,
                                           final String formatMaskString, final boolean updateOnKeystroke,
                                           final StateObserver enabledState) {
    return createTextField(property, value, formatMaskString, updateOnKeystroke, enabledState, false);
  }

  /**
   * Creates a text field based on the given property
   * @param property the property
   * @param value the value to bind to the field
   * @param formatMaskString if specified the resulting text field is a JFormattedField with this mask
   * @param updateOnKeystroke if true then the value is updated on each keystroke, otherwise on focus lost
   * @param enabledState the state controlling the enabled state of the panel
   * @param valueContainsLiteralCharacters whether or not the value should contain any literal characters
   * associated with a the format mask
   * @return a text field for the given property
   */
  public static JTextField createTextField(final Property property, final Value value, final String formatMaskString,
                                           final boolean updateOnKeystroke, final StateObserver enabledState,
                                           final boolean valueContainsLiteralCharacters) {
    requireNonNull(property, PROPERTY_PARAM_NAME);
    requireNonNull(value, VALUE_PARAM_NAME);
    final JTextField textField = createTextField(property, enabledState, formatMaskString, valueContainsLiteralCharacters);
    if (property.isString()) {
      ValueLinks.textValueLink(textField, value, property.getFormat(), updateOnKeystroke);
    }
    else if (property.isInteger()) {
      ValueLinks.integerValueLink((IntegerField) textField, value, true, updateOnKeystroke);
    }
    else if (property.isDouble()) {
      ValueLinks.doubleValueLink((DecimalField) textField, value, true, updateOnKeystroke);
    }
    else if (property.isBigDecimal()) {
      ValueLinks.bigDecimalValueLink((DecimalField) textField, value, updateOnKeystroke);
    }
    else if (property.isLong()) {
      ValueLinks.longValueLink((LongField) textField, value, true, updateOnKeystroke);
    }
    else if (property.isDate()) {
      ValueLinks.localDateValueLink((JFormattedTextField) textField, value,
              property.getDateTimeFormatPattern(), updateOnKeystroke);
    }
    else if (property.isTime()) {
      ValueLinks.localTimeValueLink((JFormattedTextField) textField, value,
              property.getDateTimeFormatPattern(), updateOnKeystroke);
    }
    else if (property.isTimestamp()) {
      ValueLinks.localDateTimeValueLink((JFormattedTextField) textField, value,
              property.getDateTimeFormatPattern(), updateOnKeystroke);
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
   * @return a combo box based on the property values
   */
  public static SteppedComboBox createPropertyComboBox(final ColumnProperty property, final Value value,
                                                       final ComboBoxModel comboBoxModel, final StateObserver enabledState) {
    return createPropertyComboBox(property, value, comboBoxModel, enabledState, false);
  }

  /**
   * Creates a combo box based on the values of the given property
   * @param property the property
   * @param value the value to bind to the field
   * @param comboBoxModel the combo box model
   * @param enabledState the state controlling the enabled state of the panel
   * @param editable if true then the combo box will be editable
   * @return a combo box based on the property values
   */
  public static SteppedComboBox createPropertyComboBox(final ColumnProperty property, final Value value,
                                                       final ComboBoxModel comboBoxModel, final StateObserver enabledState,
                                                       final boolean editable) {
    final SteppedComboBox comboBox = createComboBox(property, value, comboBoxModel, enabledState, editable);
    if (!editable) {
      addComboBoxCompletion(comboBox);
    }

    return comboBox;
  }

  /**
   * Creates a panel containing an EntityComboBox and a button for filtering that combo box based on a foreign key
   * @param entityComboBox the combo box
   * @param foreignKeyPropertyId the foreign key to base the filtering on
   * @param filterButtonTakesFocus if true then the filter button is focusable
   * @return a panel with a combo box and a button
   */
  public static JPanel createEntityComboBoxFilterPanel(final EntityComboBox entityComboBox, final String foreignKeyPropertyId,
                                                       final boolean filterButtonTakesFocus) {
    return UiUtil.createEastButtonPanel(entityComboBox, entityComboBox.createForeignKeyFilterControl(foreignKeyPropertyId),
            filterButtonTakesFocus);
  }

  private static JTextField createTextField(final Property property, final StateObserver enabledState,
                                            final String formatMaskString, final boolean valueContainsLiteralCharacters) {
    final JTextField field = createTextField(property, formatMaskString, valueContainsLiteralCharacters);
    linkToEnabledState(enabledState, field);
    field.setToolTipText(property.getDescription());
    if (property.getMaxLength() > 0 && field.getDocument() instanceof SizedDocument) {
      ((SizedDocument) field.getDocument()).setMaxLength(property.getMaxLength());
    }

    return field;
  }

  private static JTextField createTextField(final Property property, final String formatMaskString,
                                            final boolean valueContainsLiteralCharacters) {
    if (property.isInteger()) {
      return initializeIntField(property);
    }
    else if (property.isDecimal()) {
      return initializeDecimalField(property);
    }
    else if (property.isLong()) {
      return initializeLongField(property);
    }
    else if (property.isTemporal()) {
      return UiUtil.createFormattedField(DateFormats.getDateMask(property.getDateTimeFormatPattern()));
    }
    else if (property.isString()) {
      return initializeStringField(formatMaskString, valueContainsLiteralCharacters);
    }

    throw new IllegalArgumentException("Creating text fields for property type: " + property.getType() + " is not implemented");
  }

  private static JTextField initializeStringField(final String formatMaskString, final boolean valueContainsLiteralCharacters) {
    if (formatMaskString == null) {
      return new JTextField(new SizedDocument(), "", 0);
    }

    return UiUtil.createFormattedField(formatMaskString, valueContainsLiteralCharacters);
  }

  private static JTextField initializeDecimalField(final Property property) {
    final DecimalField field = new DecimalField((DecimalFormat) cloneFormat((NumberFormat) property.getFormat()));
    if (property.getMin() != null && property.getMax() != null) {
      field.setRange(Math.min(property.getMin(), 0), property.getMax());
    }

    return field;
  }

  private static JTextField initializeIntField(final Property property) {
    final IntegerField field = new IntegerField(cloneFormat((NumberFormat) property.getFormat()));
    if (property.getMin() != null && property.getMax() != null) {
      field.setRange(property.getMin(), property.getMax());
    }

    return field;
  }

  private static JTextField initializeLongField(final Property property) {
    final LongField field = new LongField(cloneFormat((NumberFormat) property.getFormat()));
    if (property.getMin() != null && property.getMax() != null) {
      field.setRange(property.getMin(), property.getMax());
    }

    return field;
  }

  private static JCheckBox initializeCheckBox(final Property property, final Value value,
                                              final StateObserver enabledState, final JCheckBox checkBox) {
    ValueLinks.toggleValueLink(checkBox.getModel(), value, false);
    linkToEnabledState(enabledState, checkBox);
    checkBox.setToolTipText(property.getDescription());

    return checkBox;
  }

  private static void linkToEnabledState(final StateObserver enabledState, final JComponent component) {
    if (enabledState != null) {
      UiUtil.linkToEnabledState(enabledState, component);
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

  private static void addComboBoxCompletion(final JComboBox comboBox) {
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
      this.lookupModel.addSelectedEntitiesListener(selected -> fireChangeEvent(get()));
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
