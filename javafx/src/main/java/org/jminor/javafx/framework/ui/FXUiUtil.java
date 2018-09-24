/*
 * Copyright (c) 2004 - 2018, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.javafx.framework.ui;

import org.jminor.common.EventObserver;
import org.jminor.common.Item;
import org.jminor.common.State;
import org.jminor.common.StateObserver;
import org.jminor.common.States;
import org.jminor.common.User;
import org.jminor.common.Util;
import org.jminor.common.Value;
import org.jminor.common.Values;
import org.jminor.common.i18n.Messages;
import org.jminor.common.model.CancelException;
import org.jminor.common.model.valuemap.EditModelValues;
import org.jminor.framework.db.EntityConnectionProvider;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.Property;
import org.jminor.framework.model.EntityLookupModel;
import org.jminor.javafx.framework.model.FXEntityEditModel;
import org.jminor.javafx.framework.model.FXEntityListModel;
import org.jminor.javafx.framework.model.ObservableEntityList;
import org.jminor.javafx.framework.ui.values.PropertyValues;
import org.jminor.javafx.framework.ui.values.StringValue;

import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.SortedList;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Control;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.PasswordField;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.ToggleButton;
import javafx.scene.image.ImageView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.Types;
import java.text.Format;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * A factory class for UI related things.
 */
public final class FXUiUtil {

  private FXUiUtil() {}

  /**
   * Displays a dialog for selecting one of the given values
   * @param values the values from which to choose
   * @param <T> the type of values
   * @return the selected value
   * @throws CancelException in case the user cancels the operation
   */
  public static <T> T selectValue(final List<T> values) {
    return selectValues(values, true).get(0);
  }

  /**
   * Displays a dialog for selecting one or more of the given values
   * @param values the values from which to choose
   * @param <T> the type of values
   * @return the selected values
   * @throws CancelException in case the user cancels the operation
   */
  public static <T> List<T> selectValues(final List<T> values) {
    return selectValues(values, false);
  }

  /**
   * Displays a dialog for selecting one or more of the given values
   * @param values the values from which to choose
   * @param <T> the type of values
   * @param single if true then only a single value can be selected
   * @return the selected values
   * @throws CancelException in case the user cancels the operation
   */
  public static <T> List<T> selectValues(final List<T> values, final boolean single) {
    final ListView<T> listView = new ListView<>(FXCollections.observableArrayList(values));
    listView.getSelectionModel().setSelectionMode(single ? SelectionMode.SINGLE : SelectionMode.MULTIPLE);
    final Dialog<List<T>> dialog = new Dialog<>();
    dialog.getDialogPane().setContent(listView);
    dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
    dialog.setResultConverter(buttonType -> {
      if (buttonType != null && buttonType.getButtonData() == ButtonBar.ButtonData.OK_DONE) {
        return listView.getSelectionModel().getSelectedItems();
      }

      return null;
    });
    listView.setOnMouseClicked(event -> {
      if (event.getClickCount() == 2) {
        ((Button) dialog.getDialogPane().lookupButton(ButtonType.OK)).fire();
      }
    });
    listView.setOnKeyPressed(event -> {
      switch (event.getCode()) {
        case ENTER:
          ((Button) dialog.getDialogPane().lookupButton(ButtonType.OK)).fire();
          break;
        case ESCAPE:
          ((Button) dialog.getDialogPane().lookupButton(ButtonType.CANCEL)).fire();
          break;
        default:
          break;
      }
    });

    Platform.runLater(listView::requestFocus);
    final Optional<List<T>> result = dialog.showAndWait();
    if (result.isPresent()) {
      final List<T> selected = result.get();
      if (!selected.isEmpty()) {
        return selected;
      }
    }

    throw new CancelException();
  }

  /**
   * Sets the given string as the system clipboard value
   * @param string the string to add to the clipboard
   */
  public static void setClipboard(final String string) {
    final ClipboardContent content = new ClipboardContent();
    content.putString(string);
    Clipboard.getSystemClipboard().setContent(content);
  }

  /**
   * Displays a confirmation dialog
   * @param message the message to display
   * @return true if confirmed
   */
  public static boolean confirm(final String message) {
    return confirm(null, null, message);
  }

  /**
   * Displays a confirmation dialog
   * @param headerText the dialog header text
   * @param message the message to display
   * @return true if confirmed
   */
  public static boolean confirm(final String headerText, final String message) {
    return confirm(null, headerText, message);
  }

  /**
   * Displays a confirmation dialog
   * @param title the dialog title
   * @param headerText the dialog header text
   * @param message the message to display
   * @return true if confirmed
   */
  public static boolean confirm(final String title, final String headerText, final String message) {
    Objects.requireNonNull(message);
    final Alert alert = new Alert(Alert.AlertType.CONFIRMATION, message, ButtonType.OK, ButtonType.CANCEL);
    if (title != null) {
      alert.setTitle(title);
    }
    if (headerText != null) {
      alert.setHeaderText(headerText);
    }
    final Optional<ButtonType> buttonType = alert.showAndWait();

    return buttonType.isPresent() && buttonType.get() == ButtonType.OK;
  }

  /**
   * Instantiates a {@link Value} instance based on the given property, linked to the given control
   * @param property the property
   * @param control the control
   * @param defaultValue the default to set after instantiation
   * @return the {@link Value} instance
   */
  public static Value createValue(final Property property, final Control control, final Object defaultValue) {
    if (property instanceof Property.ForeignKeyProperty) {
      if (control instanceof ComboBox) {
        final Value<Entity> entityValue = PropertyValues.selectedValue(((ComboBox<Entity>) control).getSelectionModel());
        entityValue.set((Entity) defaultValue);
        return entityValue;
      }
      else if (control instanceof EntityLookupField) {
        final Value<Collection<Entity>> entityValue = PropertyValues.lookupValue(((EntityLookupField) control).getModel());
        entityValue.set(defaultValue == null ? Collections.emptyList() : Collections.singletonList((Entity) defaultValue));
        return entityValue;
      }
    }
    if (property instanceof Property.ValueListProperty) {
      final Value listValue = PropertyValues.selectedItemValue(((ComboBox<Item>) control).getSelectionModel());
      listValue.set(defaultValue);
      return listValue;
    }

    switch (property.getType()) {
      case Types.BOOLEAN:
        final Value<Boolean> booleanValue = PropertyValues.booleanPropertyValue(((CheckBox) control).selectedProperty());
        booleanValue.set((Boolean) defaultValue);
        return booleanValue;
      case Types.DATE:
      case Types.TIMESTAMP:
      case Types.TIME:
        final Value<Date> dateValue = Values.value((java.sql.Date) defaultValue);
        final StringValue<LocalDate> value = createDateValue(property, (DatePicker) control);
        Values.link(createLocalDateValue(dateValue), value);
        return dateValue;
      case Types.DOUBLE:
        final StringValue<Double> doubleValue = createDoubleValue(property, (TextField) control);
        doubleValue.set((Double) defaultValue);
        return doubleValue;
      case Types.INTEGER:
        final StringValue<Integer> integerValue = createIntegerValue(property, (TextField) control);
        integerValue.set((Integer) defaultValue);
        return integerValue;
      case Types.BIGINT:
        final StringValue<Long> longValue = createLongValue(property, (TextField) control);
        longValue.set((Long) defaultValue);
        return longValue;
      case Types.CHAR:
      case Types.VARCHAR:
        final StringValue<String> stringValue = createStringValue((TextField) control);
        stringValue.set((String) defaultValue);
        return stringValue;
      default:
        throw new IllegalArgumentException("Unsupported property type: " + property.getType());
    }
  }

  /**
   * Instantiates a {@link Value} instance based on the given {@link ComboBox}
   * @param comboBox the combo box on which to base the value
   * @return the {@link Value} instance
   */
  public static Value<Entity> createEntityValue(final ComboBox<Entity> comboBox) {
    return PropertyValues.selectedValue(comboBox.getSelectionModel());
  }

  /**
   * Instantiates a {@link ToggleButton} based on the given {@link State} instance
   * @param state the state on which to base the toggle button
   * @return a {@link ToggleButton} based on the given state
   */
  public static ToggleButton createToggleButton(final State state) {
    final ToggleButton button = new ToggleButton();
    final Value<Boolean> checkBoxValue = PropertyValues.booleanPropertyValue(button.selectedProperty());
    final Value<Boolean> stateValue = Values.stateValue(state);
    Values.link(stateValue, checkBoxValue);

    return button;
  }

  /**
   * Instantiates a {@link CheckBox} based on the given {@link State} instance
   * @param state the state on which to base the check box
   * @return a {@link CheckBox} based on the given state
   */
  public static CheckBox createCheckBox(final State state) {
    final CheckBox box = new CheckBox();
    final Value<Boolean> checkBoxValue = PropertyValues.booleanPropertyValue(box.selectedProperty());
    final Value<Boolean> stateValue = Values.stateValue(state);
    Values.link(stateValue, checkBoxValue);

    return box;
  }

  /**
   * Instantiates a {@link Control} based on the given property.
   * @param property the property
   * @param connectionProvider the {@link EntityConnectionProvider} instance to use
   * @return a {@link Control} based on the given property
   */
  public static Control createControl(final Property property, final EntityConnectionProvider connectionProvider) {
    if (property instanceof Property.ForeignKeyProperty) {
      return new ComboBox<>(createEntityListModel((Property.ForeignKeyProperty) property, connectionProvider));
    }
    if (property instanceof Property.ValueListProperty) {
      return new ComboBox<>(createValueListComboBoxModel((Property.ValueListProperty) property));
    }

    switch (property.getType()) {
      case Types.BOOLEAN:
        return createCheckBox();
      case Types.DATE:
      case Types.TIMESTAMP:
      case Types.TIME:
        return createDatePicker();
      case Types.DOUBLE:
      case Types.INTEGER:
      case Types.BIGINT:
      case Types.CHAR:
      case Types.VARCHAR:
        return createTextField(property);
      default:
        throw new IllegalArgumentException("Unsupported property type: " + property.getType());
    }
  }

  /**
   * Instantiates a {@link CheckBox} based on the given property, linked to the given edit model
   * @param property the property
   * @param editModel the edit model
   * @return a {@link CheckBox} based on the given property and edit model
   */
  public static CheckBox createCheckBox(final Property property, final FXEntityEditModel editModel) {
    return createCheckBox(property, editModel, null);
  }

  /**
   * Instantiates a {@link CheckBox} based on the given property, linked to the given edit model
   * @param property the property
   * @param editModel the edit model
   * @param enabledState the {@link State} instance controlling the enabled state of the check box
   * @return a {@link CheckBox} based on the given property and edit model
   */
  public static CheckBox createCheckBox(final Property property, final FXEntityEditModel editModel,
                                        final StateObserver enabledState) {
    final CheckBox checkBox = createCheckBox(enabledState);
    final Value<Boolean> propertyValue = createBooleanValue(checkBox);
    Values.link(EditModelValues.value(editModel, property), propertyValue);

    return checkBox;
  }

  /**
   * Instantiates a {@link Value} instance based on the value of the given {@link CheckBox}
   * @param checkBox the check box on which to base the value
   * @return a {@link Value} based on the given check box
   */
  public static Value<Boolean> createBooleanValue(final CheckBox checkBox) {
    return PropertyValues.booleanPropertyValue(checkBox.selectedProperty());
  }

  /**
   * Instantiates a {@link TextField} based on the given property, linked to the given edit model
   * @param property the property
   * @param editModel the edit model
   * @return a {@link TextField} based on the given property
   */
  public static TextField createTextField(final Property property, final FXEntityEditModel editModel) {
    return createTextField(property, editModel, null);
  }

  /**
   * Instantiates a {@link TextField} based on the given property, linked to the given edit model
   * @param property the property
   * @param editModel the edit model
   * @param enabledState the {@link State} instance controlling the enabled state of the text field
   * @return a {@link TextField} based on the given property
   */
  public static TextField createTextField(final Property property, final FXEntityEditModel editModel,
                                          final StateObserver enabledState) {
    final TextField textField = createTextField(property, enabledState);
    final StringValue<String> propertyValue = createStringValue(textField);
    Values.link(EditModelValues.value(editModel, property), propertyValue);

    return textField;
  }

  /**
   * Instantiates a {@link StringValue} based on the given text field
   * @param textField the text field
   * @return a {@link StringValue} based on the given text field
   */
  public static StringValue<String> createStringValue(final TextField textField) {
    return PropertyValues.stringPropertyValue(textField.textProperty());
  }

  /**
   * Instantiates a {@link TextField} for {@link Long} values, based on the given property and linked to the given edit model
   * @param property the property
   * @param editModel the edit model
   * @return a {@link TextField} for {@link Long} values, based on the given property
   */
  public static TextField createLongField(final Property property, final FXEntityEditModel editModel) {
    return createLongField(property, editModel, null);
  }

  /**
   * Instantiates a {@link TextField} for {@link Long} values, based on the given property and linked to the given edit model
   * @param property the property
   * @param editModel the edit model
   * @param enabledState the {@link State} instance controlling the enabled state of the text field
   * @return a {@link TextField} for {@link Long} values, based on the given property
   */
  public static TextField createLongField(final Property property, final FXEntityEditModel editModel,
                                          final StateObserver enabledState) {
    final TextField textField = createTextField(property, enabledState);
    final StringValue<Long> propertyValue = createLongValue(property, textField);
    Values.link(EditModelValues.value(editModel, property), propertyValue);

    return textField;
  }

  /**
   * Instantiates a {@link StringValue} for {@link Long} values, based on the given property and linked to the given text field
   * @param property the property
   * @param textField the text field
   * @return a {@link StringValue} for {@link Long} values, based on the given property
   */
  public static StringValue<Long> createLongValue(final Property property, final TextField textField) {
    final StringValue<Long> propertyValue = PropertyValues.longPropertyValue(textField.textProperty(),
            (NumberFormat) property.getFormat());
    textField.textFormatterProperty().setValue(new TextFormatter(propertyValue.getConverter()));

    return propertyValue;
  }

  /**
   * Instantiates a {@link TextField} for {@link Integer} values, based on the given property and linked to the given edit model
   * @param property the property
   * @param editModel the edit model
   * @return a {@link TextField} for {@link Integer} values, based on the given property
   */
  public static TextField createIntegerField(final Property property, final FXEntityEditModel editModel) {
    return createIntegerField(property, editModel, null);
  }

  /**
   * Instantiates a {@link TextField} for {@link Integer} values, based on the given property and linked to the given edit model
   * @param property the property
   * @param editModel the edit model
   * @param enabledState the {@link State} instance controlling the enabled state of the text field
   * @return a {@link TextField} for {@link Integer} values, based on the given property
   */
  public static TextField createIntegerField(final Property property, final FXEntityEditModel editModel,
                                             final StateObserver enabledState) {
    final TextField textField = createTextField(property, enabledState);
    final StringValue<Integer> propertyValue = createIntegerValue(property, textField);
    Values.link(EditModelValues.value(editModel, property), propertyValue);

    return textField;
  }

  /**
   * Instantiates a {@link StringValue} for {@link Integer} values, based on the given property and linked to the given text field
   * @param property the property
   * @param textField the text field
   * @return a {@link StringValue} for {@link Integer} values, based on the given property
   */
  public static StringValue<Integer> createIntegerValue(final Property property, final TextField textField) {
    final StringValue<Integer> propertyValue = PropertyValues.integerPropertyValue(textField.textProperty(),
            (NumberFormat) property.getFormat());
    textField.textFormatterProperty().setValue(new TextFormatter(propertyValue.getConverter()));

    return propertyValue;
  }

  /**
   * Instantiates a {@link TextField} for {@link Double} values, based on the given property and linked to the given edit model
   * @param property the property
   * @param editModel the edit model
   * @return a {@link TextField} for {@link Double} values, based on the given property
   */
  public static TextField createDoubleField(final Property property, final FXEntityEditModel editModel) {
    return createDoubleField(property, editModel, null);
  }

  /**
   * Instantiates a {@link TextField} for {@link Double} values, based on the given property and linked to the given edit model
   * @param property the property
   * @param editModel the edit model
   * @param enabledState the {@link State} instance controlling the enabled state of the text field
   * @return a {@link TextField} for {@link Double} values, based on the given property
   */
  public static TextField createDoubleField(final Property property, final FXEntityEditModel editModel,
                                            final StateObserver enabledState) {
    final TextField textField = createTextField(property, enabledState);
    final StringValue<Double> propertyValue = createDoubleValue(property, textField);
    Values.link(EditModelValues.value(editModel, property), propertyValue);

    return textField;
  }

  /**
   * Instantiates a {@link StringValue} for {@link Double} values, based on the given property and linked to the given text field
   * @param property the property
   * @param textField the text field
   * @return a {@link StringValue} for {@link Double} values, based on the given property
   */
  public static StringValue<Double> createDoubleValue(final Property property, final TextField textField) {
    final StringValue<Double> propertyValue = PropertyValues.doublePropertyValue(textField.textProperty(),
            (NumberFormat) property.getFormat());
    textField.textFormatterProperty().setValue(new TextFormatter(propertyValue.getConverter()));

    return propertyValue;
  }

  /**
   * Instantiates a {@link DatePicker} based on the given property and linked to the given edit model
   * @param property the property
   * @param editModel the edit model
   * @return a {@link DatePicker} based on the given property
   */
  public static DatePicker createDatePicker(final Property property, final FXEntityEditModel editModel) {
    return createDatePicker(property, editModel, null);
  }

  /**
   * Instantiates a {@link DatePicker} based on the given property and linked to the given edit model
   * @param property the property
   * @param editModel the edit model
   * @param enabledState the {@link State} instance controlling the enabled state of the date picker
   * @return a {@link DatePicker} based on the given property
   */
  public static DatePicker createDatePicker(final Property property, final FXEntityEditModel editModel,
                                            final StateObserver enabledState) {
    final DatePicker picker = createDatePicker(enabledState);
    final StringValue<LocalDate> value = createDateValue(property, picker);

    Values.link(new LocalDateValue(EditModelValues.value(editModel, property)), value);

    return picker;
  }

  /**
   * Instantiates a {@link StringValue} for {@link LocalDate} values, based on the given property and linked to the given text field
   * @param property the property
   * @param picker the date picker
   * @return a {@link StringValue} for {@link LocalDate} values, based on the given property
   */
  public static StringValue<LocalDate> createDateValue(final Property property, final DatePicker picker) {
    final SimpleDateFormat dateFormat = (SimpleDateFormat) property.getFormat();
    final StringValue<LocalDate> dateValue = PropertyValues.datePropertyValue(picker.getEditor().textProperty(), dateFormat);

    picker.setConverter(dateValue.getConverter());
    picker.setPromptText(dateFormat.toPattern().toLowerCase());

    return dateValue;
  }

  /**
   * Instantiates a {@link Value} for {@link LocalDate} values, based on the given {@link Date} based {@link Value}
   * @param dateValue the {@link Date} based {@link Value}
   * @return a {@link StringValue} for {@link LocalDate} values, based on the given value
   */
  public static Value<LocalDate> createLocalDateValue(final Value<Date> dateValue) {
    return new LocalDateValue(dateValue);
  }

  /**
   * Links the given boolean property to the given state observer, so that changes is one are reflected in the other
   * @param property the boolean property
   * @param stateObserver the state observer
   */
  public static void link(final BooleanProperty property, final StateObserver stateObserver) {
    Objects.requireNonNull(property);
    Objects.requireNonNull(stateObserver);
    property.setValue(stateObserver.isActive());
    stateObserver.addDataListener(property::setValue);
  }

  /**
   * Instantiates a {@link EntityLookupField} based on the given property and linked to the given edit model
   * @param foreignKeyProperty the foreign key property
   * @param editModel the edit model
   * @return a {@link EntityLookupField} based on the given property
   */
  public static EntityLookupField createLookupField(final Property.ForeignKeyProperty foreignKeyProperty,
                                                    final FXEntityEditModel editModel) {
    final EntityLookupModel lookupModel = editModel.getForeignKeyLookupModel(foreignKeyProperty);
    final EntityLookupField lookupField = new EntityLookupField(lookupModel);
    Values.link(EditModelValues.value(editModel, foreignKeyProperty), PropertyValues.lookupValue(lookupModel));

    return lookupField;
  }

  /**
   * Instantiates a {@link ComboBox} based on the given property and linked to the given edit model
   * @param foreignKeyProperty the foreign key property
   * @param editModel the edit model
   * @return a {@link ComboBox} based on the given property
   */
  public static ComboBox<Entity> createForeignKeyComboBox(final Property.ForeignKeyProperty foreignKeyProperty,
                                                          final FXEntityEditModel editModel) {
    final FXEntityListModel listModel = editModel.getForeignKeyListModel(foreignKeyProperty);
    listModel.refresh();
    final ComboBox<Entity> box = new ComboBox<>(listModel.getSortedList());
    listModel.setSelectionModel(box.getSelectionModel());
    Values.link(EditModelValues.value(editModel, foreignKeyProperty), PropertyValues.selectedValue(box.getSelectionModel()));

    return box;
  }

  /**
   * Instantiates a {@link ComboBox} based on the values of the given property and linked to the given edit model
   * @param valueListProperty the property
   * @param editModel the edit model
   * @return a {@link ComboBox} based on the values of the given property
   */
  public static ComboBox<Item> createValueListComboBox(final Property.ValueListProperty valueListProperty,
                                                       final FXEntityEditModel editModel) {
    final ComboBox<Item> comboBox = new ComboBox<>(createValueListComboBoxModel(valueListProperty));
    Values.link(EditModelValues.value(editModel, valueListProperty), PropertyValues.selectedItemValue(comboBox.getSelectionModel()));
    return comboBox;
  }

  /**
   * Instantiates a new {@link CheckBox} instance
   * @return the check box
   */
  public static CheckBox createCheckBox() {
    return createCheckBox(null);
  }

  /**
   * Instantiates a new {@link CheckBox} instance
   * @param enabledState the {@link State} instance controlling the enabled state of the check box
   * @return the check box
   */
  public static CheckBox createCheckBox(final StateObserver enabledState) {
    final CheckBox checkBox = new CheckBox();
    if (enabledState != null) {
      link(checkBox.disableProperty(), enabledState.getReversedObserver());
    }

    return checkBox;
  }

  /**
   * Instantiates a {@link ObservableList} containing the {@link Item}s associated with the given value list property
   * @param property the property
   * @return a {@link ObservableList} containing the {@link Item}s associated with the given value list property
   */
  public static ObservableList<Item> createValueListComboBoxModel(final Property.ValueListProperty property) {
    return new SortedList<>(FXCollections.observableArrayList(property.getValues()),
            Comparator.comparing(Item::toString));
  }

  /**
   * Instantiates a {@link TextField} based on the given property
   * @param property the property
   * @return a {@link TextField} based on the given property
   */
  public static TextField createTextField(final Property property) {
    return createTextField(property, (StateObserver) null);
  }

  /**
   * Instantiates a {@link TextField} based on the given property
   * @param property the property
   * @param enabledState the {@link State} instance controlling the enabled state of the text field
   * @return a {@link TextField} based on the given property
   */
  public static TextField createTextField(final Property property, final StateObserver enabledState) {
    final TextField textField = new TextField();
    textField.textProperty().addListener(new ValidationChangeListener(property, textField.textProperty()));
    if (enabledState != null) {
      link(textField.disableProperty(), enabledState.getReversedObserver());
    }

    return textField;
  }

  /**
   * @return a {@link DatePicker} instance
   */
  public static DatePicker createDatePicker() {
    return createDatePicker(null);
  }

  /**
   * @param enabledState the {@link State} instance controlling the enabled state of the date picker
   * @return a {@link DatePicker} instance
   */
  public static DatePicker createDatePicker(final StateObserver enabledState) {
    final DatePicker picker = new DatePicker();
    if (enabledState != null) {
      link(picker.disableProperty(), enabledState.getReversedObserver());
    }

    return picker;
  }

  /**
   * Displays a login dialog
   * @param applicationTitle the title to display
   * @param defaultUser the default user to display in the dialog
   * @param icon the icon, if any
   * @return a {@link User} instance based on the values found in the dialog
   * @throws CancelException in case the user cancels the operation
   */
  public static User showLoginDialog(final String applicationTitle, final User defaultUser, final ImageView icon) {
    final Dialog<User> dialog = new Dialog<>();
    dialog.setTitle(Messages.get(Messages.LOGIN));
    dialog.setHeaderText(applicationTitle);
    dialog.setGraphic(icon);

    final ButtonType loginButtonType = new ButtonType(Messages.get(Messages.OK), ButtonBar.ButtonData.OK_DONE);
    dialog.getDialogPane().getButtonTypes().addAll(loginButtonType, ButtonType.CANCEL);

    final GridPane grid = new GridPane();
    grid.setHgap(10);
    grid.setVgap(10);
    grid.setPadding(new Insets(20, 150, 10, 10));

    final TextField username = new TextField(defaultUser == null ? "" : defaultUser.getUsername());
    username.setPromptText(Messages.get(Messages.USERNAME));
    final PasswordField password = new PasswordField();
    password.setText(defaultUser == null || defaultUser.getPassword() == null ? "" : String.valueOf(defaultUser.getPassword()));
    password.setPromptText(Messages.get(Messages.PASSWORD));

    grid.add(new Label(Messages.get(Messages.USERNAME)), 0, 0);
    grid.add(username, 1, 0);
    grid.add(new Label(Messages.get(Messages.PASSWORD)), 0, 1);
    grid.add(password, 1, 1);

    final Node loginButton = dialog.getDialogPane().lookupButton(loginButtonType);
    loginButton.setDisable(password.textProperty().getValue().trim().isEmpty() ||
            username.textProperty().getValue().trim().isEmpty());

    final ChangeListener<String> usernamePasswordListener = (observable, oldValue, newValue) ->
            loginButton.setDisable(password.textProperty().getValue().trim().isEmpty() ||
                    username.textProperty().getValue().trim().isEmpty());

    password.textProperty().addListener(usernamePasswordListener);
    username.textProperty().addListener(usernamePasswordListener);

    dialog.getDialogPane().setContent(grid);

    Platform.runLater(password::requestFocus);

    dialog.setResultConverter(dialogButton -> {
      if (dialogButton == loginButtonType) {
        return new User(username.getText(), password.getText().toCharArray());
      }

      return null;
    });

    final Optional<User> result = dialog.showAndWait();
    if (result.isPresent()) {
      return result.get();
    }

    throw new CancelException();
  }

  /**
   * Displays an exception dialog for the given exception
   * @param exception the exception to display
   */
  public static void showExceptionDialog(final Throwable exception) {
    final Alert alert = new Alert(Alert.AlertType.ERROR);
    alert.setTitle(Messages.get(Messages.EXCEPTION));
    alert.setHeaderText(exception.getClass().getSimpleName());
    alert.setContentText(exception.getMessage());

    final StringWriter stringWriter = new StringWriter();
    exception.printStackTrace(new PrintWriter(stringWriter));

    final TextArea stackTraceArea = new TextArea(stringWriter.toString());
    stackTraceArea.setEditable(false);
    stackTraceArea.setWrapText(true);

    stackTraceArea.setMaxWidth(Double.MAX_VALUE);
    stackTraceArea.setMaxHeight(Double.MAX_VALUE);
    GridPane.setVgrow(stackTraceArea, Priority.ALWAYS);
    GridPane.setHgrow(stackTraceArea, Priority.ALWAYS);

    final GridPane expandableContent = new GridPane();
    expandableContent.setMaxWidth(Double.MAX_VALUE);
    expandableContent.add(stackTraceArea, 0, 1);

    final DialogPane dialogPane = alert.getDialogPane();
    dialogPane.setExpandableContent(expandableContent);
    dialogPane.expandedProperty().addListener(value ->
            Platform.runLater(() -> {
              dialogPane.requestLayout();
              dialogPane.getScene().getWindow().sizeToScene();
            })
    );

    Platform.runLater(alert::showAndWait);
  }

  /**
   * Finds and returns the first parent of {@code node} of the given type
   * @param node the child node
   * @param clazz the type to find
   * @param <T> the type of the parent
   * @return the parent, or null if none is found
   */
  public static <T> T getParentOfType(final Node node, final Class<T> clazz) {
    Objects.requireNonNull(node);
    Objects.requireNonNull(clazz);
    Parent parent = node.getParent();
    while (parent != null && !parent.getClass().equals(clazz)) {
      parent = parent.getParent();
    }

    return (T) parent;
  }

  private static SortedList<Entity> createEntityListModel(final Property.ForeignKeyProperty property,
                                                          final EntityConnectionProvider connectionProvider) {
    final ObservableEntityList entityList = new ObservableEntityList(property.getForeignEntityId(),
            connectionProvider);
    entityList.refresh();

    return entityList.getSortedList();
  }

  private static final class LocalDateValue implements Value<LocalDate> {

    private final Value dateValue;

    private LocalDateValue(final Value<Date> dateValue) {
      this.dateValue = dateValue;
    }

    @Override
    public void set(final LocalDate value) {
      if (value == null) {
        dateValue.set(null);
      }
      else {
        dateValue.set(Date.from(value.atStartOfDay(ZoneId.systemDefault()).toInstant()));
      }
    }

    @Override
    public LocalDate get() {
      return toLocalDate((Date) dateValue.get());
    }

    @Override
    public EventObserver<LocalDate> getObserver() {
      return dateValue.getObserver();
    }

    private static LocalDate toLocalDate(final Date date) {
      if (date == null) {
        return null;
      }

      return Instant.ofEpochMilli(date.getTime()).atZone(ZoneId.systemDefault()).toLocalDate();
    }
  }

  private static final class ValidationChangeListener implements ChangeListener<String> {

    private final Property property;
    private final StringProperty stringProperty;
    private final State ignoreChange = States.state();

    private ValidationChangeListener(final Property property, final StringProperty stringProperty) {
      this.property = property;
      this.stringProperty = stringProperty;
    }

    @Override
    public void changed(final ObservableValue<? extends String> observable, final String oldValue, final String newValue) {
      if (ignoreChange.isActive()) {
        return;
      }
      if (!isValid(property, newValue)) {
        Platform.runLater(() -> {
          try {
            ignoreChange.setActive(true);
            stringProperty.setValue(oldValue);
          }
          finally {
            ignoreChange.setActive(false);
          }
        });
      }
    }

    private static boolean isValid(final Property property, final String value) {
      final int maxLength = property.getMaxLength();
      if (maxLength > -1 && value != null && value.length() > maxLength) {
        return false;
      }
      final Format format = property.getFormat();
      Object parsedValue = null;
      try {
        if (format != null && value != null) {
          parsedValue = PropertyValues.parseStrict(format, value);
        }
      }
      catch (final NumberFormatException | ParseException e) {
        return false;
      }
      if (parsedValue != null && property.isNumerical() && !isWithinRange(property, (Number) parsedValue)) {
        return false;
      }
      if (parsedValue instanceof Double && !Objects.equals(parsedValue,
              Util.roundDouble((Double) parsedValue, property.getMaximumFractionDigits()))) {
        return false;
      }

      return true;
    }

    private static boolean isWithinRange(final Property property, final Number value) {
      final double min = property.getMin() != null ? Math.min(property.getMin(), 0) : Double.NEGATIVE_INFINITY;
      final double max = property.getMax() == null ? Double.POSITIVE_INFINITY : property.getMax();
      final double doubleValue = value.doubleValue();

      return doubleValue >= min && doubleValue <= max;
    }
  }
}
