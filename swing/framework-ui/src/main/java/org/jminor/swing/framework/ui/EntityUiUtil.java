/*
 * Chinook.Copyright (c) 2004 - 2018, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.framework.ui;

import org.jminor.common.Configuration;
import org.jminor.common.DateUtil;
import org.jminor.common.Event;
import org.jminor.common.EventObserver;
import org.jminor.common.Events;
import org.jminor.common.LoggerProxy;
import org.jminor.common.StateObserver;
import org.jminor.common.TextUtil;
import org.jminor.common.Value;
import org.jminor.common.Values;
import org.jminor.common.db.exception.DatabaseException;
import org.jminor.common.db.valuemap.exception.ValidationException;
import org.jminor.common.model.valuemap.EditModelValues;
import org.jminor.framework.db.EntityConnectionProvider;
import org.jminor.framework.domain.Entities;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.Property;
import org.jminor.framework.i18n.FrameworkMessages;
import org.jminor.framework.model.DefaultEntityLookupModel;
import org.jminor.framework.model.EntityComboBoxModel;
import org.jminor.framework.model.EntityEditModel;
import org.jminor.framework.model.EntityLookupModel;
import org.jminor.swing.common.model.combobox.BooleanComboBoxModel;
import org.jminor.swing.common.model.combobox.ItemComboBoxModel;
import org.jminor.swing.common.ui.DateInputPanel;
import org.jminor.swing.common.ui.TextInputPanel;
import org.jminor.swing.common.ui.UiUtil;
import org.jminor.swing.common.ui.ValueLinks;
import org.jminor.swing.common.ui.checkbox.TristateCheckBox;
import org.jminor.swing.common.ui.combobox.AutoCompletion;
import org.jminor.swing.common.ui.combobox.MaximumMatch;
import org.jminor.swing.common.ui.combobox.SteppedComboBox;
import org.jminor.swing.common.ui.input.InputProviderPanel;
import org.jminor.swing.common.ui.textfield.DocumentSizeFilter;
import org.jminor.swing.common.ui.textfield.DoubleField;
import org.jminor.swing.common.ui.textfield.IntegerField;
import org.jminor.swing.common.ui.textfield.LongField;
import org.jminor.swing.common.ui.textfield.SizedDocument;
import org.jminor.swing.common.ui.valuemap.ValueLinkValidators;
import org.jminor.swing.framework.model.SwingEntityEditModel;

import javax.swing.Action;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.text.AbstractDocument;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.Point;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

/**
 * A static utility class concerned with UI related tasks.
 */
public final class EntityUiUtil {

  /**
   * Identifies the completion mode MaximumMatch
   * @see EntityUiUtil#COMBO_BOX_COMPLETION_MODE
   */
  public static final String COMPLETION_MODE_MAXIMUM_MATCH = "max";

  /**
   * Identifies the completion mode AutoCompletion
   * @see EntityUiUtil#COMBO_BOX_COMPLETION_MODE
   */
  public static final String COMPLETION_MODE_AUTOCOMPLETE = "auto";

  /**
   * Specifies the default horizontal alignment used in labels<br>
   * Value type: Integer (JLabel.LEFT, JLabel.RIGHT, JLabel.CENTER)<br>
   * Default value: JLabel.LEFT
   */
  public static final Value<Integer> LABEL_TEXT_ALIGNMENT = Configuration.integerValue("jminor.swing.labelTextAlignment", JLabel.LEFT);

  /**
   * Specifies whether maximum match or autocomplete is used for comboboxes,
   * {@link #COMPLETION_MODE_MAXIMUM_MATCH} for {@link MaximumMatch}
   * and {@link #COMPLETION_MODE_AUTOCOMPLETE} for {@link AutoCompletion}.<br>
   * Value type:String<br>
   * Default value: {@link #COMPLETION_MODE_MAXIMUM_MATCH}
   */
  public static final Value<String> COMBO_BOX_COMPLETION_MODE = Configuration.stringValue("jminor.swing.comboBoxCompletionMode", COMPLETION_MODE_MAXIMUM_MATCH);

  private static final String PROPERTY_PARAM_NAME = "property";
  private static final String EDIT_MODEL_PARAM_NAME = "editModel";
  private static final String FOREIGN_KEY_PROPERTY_PARAM_NAME = "foreignKeyProperty";
  private static final int BOOLEAN_COMBO_BOX_POPUP_WIDTH = 40;
  private static final Color INVALID_COLOR = Color.RED;
  private static final int MAXIMUM_VALUE_LENGTH = 1000;

  private static final LoggerProxy loggerProxy = LoggerProxy.createLoggerProxy();

  private EntityUiUtil() {}

  /**
   * Shows a dialog for selecting the root logging level.
   * @param dialogParent the component serving as a dialog parent
   */
  public static void setLoggingLevel(final JComponent dialogParent) {
    if (loggerProxy == null) {
      throw new RuntimeException("No LoggerProxy implementation available");
    }
    final DefaultComboBoxModel model = new DefaultComboBoxModel(loggerProxy.getLogLevels().toArray());
    model.setSelectedItem(loggerProxy.getLogLevel());
    JOptionPane.showMessageDialog(dialogParent, new JComboBox(model),
            FrameworkMessages.get(FrameworkMessages.SET_LOG_LEVEL), JOptionPane.QUESTION_MESSAGE);
    loggerProxy.setLogLevel(model.getSelectedItem());
  }

  /**
   * Performs a lookup for the given entity type, using a EntityLookupField displayed
   * in a dialog, using the default search properties for the given entityId.
   * @param entityId the entityId of the entity to perform a lookup for
   * @param connectionProvider the connection provider
   * @param singleSelection if true only a single entity can be selected
   * @param dialogParent the component serving as the dialog parent
   * @param lookupCaption the caption for the lookup field, used as a caption for the dialog as well
   * @return the selected entities or an empty collection in case a selection was not performed
   * @see EntityLookupField
   * @see Entities#getSearchProperties(String)
   */
  public static Collection<Entity> lookupEntities(final String entityId, final EntityConnectionProvider connectionProvider,
                                                  final boolean singleSelection, final JComponent dialogParent,
                                                  final String lookupCaption) {
    return lookupEntities(entityId, connectionProvider, singleSelection, dialogParent, lookupCaption, lookupCaption);
  }

  /**
   * Performs a lookup for the given entity type, using a EntityLookupField displayed
   * in a dialog, using the default search properties for the given entityId.
   * @param entityId the entityId of the entity to perform a lookup for
   * @param connectionProvider the connection provider
   * @param singleSelection if true only a single entity can be selected
   * @param dialogParent the component serving as the dialog parent
   * @param lookupCaption the caption for the lookup field
   * @param dialogTitle the title to display on the dialog
   * @return the selected entities or an empty collection in case a selection was not performed
   * @see EntityLookupField
   * @see Entities#getSearchProperties(String)
   */
  public static Collection<Entity> lookupEntities(final String entityId, final EntityConnectionProvider connectionProvider,
                                                  final boolean singleSelection, final JComponent dialogParent,
                                                  final String lookupCaption, final String dialogTitle) {
    final EntityLookupModel lookupModel = new DefaultEntityLookupModel(entityId, connectionProvider);
    if (singleSelection) {
      lookupModel.getMultipleSelectionAllowedValue().set(false);
    }
    final InputProviderPanel inputPanel = new InputProviderPanel(lookupCaption, new EntityLookupProvider(lookupModel, null));
    UiUtil.displayInDialog(dialogParent, inputPanel, dialogTitle, true, inputPanel.getOkButton(), inputPanel.getButtonClickObserver());
    if (inputPanel.isInputAccepted()) {
      return lookupModel.getSelectedEntities();
    }

    return Collections.emptyList();
  }

  /**
   * Creates a JLabel with a caption from the given property, using the default label text alignment
   * @param property the property for which to create the label
   * @return a JLabel for the given property
   * @see EntityUiUtil#LABEL_TEXT_ALIGNMENT
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
    Objects.requireNonNull(property, PROPERTY_PARAM_NAME);
    final JLabel label = new JLabel(property.getCaption(), horizontalAlignment);
    if (property.getMnemonic() != null) {
      label.setDisplayedMnemonic(property.getMnemonic());
    }

    return label;
  }

  /**
   * Creates a JCheckBox based on the given boolean property
   * @param property the property on which value to base the checkbox
   * @param editModel the edit model to bind with the value
   * @return a check box based on the given property
   * @throws IllegalArgumentException in case the property is not a boolean property
   */
  public static JCheckBox createCheckBox(final Property property, final EntityEditModel editModel) {
    return createCheckBox(property, editModel, null);
  }

  /**
   * Creates a JCheckBox based on the given boolean property
   * @param property the property on which value to base the checkbox
   * @param editModel the edit model to bind with the value
   * @param enabledState the state controlling the enabled state of the checkbox
   * @return a check box based on the given property
   * @throws IllegalArgumentException in case the property is not a boolean property
   */
  public static JCheckBox createCheckBox(final Property property, final EntityEditModel editModel,
                                         final StateObserver enabledState) {
    return createCheckBox(property, editModel, enabledState, true);
  }

  /**
   * Creates a JCheckBox based on the given boolean property
   * @param property the property on which value to base the checkbox
   * @param editModel the edit model to bind with the value
   * @param enabledState the state controlling the enabled state of the checkbox
   * @param includeCaption if true then the property caption is included as the checkbox text
   * @return a check box based on the given property
   * @throws IllegalArgumentException in case the property is not a boolean property
   */
  public static JCheckBox createCheckBox(final Property property, final EntityEditModel editModel,
                                         final StateObserver enabledState, final boolean includeCaption) {
    Objects.requireNonNull(property, PROPERTY_PARAM_NAME);
    checkProperty(property, editModel);
    if (!property.isBoolean()) {
      throw new IllegalArgumentException("Boolean property required for createCheckBox");
    }

    final JCheckBox checkBox = includeCaption ? new JCheckBox(property.getCaption()) : new JCheckBox();
    ValueLinks.toggleValueLink(checkBox.getModel(), EditModelValues.<Boolean>value(editModel, property), false);
    UiUtil.linkToEnabledState(enabledState, checkBox);
    if (property.getDescription() != null) {
      checkBox.setToolTipText(property.getDescription());
    }
    else {
      checkBox.setToolTipText(property.getCaption());
    }
    if (EntityEditPanel.TRANSFER_FOCUS_ON_ENTER.get()) {
      UiUtil.transferFocusOnEnter(checkBox);
    }

    return checkBox;
  }

  /**
   * Creates a TristateCheckBox based on the given boolean property
   * @param property the property on which value to base the checkbox
   * @param editModel the edit model to bind with the value
   * @param enabledState the state controlling the enabled state of the checkbox
   * @param includeCaption if true then the property caption is included as the checkbox text
   * @return a check box based on the given property
   * @throws IllegalArgumentException in case the property is not a nullable boolean property
   */
  public static TristateCheckBox createTristateCheckBox(final Property property, final EntityEditModel editModel,
                                                        final StateObserver enabledState, final boolean includeCaption) {
    Objects.requireNonNull(property, PROPERTY_PARAM_NAME);
    Objects.requireNonNull(editModel, EDIT_MODEL_PARAM_NAME);
    checkProperty(property, editModel);
    if (!property.isBoolean() || !property.isNullable()) {
      throw new IllegalArgumentException("Nullable boolean property required for createTristateCheckBox");
    }

    final TristateCheckBox checkBox = new TristateCheckBox(includeCaption ? property.getCaption() : null);
    ValueLinks.toggleValueLink(checkBox.getModel(), EditModelValues.<Boolean>value(editModel, property), false);
    UiUtil.linkToEnabledState(enabledState, checkBox);
    if (property.getDescription() != null) {
      checkBox.setToolTipText(property.getDescription());
    }
    else {
      checkBox.setToolTipText(property.getCaption());
    }
    if (EntityEditPanel.TRANSFER_FOCUS_ON_ENTER.get()) {
      UiUtil.transferFocusOnEnter(checkBox);
    }

    return checkBox;
  }

  /**
   * Creates a combobox containing the values (null, yes, no) based on the given boolean property
   * @param property the property on which to base the combobox
   * @param editModel the edit model to bind with the value
   * @return a SteppedComboBox based on the given boolean property
   */
  public static SteppedComboBox createBooleanComboBox(final Property property, final EntityEditModel editModel) {
    return createBooleanComboBox(property, editModel, null);
  }

  /**
   * Creates a combobox containing the values (null, yes, no) based on the given boolean property
   * @param property the property on which to base the combobox
   * @param editModel the edit model to bind with the value
   * @param enabledState the state controlling the enabled state of the combobox
   * @return a SteppedComboBox based on the given boolean property
   */
  public static SteppedComboBox createBooleanComboBox(final Property property, final EntityEditModel editModel,
                                                      final StateObserver enabledState) {
    final SteppedComboBox box = createComboBox(property, editModel, new BooleanComboBoxModel(), enabledState);
    box.setPopupWidth(BOOLEAN_COMBO_BOX_POPUP_WIDTH);

    return box;
  }

  /**
   * Creates EntityComboBox based on the given foreign key property
   * @param foreignKeyProperty the foreign key property on which entity to base the combobox
   * @param editModel the edit model to bind with the value
   * @return a EntityComboBox based on the given foreign key property
   */
  public static EntityComboBox createForeignKeyComboBox(final Property.ForeignKeyProperty foreignKeyProperty,
                                                        final EntityEditModel editModel) {
    return createForeignKeyComboBox(foreignKeyProperty, editModel, null);
  }

  /**
   * Creates EntityComboBox based on the given foreign key property
   * @param foreignKeyProperty the foreign key property on which entity to base the combobox
   * @param editModel the edit model to bind with the value
   * @param enabledState the state controlling the enabled state of the combobox
   * @return a EntityComboBox based on the given foreign key property
   */
  public static EntityComboBox createForeignKeyComboBox(final Property.ForeignKeyProperty foreignKeyProperty,
                                                        final EntityEditModel editModel, final StateObserver enabledState) {
    Objects.requireNonNull(foreignKeyProperty, FOREIGN_KEY_PROPERTY_PARAM_NAME);
    Objects.requireNonNull(editModel, EDIT_MODEL_PARAM_NAME);
    checkProperty(foreignKeyProperty, editModel);
    final EntityComboBoxModel boxModel = ((SwingEntityEditModel) editModel).getForeignKeyComboBoxModel(foreignKeyProperty);
    boxModel.refresh();
    final EntityComboBox comboBox = new EntityComboBox(boxModel);
    ValueLinks.selectedItemValueLink(comboBox, EditModelValues.<Entity>value(editModel, foreignKeyProperty));
    UiUtil.linkToEnabledState(enabledState, comboBox);
    addComboBoxCompletion(comboBox);
    comboBox.setToolTipText(foreignKeyProperty.getDescription());
    if (EntityEditPanel.TRANSFER_FOCUS_ON_ENTER.get()) {
      //getEditor().getEditorComponent() only required because the combo box is editable, due to addComboBoxCompletion() above
      UiUtil.transferFocusOnEnter((JComponent) comboBox.getEditor().getEditorComponent());
    }

    return comboBox;
  }

  /**
   * Creates a read-only, non-focusable JTextField displaying the value of the given property in the given edit model
   * @param foreignKeyProperty the property which value should be displayed
   * @param editModel the edit model
   * @return a read-only, non-focusable JTextField displaying the value of the given property
   */
  public static JTextField createForeignKeyField(final Property.ForeignKeyProperty foreignKeyProperty,
                                                 final EntityEditModel editModel) {
    Objects.requireNonNull(foreignKeyProperty, FOREIGN_KEY_PROPERTY_PARAM_NAME);
    Objects.requireNonNull(editModel, EDIT_MODEL_PARAM_NAME);
    checkProperty(foreignKeyProperty, editModel);
    final JTextField textField = new JTextField();
    textField.setEditable(false);
    textField.setFocusable(false);
    textField.setToolTipText(foreignKeyProperty.getDescription());
    final Event<String> valueChangeEvent = Events.event();
    editModel.addValueListener(foreignKeyProperty, info -> {
      final Entity value = (Entity) info.getNewValue();
      valueChangeEvent.fire(value == null ? "" : value.toString());
    });
    ValueLinks.textValueLink(textField, new Value<String>() {
      @Override
      public void set(final String value) {/*read only*/}
      @Override
      public String get() {
        final Entity value = editModel.getForeignKeyValue(foreignKeyProperty.getPropertyId());

        return value == null ? "" : value.toString();
      }
      @Override
      public EventObserver<String> getObserver() {
        return valueChangeEvent.getObserver();
      }
    }, null, false, true);

    return textField;
  }

  /**
   * Creates a EntityLookupField based on the given foreign key property
   * @param foreignKeyProperty the foreign key property on which entity to base the lookup model
   * @param editModel the edit model providing the {@link EntityLookupModel} to use and to bind with the value
   * @return a lookup model based on the given foreign key property
   */
  public static EntityLookupField createForeignKeyLookupField(final Property.ForeignKeyProperty foreignKeyProperty,
                                                              final EntityEditModel editModel) {
    return createForeignKeyLookupField(foreignKeyProperty, editModel, null);
  }

  /**
   * Creates a EntityLookupField based on the given foreign key property
   * @param foreignKeyProperty the foreign key property on which entity to base the lookup model
   * @param editModel the edit model providing the {@link EntityLookupModel} to use and to bind with the value
   * @param enabledState the state controlling the enabled state of the lookup field
   * @return a lookup model based on the given foreign key property
   */
  public static EntityLookupField createForeignKeyLookupField(final Property.ForeignKeyProperty foreignKeyProperty,
                                                              final EntityEditModel editModel, final StateObserver enabledState) {
    Objects.requireNonNull(editModel, EDIT_MODEL_PARAM_NAME);
    Objects.requireNonNull(foreignKeyProperty, FOREIGN_KEY_PROPERTY_PARAM_NAME);
    final EntityLookupModel lookupModel = editModel.getForeignKeyLookupModel(foreignKeyProperty);
    final EntityLookupField lookupField = new EntityLookupField(lookupModel);
    if (EntityEditPanel.TRANSFER_FOCUS_ON_ENTER.get()) {
      lookupField.setTransferFocusOnEnter();
    }
    Values.link(EditModelValues.<Entity>value(editModel, foreignKeyProperty), new LookupUIValue(lookupField.getModel()));
    UiUtil.linkToEnabledState(enabledState, lookupField);
    lookupField.setToolTipText(foreignKeyProperty.getDescription());
    UiUtil.selectAllOnFocusGained(lookupField);

    return lookupField;
  }

  /**
   * Creates a combo box based on the values in the given value list property
   * @param property the property
   * @param editModel the edit model to bind with the value
   * @return a combo box based on the given values
   */
  public static SteppedComboBox createValueListComboBox(final Property.ValueListProperty property, final EntityEditModel editModel) {
    return createValueListComboBox(property, editModel, true, null);
  }

  /**
   * Creates a combo box based on the values in the given value list property
   * @param property the property
   * @param editModel the edit model to bind with the value
   * @param enabledState the state controlling the enabled state of the combo box
   * @return a combo box based on the given values
   */
  public static SteppedComboBox createValueListComboBox(final Property.ValueListProperty property, final EntityEditModel editModel,
                                                        final StateObserver enabledState) {
    return createValueListComboBox(property, editModel, true, enabledState);
  }

  /**
   * Creates a combo box based on the values in the given value list property
   * @param property the property
   * @param editModel the edit model to bind with the value
   * @param sortItems if true then the items are sorted
   * @return a combo box based on the given values
   */
  public static SteppedComboBox createValueListComboBox(final Property.ValueListProperty property, final EntityEditModel editModel,
                                                        final boolean sortItems) {
    return createValueListComboBox(property, editModel, sortItems, null);
  }

  /**
   * Creates a combo box based on the values in the given value list property
   * @param property the property
   * @param editModel the edit model to bind with the value
   * @param sortItems if true then the items are sorted
   * @param enabledState the state controlling the enabled state of the combo box
   * @return a combo box based on the given values
   */
  public static SteppedComboBox createValueListComboBox(final Property.ValueListProperty property, final EntityEditModel editModel,
                                                        final boolean sortItems, final StateObserver enabledState) {
    final ItemComboBoxModel model;
    if (sortItems) {
      model = new ItemComboBoxModel(property.getValues());
    }
    else {
      model = new ItemComboBoxModel(null, property.getValues());
    }
    final SteppedComboBox comboBox = createComboBox(property, editModel, model, enabledState);
    addComboBoxCompletion(comboBox);

    return comboBox;
  }

  /**
   * Creates a combo box based on the given combo box model
   * @param property the property
   * @param editModel the edit model to bind with the value
   * @param model the combo box model
   * @param enabledState the state controlling the enabled state of the combo box
   * @return a combo box based on the given model
   */
  public static SteppedComboBox createComboBox(final Property property, final EntityEditModel editModel,
                                               final ComboBoxModel model, final StateObserver enabledState) {
    return createComboBox(property, editModel, model, enabledState, false);
  }

  /**
   * Creates a combo box based on the given combo box model
   * @param property the property
   * @param editModel the edit model to bind with the value
   * @param model the combo box model
   * @param enabledState the state controlling the enabled state of the combo box
   * @param editable if true then the combo box is made editable
   * @return a combo box based on the given model
   */
  public static SteppedComboBox createComboBox(final Property property, final EntityEditModel editModel,
                                               final ComboBoxModel model, final StateObserver enabledState,
                                               final boolean editable) {
    Objects.requireNonNull(property, PROPERTY_PARAM_NAME);
    checkProperty(property, editModel);
    final SteppedComboBox comboBox = new SteppedComboBox(model);
    comboBox.setEditable(editable);
    ValueLinks.selectedItemValueLink(comboBox, EditModelValues.value(editModel, property));
    UiUtil.linkToEnabledState(enabledState, comboBox);
    comboBox.setToolTipText(property.getDescription());
    if (EntityEditPanel.TRANSFER_FOCUS_ON_ENTER.get()) {
      UiUtil.transferFocusOnEnter((JComponent) comboBox.getEditor().getEditorComponent());
      UiUtil.transferFocusOnEnter(comboBox);
    }

    return comboBox;
  }

  /**
   * Creates a panel with a date input field and a button for opening a date input dialog
   * @param property the property
   * @param editModel the edit model to bind with the value
   * @param readOnly if true then the value is read only
   * @param includeButton if true then a button for opening a date input dialog is included
   * @return a date input panel
   */
  public static DateInputPanel createDateInputPanel(final Property property, final EntityEditModel editModel,
                                                    final boolean readOnly, final boolean includeButton) {
    return createDateInputPanel(property, editModel, readOnly, includeButton, null);
  }

  /**
   * Creates a panel with a date input field and a button for opening a date input dialog
   * @param property the property
   * @param editModel the edit model to bind with the value
   * @param readOnly if true then the value is read only
   * @param includeButton if true then a button for opening a date input dialog is included
   * @param enabledState the state controlling the enabled state of the panel
   * @return a date input panel
   */
  public static DateInputPanel createDateInputPanel(final Property property, final EntityEditModel editModel,
                                                    final boolean readOnly, final boolean includeButton,
                                                    final StateObserver enabledState) {
    Objects.requireNonNull(property,PROPERTY_PARAM_NAME);
    if (!property.isDateOrTime()) {
      throw new IllegalArgumentException("Property " + property + " is not a date or time property");
    }

    final JFormattedTextField field = (JFormattedTextField) createTextField(property, editModel, readOnly,
            DateUtil.getDateMask((SimpleDateFormat) property.getFormat()), true, enabledState);
    final DateInputPanel panel = new DateInputPanel(field, (SimpleDateFormat) property.getFormat(), includeButton, enabledState);
    if (panel.getButton() != null && EntityEditPanel.TRANSFER_FOCUS_ON_ENTER.get()) {
      UiUtil.transferFocusOnEnter(panel.getButton());
    }

    return panel;
  }

  /**
   * Creates a panel with a text field and a button for opening a dialog with a text area
   * @param property the property
   * @param editModel the edit model to bind with the value
   * @param readOnly if true then the value is read only
   * @param immediateUpdate if true then the value is committed on each keystroke, otherwise on focus lost
   * @param buttonFocusable if true then the dialog button is focusable
   * @return a text input panel
   */
  public static TextInputPanel createTextInputPanel(final Property property, final EntityEditModel editModel,
                                                    final boolean readOnly, final boolean immediateUpdate,
                                                    final boolean buttonFocusable) {
    Objects.requireNonNull(property, PROPERTY_PARAM_NAME);
    Objects.requireNonNull(editModel, EDIT_MODEL_PARAM_NAME);
    final JTextField field = createTextField(property, editModel, readOnly, null, immediateUpdate);
    final TextInputPanel panel = new TextInputPanel(field, property.getCaption(), null, buttonFocusable);
    panel.setMaxLength(property.getMaxLength());
    if (panel.getButton() != null && EntityEditPanel.TRANSFER_FOCUS_ON_ENTER.get()) {
      UiUtil.transferFocusOnEnter(panel.getButton());//todo
    }

    return panel;
  }

  /**
   * Creates a text area based on the given property
   * @param property the property
   * @param editModel the edit model to bind with the value
   * @param readOnly if true then the value is read only
   * @return a text area
   */
  public static JTextArea createTextArea(final Property property, final EntityEditModel editModel,
                                         final boolean readOnly) {
    return createTextArea(property, editModel, -1, -1, readOnly);
  }

  /**
   * Creates a text area based on the given property
   * @param property the property
   * @param editModel the edit model to bind with the value
   * @param rows the number of rows
   * @param columns the number of columns
   * @param readOnly if true then the value is read only
   * @return a text area
   */
  public static JTextArea createTextArea(final Property property, final EntityEditModel editModel,
                                         final int rows, final int columns, final boolean readOnly) {
    return createTextArea(property, editModel, rows, columns, readOnly, null);
  }

  /**
   * Creates a text area based on the given property
   * @param property the property
   * @param editModel the edit model to bind with the value
   * @param rows the number of rows
   * @param columns the number of columns
   * @param readOnly if true then the value is read only
   * @param enabledState a state indicating when the text area should be enabled
   * @return a text area
   */
  public static JTextArea createTextArea(final Property property, final EntityEditModel editModel,
                                         final int rows, final int columns, final boolean readOnly,
                                         final StateObserver enabledState) {
    Objects.requireNonNull(property, PROPERTY_PARAM_NAME);
    Objects.requireNonNull(editModel, EDIT_MODEL_PARAM_NAME);
    checkProperty(property, editModel);
    if (!property.isString()) {
      throw new IllegalArgumentException("Cannot create a text area for a non-string property");
    }

    final JTextArea textArea = rows > 0 && columns > 0 ? new JTextArea(rows, columns) : new JTextArea();
    textArea.setLineWrap(true);
    textArea.setWrapStyleWord(true);
    if (property.getMaxLength() > 0) {
      ((AbstractDocument) textArea.getDocument()).setDocumentFilter(new DocumentSizeFilter(property.getMaxLength()));
    }
    if (enabledState != null) {
      UiUtil.linkToEnabledState(enabledState, textArea);
    }

    ValueLinks.textValueLink(textArea, EditModelValues.<String>value(editModel, property), null, true, readOnly);
    ValueLinkValidators.addValidator(property, textArea, editModel);
    textArea.setToolTipText(property.getDescription());

    return textArea;
  }

  /**
   * Creates a text field based on the given property
   * @param property the property
   * @param editModel the edit model to bind with the value
   * @return a text field for the given property
   */
  public static JTextField createTextField(final Property property, final EntityEditModel editModel) {
    return createTextField(property, editModel, false, null, true);
  }

  /**
   * Creates a text field based on the given property
   * @param property the property
   * @param editModel the edit model to bind with the value
   * @param readOnly if true then the field will be read only
   * @param formatMaskString if specified the resulting text field is a JFormattedField with this mask
   * @param immediateUpdate if true then the value is committed on each keystroke, otherwise on focus lost
   * @return a text field for the given property
   */
  public static JTextField createTextField(final Property property, final EntityEditModel editModel,
                                           final boolean readOnly, final String formatMaskString,
                                           final boolean immediateUpdate) {
    return createTextField(property, editModel, readOnly, formatMaskString, immediateUpdate, null);
  }

  /**
   * Creates a text field based on the given property
   * @param property the property
   * @param editModel the edit model to bind with the value
   * @param readOnly if true then the field will be read only
   * @param formatMaskString if specified the resulting text field is a JFormattedField with this mask
   * @param immediateUpdate if true then the value is committed on each keystroke, otherwise on focus lost
   * @param enabledState the state controlling the enabled state of the panel
   * @return a text field for the given property
   */
  public static JTextField createTextField(final Property property, final EntityEditModel editModel,
                                           final boolean readOnly, final String formatMaskString,
                                           final boolean immediateUpdate, final StateObserver enabledState) {
    return createTextField(property, editModel, readOnly, formatMaskString, immediateUpdate, enabledState, false);
  }

  /**
   * Creates a text field based on the given property
   * @param property the property
   * @param editModel the edit model to bind with the value
   * @param readOnly if true then the field will be read only
   * @param formatMaskString if specified the resulting text field is a JFormattedField with this mask
   * @param immediateUpdate if true then the value is committed on each keystroke, otherwise on focus lost
   * @param enabledState the state controlling the enabled state of the panel
   * @param valueContainsLiteralCharacters whether or not the value should contain any literal characters
   * associated with a the format mask
   * @return a text field for the given property
   */
  public static JTextField createTextField(final Property property, final EntityEditModel editModel,
                                           final boolean readOnly, final String formatMaskString,
                                           final boolean immediateUpdate, final StateObserver enabledState,
                                           final boolean valueContainsLiteralCharacters) {
    Objects.requireNonNull(property, PROPERTY_PARAM_NAME);
    Objects.requireNonNull(editModel, EDIT_MODEL_PARAM_NAME);
    checkProperty(property, editModel);
    final JTextField textField = initializeTextField(property, editModel, enabledState, formatMaskString, valueContainsLiteralCharacters);
    if (property.isString()) {
      ValueLinks.textValueLink(textField, EditModelValues.<String>value(editModel, property), property.getFormat(), immediateUpdate, readOnly);
    }
    else if (property.isInteger()) {
      ValueLinks.integerValueLink((IntegerField) textField, EditModelValues.<Integer>value(editModel, property), false, readOnly, immediateUpdate);
    }
    else if (property.isDouble()) {
      ValueLinks.doubleValueLink((DoubleField) textField, EditModelValues.<Double>value(editModel, property), false, readOnly, immediateUpdate);
    }
    else if (property.isLong()) {
      ValueLinks.longValueLink((LongField) textField, EditModelValues.<Long>value(editModel, property), false, readOnly, immediateUpdate);
    }
    else if (property.isDateOrTime()) {
      ValueLinks.dateValueLink((JFormattedTextField) textField, EditModelValues.<Date>value(editModel, property),
              readOnly, (SimpleDateFormat) property.getFormat(), property.getType(), immediateUpdate);
    }
    else {
      throw new IllegalArgumentException("Not a text based property: " + property);
    }
    if (property.isString() && formatMaskString != null) {
      ValueLinkValidators.addFormattedValidator(property, textField, editModel);
    }
    else {
      ValueLinkValidators.addValidator(property, textField, editModel);
    }

    return textField;
  }

  /**
   * Creates a combo box based on the values of the given property
   * @param propertyId the propertyId
   * @param editModel the edit model to bind with the value
   * @return a combo box based on the property values
   */
  public static SteppedComboBox createPropertyComboBox(final String propertyId, final EntityEditModel editModel) {
    return createPropertyComboBox(propertyId, editModel, null);
  }

  /**
   * Creates a combo box based on the values of the given property
   * @param propertyId the propertyId
   * @param editModel the edit model to bind with the value
   * @param enabledState the state controlling the enabled state of the panel
   * @return a combo box based on the property values
   */
  public static SteppedComboBox createPropertyComboBox(final String propertyId, final EntityEditModel editModel,
                                                       final StateObserver enabledState) {
    return createPropertyComboBox(editModel.getEntities().getColumnProperty(editModel.getEntityId(), propertyId),
            editModel, enabledState);
  }

  /**
   * Creates a combo box based on the values of the given property
   * @param property the property
   * @param editModel the edit model to bind with the value
   * @return a combo box based on the property values
   */
  public static SteppedComboBox createPropertyComboBox(final Property.ColumnProperty property, final EntityEditModel editModel) {
    return createPropertyComboBox(property, editModel, null);
  }

  /**
   * Creates a combo box based on the values of the given property
   * @param property the property
   * @param editModel the edit model to bind with the value
   * @param enabledState the state controlling the enabled state of the panel
   * @return a combo box based on the property values
   */
  public static SteppedComboBox createPropertyComboBox(final Property.ColumnProperty property, final EntityEditModel editModel,
                                                       final StateObserver enabledState) {
    return createPropertyComboBox(property, editModel, enabledState, false);
  }

  /**
   * Creates a combo box based on the values of the given property
   * @param property the property
   * @param editModel the edit model to bind with the value
   * @param enabledState the state controlling the enabled state of the panel
   * @param editable if true then the combo box will be editable
   * @return a combo box based on the property values
   */
  public static SteppedComboBox createPropertyComboBox(final Property.ColumnProperty property, final EntityEditModel editModel,
                                                       final StateObserver enabledState, final boolean editable) {
    final SteppedComboBox comboBox = createComboBox(property, editModel,
            (ComboBoxModel) ((SwingEntityEditModel) editModel).getComboBoxModel(property.getPropertyId()), enabledState, editable);
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
    return createEastButtonPanel(entityComboBox, entityComboBox.createForeignKeyFilterControl(foreignKeyPropertyId),
            filterButtonTakesFocus);
  }

  /**
   * Creates a panel with centerComponent in the BorderLayout.CENTER position and a button based on buttonAction
   * in the BorderLayout.EAST position, with the button having size UiUtil.DIMENSION_TEXT_FIELD_SQUARE.
   * @param centerComponent the center component
   * @param buttonAction the button action
   * @param buttonFocusable if true then the button is focusable, otherwise not
   * @return a panel
   */
  public static JPanel createEastButtonPanel(final JComponent centerComponent, final Action buttonAction,
                                             final boolean buttonFocusable) {
    final JPanel panel = new JPanel(new BorderLayout());
    final JButton button = new JButton(buttonAction);
    button.setPreferredSize(UiUtil.DIMENSION_TEXT_FIELD_SQUARE);
    button.setFocusable(buttonFocusable);

    panel.add(centerComponent, BorderLayout.CENTER);
    panel.add(button, BorderLayout.EAST);

    return panel;
  }

  /**
   * Displays a popup menu containing the values of the given entity
   * @param entity the entity which values to display
   * @param component the component on which to display the popup menu
   * @param location the popup menu location
   * @param connectionProvider the connection provider for populating the values
   */
  public static void showEntityMenu(final Entity entity, final JComponent component, final Point location,
                                    final EntityConnectionProvider connectionProvider) {
    if (entity != null) {
      final JPopupMenu popupMenu = new JPopupMenu();
      populateEntityMenu(popupMenu, (Entity) entity.getCopy(), connectionProvider);
      popupMenu.show(component, location.x, location.y);
    }
  }

  private static JTextField initializeTextField(final Property property, final EntityEditModel editModel,
                                                final StateObserver enabledState, final String formatMaskString,
                                                final boolean valueContainsLiteralCharacters) {
    final JTextField field = initializeTextField(property, formatMaskString, valueContainsLiteralCharacters);
    UiUtil.linkToEnabledState(enabledState, field);
    if (EntityEditPanel.TRANSFER_FOCUS_ON_ENTER.get()) {
      UiUtil.transferFocusOnEnter(field);
    }
    field.setToolTipText(property.getDescription());
    if (property.getMaxLength() > 0 && field.getDocument() instanceof SizedDocument) {
      ((SizedDocument) field.getDocument()).setMaxLength(property.getMaxLength());
    }
    if (editModel.isLookupAllowed(property)) {
      UiUtil.addLookupDialog(field, editModel.getValueProvider(property));
    }

    return field;
  }

  private static JTextField initializeTextField(final Property property, final String formatMaskString, final boolean valueContainsLiteralCharacters) {
    final JTextField field;
    if (property.isInteger()) {
      field = initializeIntField(property);
    }
    else if (property.isDouble()) {
      field = initializeDoubleField(property);
    }
    else if (property.isLong()) {
      field = initializeLongField(property);
    }
    else if (property.isDateOrTime()) {
      field = UiUtil.createFormattedField(DateUtil.getDateMask((SimpleDateFormat) property.getFormat()));
    }
    else if (property.isString()) {
      field = initializeStringField(formatMaskString, valueContainsLiteralCharacters);
    }
    else {
      throw new IllegalArgumentException("Creating text fields for property type: " + property.getType() + " is not implemented");
    }

    return field;
  }

  private static JTextField initializeStringField(final String formatMaskString, final boolean valueContainsLiteralCharacters) {
    final JTextField field;
    if (formatMaskString == null) {
      field = new JTextField(new SizedDocument(), "", 0);
    }
    else {
      field = UiUtil.createFormattedField(formatMaskString, valueContainsLiteralCharacters);
    }

    return field;
  }

  private static JTextField initializeDoubleField(final Property property) {
    final DoubleField field = new DoubleField((DecimalFormat) cloneFormat((NumberFormat) property.getFormat()));
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

  private static void checkProperty(final Property property, final EntityEditModel editModel) {
    if (!property.getEntityId().equals(editModel.getEntityId())) {
      throw new IllegalArgumentException("Entity type mismatch: " + property.getEntityId() + ", should be: " + editModel.getEntityId());
    }
  }

  /**
   * Populates the given root menu with the property values of the given entity
   * @param rootMenu the menu to populate
   * @param entity the entity
   * @param connectionProvider if provided then lazy loaded entity references are loaded so that the full object graph can be shown
   */
  private static void populateEntityMenu(final JComponent rootMenu, final Entity entity,
                                         final EntityConnectionProvider connectionProvider) {
    final Entities entities = connectionProvider.getEntities();
    populatePrimaryKeyMenu(rootMenu, entity, new ArrayList<>(entities.getPrimaryKeyProperties(entity.getEntityId())));
    populateForeignKeyMenu(rootMenu, entity, connectionProvider, new ArrayList<>(entities.getForeignKeyProperties(entity.getEntityId())));
    populateValueMenu(rootMenu, entity, new ArrayList<>(entities.getProperties(entity.getEntityId(), true)), entities);
  }

  private static void populatePrimaryKeyMenu(final JComponent rootMenu, final Entity entity, final List<Property.ColumnProperty> primaryKeyProperties) {
    TextUtil.collate(primaryKeyProperties);
    for (final Property.ColumnProperty property : primaryKeyProperties) {
      final boolean modified = entity.isModified(property);
      final StringBuilder builder = new StringBuilder("[PK] ").append(property.getPropertyId()).append(": ").append(entity.getAsString(property));
      if (modified) {
        builder.append(getOriginalValue(entity, property));
      }
      final JMenuItem menuItem = new JMenuItem(builder.toString());
      setInvalidModified(menuItem, true, modified);
      menuItem.setToolTipText(property.getPropertyId());
      rootMenu.add(menuItem);
    }
  }

  private static void populateForeignKeyMenu(final JComponent rootMenu, final Entity entity,
                                             final EntityConnectionProvider connectionProvider,
                                             final List<Property.ForeignKeyProperty> fkProperties) {
    try {
      TextUtil.collate(fkProperties);
      final Entity.Validator validator = connectionProvider.getEntities().getValidator(entity.getEntityId());
      for (final Property.ForeignKeyProperty property : fkProperties) {
        final boolean fkValueNull = entity.isForeignKeyNull(property);
        final boolean isLoaded = entity.isLoaded(property.getPropertyId());
        final boolean valid = isValid(validator, entity, property);
        final boolean modified = entity.isModified(property);
        final String toolTipText = getForeignKeyColumnNames(property);
        if (!fkValueNull) {
          final Entity referencedEntity;
          if (isLoaded) {
            referencedEntity = entity.getForeignKey(property.getPropertyId());
          }
          else {
            referencedEntity = connectionProvider.getConnection().selectSingle(entity.getReferencedKey(property));
            entity.remove(property);
            entity.put(property, referencedEntity);
          }
          final StringBuilder builder = new StringBuilder("[FK").append(isLoaded ? "] " : "+] ").append(property.getCaption())
                  .append(": ").append(referencedEntity.toString());
          if (modified) {
            builder.append(getOriginalValue(entity, property));
          }
          final JMenu foreignKeyMenu = new JMenu(builder.toString());
          setInvalidModified(foreignKeyMenu, valid, modified);
          foreignKeyMenu.setToolTipText(toolTipText);
          populateEntityMenu(foreignKeyMenu, entity.getForeignKey(property.getPropertyId()), connectionProvider);
          rootMenu.add(foreignKeyMenu);
        }
        else {
          final StringBuilder builder = new StringBuilder("[FK] ").append(property.getCaption()).append(": <null>");
          if (modified) {
            builder.append(getOriginalValue(entity, property));
          }
          final JMenuItem menuItem = new JMenuItem(builder.toString());
          setInvalidModified(menuItem, valid, modified);
          menuItem.setToolTipText(toolTipText);
          rootMenu.add(menuItem);
        }
      }
    }
    catch (final DatabaseException e) {
      throw new RuntimeException(e);
    }
  }

  private static String getForeignKeyColumnNames(final Property.ForeignKeyProperty foreignKeyProperty) {
    final List<String> columnNames = new LinkedList<>();
    foreignKeyProperty.getProperties().forEach(property -> columnNames.add(property.getColumnName()));

    return String.join(", ", columnNames);
  }

  private static void populateValueMenu(final JComponent rootMenu, final Entity entity, final List<Property> properties,
                                        final Entities entities) {
    TextUtil.collate(properties);
    final int maxValueLength = 20;
    final Entity.Validator validator = entities.getValidator(entity.getEntityId());
    for (final Property property : properties) {
      final boolean valid = isValid(validator, entity, property);
      final boolean modified = entity.isModified(property);
      final boolean isForeignKeyProperty = property instanceof Property.ColumnProperty
              && ((Property.ColumnProperty) property).isForeignKeyProperty();
      if (!isForeignKeyProperty && !(property instanceof Property.ForeignKeyProperty)) {
        final String prefix = "[" + property.getTypeClass().getSimpleName().substring(0, 1)
                + (property instanceof Property.DerivedProperty ? "*" : "")
                + (property instanceof Property.DenormalizedProperty ? "+" : "") + "] ";
        final String value = entity.isValueNull(property) ? "<null>" : entity.getAsString(property);
        final boolean longValue = value != null && value.length() > maxValueLength;
        final StringBuilder builder = new StringBuilder(prefix).append(property).append(": ");
        if (longValue) {
          builder.append(value.substring(0, maxValueLength)).append("...");
        }
        else {
          builder.append(value);
        }
        if (modified) {
          builder.append(getOriginalValue(entity, property));
        }
        final JMenuItem menuItem = new JMenuItem(builder.toString());
        setInvalidModified(menuItem, valid, modified);
        final StringBuilder toolTipBuilder = new StringBuilder();
        if (property instanceof Property.ColumnProperty) {
          toolTipBuilder.append(property.getPropertyId());
        }
        if (longValue) {
          if (value.length() > MAXIMUM_VALUE_LENGTH) {
            toolTipBuilder.append(value.substring(0, MAXIMUM_VALUE_LENGTH));
          }
          else {
            toolTipBuilder.append(value);
          }
        }
        menuItem.setToolTipText(toolTipBuilder.toString());
        rootMenu.add(menuItem);
      }
    }
  }

  private static void setInvalidModified(final JMenuItem menuItem, final boolean valid, final boolean modified) {
    final Font currentFont = menuItem.getFont();
    if (!valid) {
      menuItem.setBackground(INVALID_COLOR);
      menuItem.setFont(new Font(currentFont.getName(), Font.BOLD, currentFont.getSize()));
    }
    if (modified) {
      menuItem.setFont(new Font(currentFont.getName(), currentFont.getStyle() | Font.ITALIC, currentFont.getSize()));
    }
  }

  private static String getOriginalValue(final Entity entity, final Property property) {
    final Object originalValue = entity.getOriginal(property);

    return " | " + (originalValue == null ? "<null>" : originalValue.toString());
  }

  private static boolean isValid(final Entity.Validator validator, final Entity entity, final Property property) {
    try {
      validator.validate(entity, property);
      return true;
    }
    catch (final ValidationException e) {
      return false;
    }
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

  private static final class LookupUIValue implements Value<Entity> {
    private final Event<Entity> changeEvent = Events.event();
    private final EntityLookupModel lookupModel;

    private LookupUIValue(final EntityLookupModel lookupModel) {
      this.lookupModel = lookupModel;
      this.lookupModel.addSelectedEntitiesListener(selected -> changeEvent.eventOccurred(selected.isEmpty() ? null : selected.iterator().next()));
    }

    @Override
    public void set(final Entity value) {
      lookupModel.setSelectedEntity(value);
    }

    @Override
    public Entity get() {
      final Collection<Entity> selectedEntities = lookupModel.getSelectedEntities();
      return selectedEntities.isEmpty() ? null : selectedEntities.iterator().next();
    }

    @Override
    public EventObserver<Entity> getObserver() {
      return changeEvent.getObserver();
    }
  }
}
