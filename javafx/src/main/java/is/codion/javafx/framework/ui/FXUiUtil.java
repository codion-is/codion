/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.javafx.framework.ui;

import is.codion.common.Util;
import is.codion.common.i18n.Messages;
import is.codion.common.item.Item;
import is.codion.common.model.CancelException;
import is.codion.common.state.State;
import is.codion.common.state.StateObserver;
import is.codion.common.user.User;
import is.codion.common.value.Value;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.domain.entity.Attribute;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.ForeignKey;
import is.codion.framework.domain.property.ForeignKeyProperty;
import is.codion.framework.domain.property.ItemProperty;
import is.codion.framework.domain.property.Property;
import is.codion.framework.model.EntitySearchModel;
import is.codion.javafx.framework.model.FXEntityEditModel;
import is.codion.javafx.framework.model.FXEntityListModel;
import is.codion.javafx.framework.model.ObservableEntityList;
import is.codion.javafx.framework.ui.values.PropertyValues;
import is.codion.javafx.framework.ui.values.StringValue;

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
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.Format;
import java.text.NumberFormat;
import java.text.ParseException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Objects.requireNonNull;

/**
 * A factory class for UI related things.
 */
public final class FXUiUtil {

  /**
   * Specifies whether single item selection is enabled when selecting files and or directories.
   */
  public enum SingleSelection {
    /**
     * Single selection is enabled.
     */
    YES,
    /**
     * Single selection is not enabled.
     */
    NO
  }

  private FXUiUtil() {}

  /**
   * Displays a dialog for selecting one of the given values
   * @param values the values from which to choose
   * @param <T> the type of values
   * @return the selected value
   * @throws CancelException in case the user cancels the operation
   */
  public static <T> T selectValue(final List<T> values) {
    return selectValues(values, SingleSelection.YES).get(0);
  }

  /**
   * Displays a dialog for selecting one or more of the given values
   * @param values the values from which to choose
   * @param <T> the type of values
   * @return the selected values
   * @throws CancelException in case the user cancels the operation
   */
  public static <T> List<T> selectValues(final List<T> values) {
    return selectValues(values, SingleSelection.NO);
  }

  /**
   * Displays a dialog for selecting one or more of the given values
   * @param values the values from which to choose
   * @param <T> the type of values
   * @param singleSelection if yes then only a single value can be selected
   * @return the selected values
   * @throws CancelException in case the user cancels the operation
   */
  public static <T> List<T> selectValues(final List<T> values, final SingleSelection singleSelection) {
    final ListView<T> listView = new ListView<>(FXCollections.observableArrayList(values));
    listView.getSelectionModel().setSelectionMode(singleSelection == SingleSelection.YES ? SelectionMode.SINGLE : SelectionMode.MULTIPLE);
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
    requireNonNull(message);
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
   * @param <T> the value type
   * @return the {@link Value} instance
   */
  public static <T> Value<T> createValue(final Property<T> property, final Control control, final T defaultValue) {
    if (property instanceof ForeignKeyProperty) {
      if (control instanceof ComboBox) {
        final Value<Entity> entityValue = PropertyValues.selectedValue(((ComboBox<Entity>) control).getSelectionModel());
        entityValue.set((Entity) defaultValue);

        return (Value<T>) entityValue;
      }
      else if (control instanceof EntitySearchField) {
        final Value<List<Entity>> entityValue = PropertyValues.multipleSearchValue(((EntitySearchField) control).getModel());
        entityValue.set(defaultValue == null ? emptyList() : singletonList((Entity) defaultValue));

        return (Value<T>) entityValue;
      }
    }
    if (property instanceof ItemProperty) {
      final Value<T> listValue = PropertyValues.selectedItemValue(((ComboBox<Item<T>>) control).getSelectionModel());
      listValue.set(defaultValue);

      return listValue;
    }

    final Attribute<?> attribute = property.getAttribute();
    if (attribute.isBoolean()) {
      final Value<Boolean> booleanValue = PropertyValues.booleanPropertyValue(((CheckBox) control).selectedProperty());
      booleanValue.set((Boolean) defaultValue);

      return (Value<T>) booleanValue;
    }
    if (attribute.isLocalDate()) {
      final Value<LocalDate> dateValue = Value.value((LocalDate) defaultValue);
      createDateValue((Property<LocalDate>) property, (DatePicker) control).link(dateValue);

      return (Value<T>) dateValue;
    }
    if (attribute.isLocalDateTime()) {
      final Value<LocalDateTime> dateTimeValue = Value.value((LocalDateTime) defaultValue);
      createTimestampValue((Property<LocalDateTime>) property, (TextField) control).link(dateTimeValue);

      return (Value<T>) dateTimeValue;
    }
    if (attribute.isLocalTime()) {
      final Value<LocalTime> timeValue = Value.value((LocalTime) defaultValue);
      createTimeValue((Property<LocalTime>) property, (TextField) control).link(timeValue);

      return (Value<T>) timeValue;
    }
    if (attribute.isDouble()) {
      final StringValue<Double> doubleValue = createDoubleValue((Property<Double>) property, (TextField) control);
      doubleValue.set((Double) defaultValue);

      return (Value<T>) doubleValue;
    }
    if (attribute.isBigDecimal()) {
      final StringValue<BigDecimal> bigDecimalValue = createBigDecimalValue((Property<BigDecimal>) property, (TextField) control);
      bigDecimalValue.set((BigDecimal) defaultValue);

      return (Value<T>) bigDecimalValue;
    }
    if (attribute.isInteger()) {
      final StringValue<Integer> integerValue = createIntegerValue((Property<Integer>) property, (TextField) control);
      integerValue.set((Integer) defaultValue);

      return (Value<T>) integerValue;
    }
    if (attribute.isLong()) {
      final StringValue<Long> longValue = createLongValue((Property<Long>) property, (TextField) control);
      longValue.set((Long) defaultValue);

      return (Value<T>) longValue;
    }
    if (attribute.isString() || attribute.isCharacter()) {
      final StringValue<String> stringValue = createStringValue((TextField) control);
      stringValue.set((String) defaultValue);

      return (Value<T>) stringValue;
    }

    throw new IllegalArgumentException("Unsupported property type: " + attribute.getTypeClass());
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
    checkBoxValue.link(state);

    return button;
  }

  /**
   * Instantiates a {@link CheckBox} based on the given {@link State} instance
   * @param state the state on which to base the check-box
   * @return a {@link CheckBox} based on the given state
   */
  public static CheckBox createCheckBox(final State state) {
    final CheckBox box = new CheckBox();
    final Value<Boolean> checkBoxValue = PropertyValues.booleanPropertyValue(box.selectedProperty());
    checkBoxValue.link(state);

    return box;
  }

  /**
   * Instantiates a {@link Control} based on the given property.
   * @param property the property
   * @param connectionProvider the {@link EntityConnectionProvider} instance to use
   * @param <T> the value type
   * @return a {@link Control} based on the given property
   */
  public static <T> Control createControl(final Property<T> property, final EntityConnectionProvider connectionProvider) {
    if (property instanceof ForeignKeyProperty) {
      return new ComboBox<>(createEntityListModel((ForeignKeyProperty) property, connectionProvider));
    }
    if (property instanceof ItemProperty) {
      return new ComboBox<>(createItemComboBoxModel((ItemProperty<T>) property));
    }

    final Attribute<?> attribute = property.getAttribute();
    if (attribute.isBoolean()) {
      return createCheckBox();
    }
    if (attribute.isLocalDate()) {
      return createDatePicker();
    }
    if (attribute.isTemporal() || attribute.isNumerical() || attribute.isString() || attribute.isCharacter()) {
      return createTextField(property);
    }

    throw new IllegalArgumentException("Unsupported property type: " + attribute.getTypeClass());
  }

  /**
   * Instantiates a {@link CheckBox} based on the given property, linked to the given edit model
   * @param property the property
   * @param editModel the edit model
   * @return a {@link CheckBox} based on the given property and edit model
   */
  public static CheckBox createCheckBox(final Property<Boolean> property, final FXEntityEditModel editModel) {
    return createCheckBox(property, editModel, null);
  }

  /**
   * Instantiates a {@link CheckBox} based on the given property, linked to the given edit model
   * @param property the property
   * @param editModel the edit model
   * @param enabledState the {@link State} instance controlling the enabled state of the check-box
   * @return a {@link CheckBox} based on the given property and edit model
   */
  public static CheckBox createCheckBox(final Property<Boolean> property, final FXEntityEditModel editModel,
                                        final StateObserver enabledState) {
    final CheckBox checkBox = createCheckBox(enabledState);
    createBooleanValue(checkBox).link(editModel.value(property.getAttribute()));

    return checkBox;
  }

  /**
   * Instantiates a {@link Value} instance based on the value of the given {@link CheckBox}
   * @param checkBox the check-box on which to base the value
   * @return a {@link Value} based on the given check-box
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
  public static TextField createTextField(final Property<String> property, final FXEntityEditModel editModel) {
    return createTextField(property, editModel, null);
  }

  /**
   * Instantiates a {@link TextField} based on the given property, linked to the given edit model
   * @param property the property
   * @param editModel the edit model
   * @param enabledState the {@link State} instance controlling the enabled state of the text field
   * @return a {@link TextField} based on the given property
   */
  public static TextField createTextField(final Property<String> property, final FXEntityEditModel editModel,
                                          final StateObserver enabledState) {
    final TextField textField = createTextField(property, enabledState);
    createStringValue(textField).link(editModel.value(property.getAttribute()));

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
  public static TextField createLongField(final Property<Long> property, final FXEntityEditModel editModel) {
    return createLongField(property, editModel, null);
  }

  /**
   * Instantiates a {@link TextField} for {@link Long} values, based on the given property and linked to the given edit model
   * @param property the property
   * @param editModel the edit model
   * @param enabledState the {@link State} instance controlling the enabled state of the text field
   * @return a {@link TextField} for {@link Long} values, based on the given property
   */
  public static TextField createLongField(final Property<Long> property, final FXEntityEditModel editModel,
                                          final StateObserver enabledState) {
    final TextField textField = createTextField(property, enabledState);
    createLongValue(property, textField).link(editModel.value(property.getAttribute()));

    return textField;
  }

  /**
   * Instantiates a {@link StringValue} for {@link Long} values, based on the given property and linked to the given text field
   * @param property the property
   * @param textField the text field
   * @return a {@link StringValue} for {@link Long} values, based on the given property
   */
  public static StringValue<Long> createLongValue(final Property<Long> property, final TextField textField) {
    final StringValue<Long> propertyValue = PropertyValues.longPropertyValue(textField.textProperty(),
            (NumberFormat) property.getFormat());
    textField.textFormatterProperty().setValue(new TextFormatter<>(propertyValue.getConverter()));

    return propertyValue;
  }

  /**
   * Instantiates a {@link TextField} for {@link Integer} values, based on the given property and linked to the given edit model
   * @param property the property
   * @param editModel the edit model
   * @return a {@link TextField} for {@link Integer} values, based on the given property
   */
  public static TextField createIntegerField(final Property<Integer> property, final FXEntityEditModel editModel) {
    return createIntegerField(property, editModel, null);
  }

  /**
   * Instantiates a {@link TextField} for {@link Integer} values, based on the given property and linked to the given edit model
   * @param property the property
   * @param editModel the edit model
   * @param enabledState the {@link State} instance controlling the enabled state of the text field
   * @return a {@link TextField} for {@link Integer} values, based on the given property
   */
  public static TextField createIntegerField(final Property<Integer> property, final FXEntityEditModel editModel,
                                             final StateObserver enabledState) {
    final TextField textField = createTextField(property, enabledState);
    createIntegerValue(property, textField).link(editModel.value(property.getAttribute()));

    return textField;
  }

  /**
   * Instantiates a {@link StringValue} for {@link Integer} values, based on the given property and linked to the given text field
   * @param property the property
   * @param textField the text field
   * @return a {@link StringValue} for {@link Integer} values, based on the given property
   */
  public static StringValue<Integer> createIntegerValue(final Property<Integer> property, final TextField textField) {
    final StringValue<Integer> propertyValue = PropertyValues.integerPropertyValue(textField.textProperty(),
            (NumberFormat) property.getFormat());
    textField.textFormatterProperty().setValue(new TextFormatter<>(propertyValue.getConverter()));

    return propertyValue;
  }

  /**
   * Instantiates a {@link TextField} for {@link Double} values, based on the given property and linked to the given edit model
   * @param property the property
   * @param editModel the edit model
   * @return a {@link TextField} for {@link Double} values, based on the given property
   */
  public static TextField createDoubleField(final Property<Double> property, final FXEntityEditModel editModel) {
    return createDoubleField(property, editModel, null);
  }

  /**
   * Instantiates a {@link TextField} for {@link Double} values, based on the given property and linked to the given edit model
   * @param property the property
   * @param editModel the edit model
   * @param enabledState the {@link State} instance controlling the enabled state of the text field
   * @return a {@link TextField} for {@link Double} values, based on the given property
   */
  public static TextField createDoubleField(final Property<Double> property, final FXEntityEditModel editModel,
                                            final StateObserver enabledState) {
    final TextField textField = createTextField(property, enabledState);
    createDoubleValue(property, textField).link(editModel.value(property.getAttribute()));

    return textField;
  }

  /**
   * Instantiates a {@link TextField} for {@link BigDecimal} values, based on the given property and linked to the given edit model
   * @param property the property
   * @param editModel the edit model
   * @return a {@link TextField} for {@link BigDecimal} values, based on the given property
   */
  public static TextField createBigDecimalField(final Property<BigDecimal> property, final FXEntityEditModel editModel) {
    return createBigDecimalField(property, editModel, null);
  }

  /**
   * Instantiates a {@link TextField} for {@link BigDecimal} values, based on the given property and linked to the given edit model
   * @param property the property
   * @param editModel the edit model
   * @param enabledState the {@link State} instance controlling the enabled state of the text field
   * @return a {@link TextField} for {@link BigDecimal} values, based on the given property
   */
  public static TextField createBigDecimalField(final Property<BigDecimal> property, final FXEntityEditModel editModel,
                                                final StateObserver enabledState) {
    final TextField textField = createTextField(property, enabledState);
    createBigDecimalValue(property, textField).link(editModel.value(property.getAttribute()));

    return textField;
  }

  /**
   * Instantiates a {@link StringValue} for {@link Double} values, based on the given property and linked to the given text field
   * @param property the property
   * @param textField the text field
   * @return a {@link StringValue} for {@link Double} values, based on the given property
   */
  public static StringValue<Double> createDoubleValue(final Property<Double> property, final TextField textField) {
    final StringValue<Double> propertyValue = PropertyValues.doublePropertyValue(textField.textProperty(),
            (NumberFormat) property.getFormat());
    textField.textFormatterProperty().setValue(new TextFormatter<>(propertyValue.getConverter()));

    return propertyValue;
  }

  /**
   * Instantiates a {@link StringValue} for {@link java.math.BigDecimal} values, based on the given property and linked to the given text field
   * @param property the property
   * @param textField the text field
   * @return a {@link StringValue} for {@link java.math.BigDecimal} values, based on the given property
   */
  public static StringValue<BigDecimal> createBigDecimalValue(final Property<BigDecimal> property, final TextField textField) {
    final StringValue<BigDecimal> propertyValue = PropertyValues.bigDecimalPropertyValue(textField.textProperty(),
            (DecimalFormat) property.getFormat());
    textField.textFormatterProperty().setValue(new TextFormatter<>(propertyValue.getConverter()));

    return propertyValue;
  }

  /**
   * Instantiates a {@link DatePicker} based on the given property and linked to the given edit model
   * @param property the property
   * @param editModel the edit model
   * @return a {@link DatePicker} based on the given property
   */
  public static DatePicker createDatePicker(final Property<LocalDate> property, final FXEntityEditModel editModel) {
    return createDatePicker(property, editModel, null);
  }

  /**
   * Instantiates a {@link DatePicker} based on the given property and linked to the given edit model
   * @param property the property
   * @param editModel the edit model
   * @param enabledState the {@link State} instance controlling the enabled state of the date picker
   * @return a {@link DatePicker} based on the given property
   */
  public static DatePicker createDatePicker(final Property<LocalDate> property, final FXEntityEditModel editModel,
                                            final StateObserver enabledState) {
    final DatePicker picker = createDatePicker(enabledState);
    createDateValue(property, picker).link(editModel.value(property.getAttribute()));

    return picker;
  }

  /**
   * Instantiates a {@link StringValue} for {@link LocalDate} values, based on the given property and linked to the given text field
   * @param property the property
   * @param picker the date picker
   * @return a {@link StringValue} for {@link LocalDate} values, based on the given property
   */
  public static StringValue<LocalDate> createDateValue(final Property<LocalDate> property, final DatePicker picker) {
    final StringValue<LocalDate> dateValue = PropertyValues.datePropertyValue(picker.getEditor().textProperty(), property.getDateTimeFormatter());
    picker.setConverter(dateValue.getConverter());
    picker.setPromptText(property.getDateTimePattern().toLowerCase());

    return dateValue;
  }

  /**
   * Instantiates a {@link StringValue} for {@link LocalDateTime} values, based on the given property and linked to the given text field
   * @param property the property
   * @param textField the text field
   * @return a {@link StringValue} for {@link LocalDateTime} values, based on the given property
   */
  public static StringValue<LocalDateTime> createTimestampValue(final Property<LocalDateTime> property, final TextField textField) {
    final StringValue<LocalDateTime> timestampValue = PropertyValues.timestampPropertyValue(textField.textProperty(), property.getDateTimeFormatter());
    textField.setTextFormatter(new TextFormatter<>(timestampValue.getConverter()));

    return timestampValue;
  }

  /**
   * Instantiates a {@link StringValue} for {@link LocalTime} values, based on the given property and linked to the given text field
   * @param property the property
   * @param textField the text field
   * @return a {@link StringValue} for {@link LocalTime} values, based on the given property
   */
  public static StringValue<LocalTime> createTimeValue(final Property<LocalTime> property, final TextField textField) {
    final StringValue<LocalTime> timeValue = PropertyValues.timePropertyValue(textField.textProperty(), property.getDateTimeFormatter());
    textField.setTextFormatter(new TextFormatter<>(timeValue.getConverter()));

    return timeValue;
  }

  /**
   * Links the given boolean property to the given state observer, so that changes is one are reflected in the other
   * @param property the boolean property
   * @param stateObserver the state observer
   */
  public static void link(final BooleanProperty property, final StateObserver stateObserver) {
    requireNonNull(property);
    requireNonNull(stateObserver);
    property.setValue(stateObserver.get());
    stateObserver.addDataListener(property::setValue);
  }

  /**
   * Instantiates a {@link EntitySearchField} based on the given foreign key and linked to the given edit model
   * @param foreignKey the foreign key
   * @param editModel the edit model
   * @return a {@link EntitySearchField} based on the given property
   */
  public static EntitySearchField createSearchField(final ForeignKey foreignKey, final FXEntityEditModel editModel) {
    final EntitySearchModel searchModel = requireNonNull(editModel).getForeignKeySearchModel(requireNonNull(foreignKey));
    final EntitySearchField searchField = new EntitySearchField(searchModel);
    PropertyValues.singleSearchValue(searchModel).link(editModel.value(foreignKey));

    return searchField;
  }

  /**
   * Instantiates a {@link ComboBox} based on the given foreign key and linked to the given edit model
   * @param foreignKey the foreign key
   * @param editModel the edit model
   * @return a {@link ComboBox} based on the given property
   */
  public static ComboBox<Entity> createForeignKeyComboBox(final ForeignKey foreignKey, final FXEntityEditModel editModel) {
    final FXEntityListModel listModel = requireNonNull(editModel).getForeignKeyListModel(requireNonNull(foreignKey));
    listModel.refresh();
    final ComboBox<Entity> box = new ComboBox<>(listModel.getSortedList());
    listModel.setSelectionModel(box.getSelectionModel());
    PropertyValues.selectedValue(box.getSelectionModel()).link(editModel.value(foreignKey));

    return box;
  }

  /**
   * Instantiates a {@link ComboBox} based on the values of the given property and linked to the given edit model
   * @param itemProperty the property
   * @param editModel the edit model
   * @param <T> the property type
   * @return a {@link ComboBox} based on the values of the given property
   */
  public static <T> ComboBox<Item<T>> createItemComboBox(final ItemProperty<T> itemProperty, final FXEntityEditModel editModel) {
    final ComboBox<Item<T>> comboBox = new ComboBox<>(createItemComboBoxModel(itemProperty));
    PropertyValues.selectedItemValue(comboBox.getSelectionModel()).link(editModel.value(itemProperty.getAttribute()));

    return comboBox;
  }

  /**
   * Instantiates a new {@link CheckBox} instance
   * @return the check-box
   */
  public static CheckBox createCheckBox() {
    return createCheckBox(null);
  }

  /**
   * Instantiates a new {@link CheckBox} instance
   * @param enabledState the {@link State} instance controlling the enabled state of the check-box
   * @return the check-box
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
   * @param <T> the value type
   * @return a {@link ObservableList} containing the {@link Item}s associated with the given value list property
   */
  public static <T> ObservableList<Item<T>> createItemComboBoxModel(final ItemProperty<T> property) {
    return new SortedList<>(FXCollections.observableArrayList(property.getItems()),
            Comparator.comparing(Item::toString));
  }

  /**
   * Instantiates a {@link TextField} based on the given property
   * @param property the property
   * @param <T> the value type
   * @return a {@link TextField} based on the given property
   */
  public static <T> TextField createTextField(final Property<T> property) {
    return createTextField(property, null);
  }

  /**
   * Instantiates a {@link TextField} based on the given property
   * @param property the property
   * @param enabledState the {@link State} instance controlling the enabled state of the text field
   * @param <T> the value type
   * @return a {@link TextField} based on the given property
   */
  public static <T> TextField createTextField(final Property<T> property, final StateObserver enabledState) {
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
        return User.user(username.getText(), password.getText().toCharArray());
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
    alert.setTitle(Messages.get(Messages.ERROR));
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
    requireNonNull(node);
    requireNonNull(clazz);
    Parent parent = node.getParent();
    while (parent != null && !parent.getClass().equals(clazz)) {
      parent = parent.getParent();
    }

    return (T) parent;
  }

  private static SortedList<Entity> createEntityListModel(final ForeignKeyProperty property,
                                                          final EntityConnectionProvider connectionProvider) {
    final ObservableEntityList entityList = new ObservableEntityList(property.getReferencedEntityType(), connectionProvider);
    entityList.refresh();

    return entityList.getSortedList();
  }

  private static final class ValidationChangeListener implements ChangeListener<String> {

    private final Property<?> property;
    private final StringProperty stringProperty;
    private final State ignoreChange = State.state();

    private ValidationChangeListener(final Property<?> property, final StringProperty stringProperty) {
      this.property = property;
      this.stringProperty = stringProperty;
    }

    @Override
    public void changed(final ObservableValue<? extends String> observable, final String oldValue, final String newValue) {
      if (ignoreChange.get()) {
        return;
      }
      if (!isValid(property, newValue)) {
        Platform.runLater(() -> {
          try {
            ignoreChange.set(true);
            stringProperty.setValue(oldValue);
          }
          finally {
            ignoreChange.set(false);
          }
        });
      }
    }

    private static boolean isValid(final Property<?> property, final String value) {
      final int maximumLength = property.getMaximumLength();
      if (maximumLength > -1 && value != null && value.length() > maximumLength) {
        return false;
      }
      if (property.getAttribute().isTemporal()) {
        try {
          if (value != null) {
            property.getDateTimeFormatter().parse(value);
          }

          return true;
        }
        catch (final DateTimeParseException e) {
          return false;
        }
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
      if (parsedValue != null && property.getAttribute().isNumerical() && !isWithinRange(property, (Number) parsedValue)) {
        return false;
      }
      if (parsedValue instanceof Double && !Objects.equals(parsedValue,
              Util.roundDouble((Double) parsedValue, property.getMaximumFractionDigits()))) {
        return false;
      }

      return true;
    }

    private static boolean isWithinRange(final Property<?> property, final Number value) {
      final double min = property.getMinimumValue() != null ? Math.min(property.getMinimumValue(), 0) : Double.NEGATIVE_INFINITY;
      final double max = property.getMaximumValue() == null ? Double.POSITIVE_INFINITY : property.getMaximumValue();
      final double doubleValue = value.doubleValue();

      return doubleValue >= min && doubleValue <= max;
    }
  }
}
