/*
 * Copyright (c) 2004 - 2016, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.javafx.framework.ui;

import org.jminor.common.EventObserver;
import org.jminor.common.State;
import org.jminor.common.StateObserver;
import org.jminor.common.States;
import org.jminor.common.Value;
import org.jminor.common.Values;
import org.jminor.common.i18n.Messages;
import org.jminor.common.model.CancelException;
import org.jminor.common.model.Item;
import org.jminor.common.model.User;
import org.jminor.common.model.Util;
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
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class FXUiUtil {

  private FXUiUtil() {}

  public static <T> T selectValue(final List<T> values) {
    return selectValues(values, true).get(0);
  }

  public static <T> List<T> selectValues(final List<T> values) {
    return selectValues(values, false);
  }

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

  public static void setClipboard(final String string) {
    final ClipboardContent content = new ClipboardContent();
    content.putString(string);
    Clipboard.getSystemClipboard().setContent(content);
  }

  public static boolean confirm(final String message) {
    return confirm(null, null, message);
  }

  public static boolean confirm(final String headerText, final String message) {
    return confirm(null, headerText, message);
  }

  public static boolean confirm(final String title, final String headerText, final String message) {
    Objects.requireNonNull(message);
    final Alert alert = new Alert(Alert.AlertType.CONFIRMATION, message, ButtonType.OK, ButtonType.CANCEL);
    if (title != null) {
      alert.setTitle(title);
    }
    if (headerText != null) {
      alert.setHeaderText(headerText);
    }
    return alert.showAndWait().get() == ButtonType.OK;
  }

  public static Value createValue(final Property property, final Control control, final Object defaultValue) {
    if (property instanceof Property.ForeignKeyProperty) {
      if (control instanceof ComboBox) {
        final Value<Entity> entityValue = PropertyValues.selectedValue(((ComboBox<Entity>) control).getSelectionModel());
        entityValue.set((Entity) defaultValue);
        return entityValue;
      }
      else if (control instanceof EntityLookupField) {
        final Value<Collection<Entity>> entityValue = PropertyValues.lookupValue (((EntityLookupField) control).getModel());
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

  public static Value<Entity> createEntityValue(final ComboBox<Entity> comboBox) {
    return PropertyValues.selectedValue(comboBox.getSelectionModel());
  }

  public static ToggleButton createToggleButton(final State state) {
    final ToggleButton button = new ToggleButton();
    final Value<Boolean> checkBoxValue = PropertyValues.booleanPropertyValue(button.selectedProperty());
    final Value<Boolean> stateValue = Values.stateValue(state);
    Values.link(stateValue, checkBoxValue);

    return button;
  }

  public static CheckBox createCheckBox(final State state) {
    final CheckBox box = new CheckBox();
    final Value<Boolean> checkBoxValue = PropertyValues.booleanPropertyValue(box.selectedProperty());
    final Value<Boolean> stateValue = Values.stateValue(state);
    Values.link(stateValue, checkBoxValue);

    return box;
  }

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

  public static CheckBox createCheckBox(final Property property, final FXEntityEditModel editModel) {
    return createCheckBox(property, editModel, null);
  }

  public static CheckBox createCheckBox(final Property property, final FXEntityEditModel editModel,
                                        final StateObserver enabledState) {
    final CheckBox checkBox = createCheckBox(enabledState);
    final Value<Boolean> propertyValue = createBooleanValue(checkBox);
    Values.link(EditModelValues.value(editModel, property.getPropertyID()), propertyValue);

    return checkBox;
  }

  public static Value<Boolean> createBooleanValue(final CheckBox checkBox) {
    return PropertyValues.booleanPropertyValue(checkBox.selectedProperty());
  }

  public static TextField createTextField(final Property property, final FXEntityEditModel editModel) {
    return createTextField(property, editModel, null);
  }

  public static TextField createTextField(final Property property, final FXEntityEditModel editModel,
                                          final StateObserver enabledState) {
    final TextField textField = createTextField(property, enabledState);
    final StringValue<String> propertyValue = createStringValue(textField);
    Values.link(EditModelValues.value(editModel, property.getPropertyID()), propertyValue);

    return textField;
  }

  public static StringValue<String> createStringValue(final TextField textField) {
    return PropertyValues.stringPropertyValue(textField.textProperty());
  }

  public static TextField createLongField(final Property property, final FXEntityEditModel editModel) {
    return createLongField(property, editModel, null);
  }

  public static TextField createLongField(final Property property, final FXEntityEditModel editModel,
                                          final StateObserver enabledState) {
    final TextField textField = createTextField(property, enabledState);
    final StringValue<Long> propertyValue = createLongValue(property, textField);
    Values.link(EditModelValues.value(editModel, property.getPropertyID()), propertyValue);

    return textField;
  }

  public static StringValue<Long> createLongValue(final Property property, final TextField textField) {
    final StringValue<Long> propertyValue = PropertyValues.longPropertyValue(textField.textProperty(),
            (NumberFormat) property.getFormat());
    textField.textFormatterProperty().setValue(new TextFormatter(propertyValue.getConverter()));

    return propertyValue;
  }

  public static TextField createIntegerField(final Property property, final FXEntityEditModel editModel) {
    return createIntegerField(property, editModel, null);
  }

  public static TextField createIntegerField(final Property property, final FXEntityEditModel editModel,
                                             final StateObserver enabledState) {
    final TextField textField = createTextField(property, enabledState);
    final StringValue<Integer> propertyValue = createIntegerValue(property, textField);
    Values.link(EditModelValues.value(editModel, property.getPropertyID()), propertyValue);

    return textField;
  }

  public static StringValue<Integer> createIntegerValue(final Property property, final TextField textField) {
    final StringValue<Integer> propertyValue = PropertyValues.integerPropertyValue(textField.textProperty(),
            (NumberFormat) property.getFormat());
    textField.textFormatterProperty().setValue(new TextFormatter(propertyValue.getConverter()));

    return propertyValue;
  }

  public static TextField createDoubleField(final Property property, final FXEntityEditModel editModel) {
    return createDoubleField(property, editModel, null);
  }

  public static TextField createDoubleField(final Property property, final FXEntityEditModel editModel,
                                            final StateObserver enabledState) {
    final TextField textField = createTextField(property, enabledState);
    final StringValue<Double> propertyValue = createDoubleValue(property, textField);
    Values.link(EditModelValues.value(editModel, property.getPropertyID()), propertyValue);

    return textField;
  }

  public static StringValue<Double> createDoubleValue(final Property property, final TextField textField) {
    final StringValue<Double> propertyValue = PropertyValues.doublePropertyValue(textField.textProperty(),
            (NumberFormat) property.getFormat());
    textField.textFormatterProperty().setValue(new TextFormatter(propertyValue.getConverter()));

    return propertyValue;
  }

  public static DatePicker createDatePicker(final Property property, final FXEntityEditModel editModel) {
    return createDatePicker(property, editModel, null);
  }

  public static DatePicker createDatePicker(final Property property, final FXEntityEditModel editModel,
                                            final StateObserver enabledState) {
    final DatePicker picker = createDatePicker(enabledState);
    final StringValue<LocalDate> value = createDateValue(property, picker);

    Values.link(new LocalDateValue(EditModelValues.value(editModel, property.getPropertyID())), value);

    return picker;
  }

  public static StringValue<LocalDate> createDateValue(final Property property, final DatePicker picker) {
    final SimpleDateFormat dateFormat = (SimpleDateFormat) property.getFormat();
    final StringValue<LocalDate> dateValue = PropertyValues.datePropertyValue(picker.getEditor().textProperty(), dateFormat);

    picker.setConverter(dateValue.getConverter());
    picker.setPromptText(dateFormat.toPattern().toLowerCase());

    return dateValue;
  }

  public static Value<LocalDate> createLocalDateValue(final Value<Date> dateValue) {
    return new LocalDateValue(dateValue);
  }

  public static void link(final BooleanProperty property, final StateObserver stateObserver) {
    Objects.requireNonNull(property);
    Objects.requireNonNull(stateObserver);
    property.setValue(stateObserver.isActive());
    stateObserver.addInfoListener(property::setValue);
  }

  public static EntityLookupField createLookupField(final Property.ForeignKeyProperty foreignKeyProperty,
                                                    final FXEntityEditModel editModel) {
    final EntityLookupModel lookupModel = editModel.getForeignKeyLookupModel(foreignKeyProperty);
    final EntityLookupField lookupField = new EntityLookupField(lookupModel);
    Values.link(EditModelValues.value(editModel, foreignKeyProperty.getPropertyID()), PropertyValues.lookupValue(lookupModel));

    return lookupField;
  }

  public static ComboBox<Entity> createForeignKeyComboBox(final Property.ForeignKeyProperty property,
                                                          final FXEntityEditModel editModel) {
    final FXEntityListModel listModel = editModel.getForeignKeyListModel(property);
    listModel.refresh();
    final ComboBox<Entity> box = new ComboBox<>(listModel.getSortedList());
    listModel.setSelectionModel(box.getSelectionModel());
    Values.link(EditModelValues.value(editModel, property.getPropertyID()), PropertyValues.selectedValue(box.getSelectionModel()));

    return box;
  }

  public static ComboBox<Item> createValueListComboBox(final Property.ValueListProperty property,
                                                       final FXEntityEditModel editModel) {
    final ComboBox<Item> comboBox = new ComboBox<>(createValueListComboBoxModel(property));
    Values.link(EditModelValues.value(editModel, property.getPropertyID()), PropertyValues.selectedItemValue(comboBox.getSelectionModel()));
    return comboBox;
  }

  public static CheckBox createCheckBox() {
    return createCheckBox(null);
  }

  public static CheckBox createCheckBox(final StateObserver enabledState) {
    final CheckBox checkBox = new CheckBox();
    if (enabledState != null) {
      link(checkBox.disableProperty(), enabledState.getReversedObserver());
    }

    return checkBox;
  }

  public static ObservableList<Item> createValueListComboBoxModel(final Property.ValueListProperty property) {
    return new SortedList<>(FXCollections.observableArrayList(property.getValues()),
            (o1, o2) -> o1.toString().compareTo(o2.toString()));
  }

  public static TextField createTextField(final Property property) {
    return createTextField(property, (StateObserver) null);
  }

  public static TextField createTextField(final Property property, final StateObserver enabledState) {
    final TextField textField = new TextField();
    textField.textProperty().addListener(new ValidationChangeListener(property, textField.textProperty()));
    if (enabledState != null) {
      link(textField.disableProperty(), enabledState.getReversedObserver());
    }

    return textField;
  }

  public static DatePicker createDatePicker() {
    return createDatePicker(null);
  }

  public static DatePicker createDatePicker(final StateObserver enabledState) {
    final DatePicker picker = new DatePicker();
    if (enabledState != null) {
      link(picker.disableProperty(), enabledState.getReversedObserver());
    }

    return picker;
  }

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
    password.setText(defaultUser == null || defaultUser.getPassword() == null ? "" : defaultUser.getPassword());
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
        return new User(username.getText(), password.getText());
      }

      return null;
    });

    final Optional<User> result = dialog.showAndWait();
    if (result.isPresent()) {
      return result.get();
    }

    throw new CancelException();
  }

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
    dialogPane.expandedProperty().addListener(value -> {
      Platform.runLater(() -> {
        dialogPane.requestLayout();
        dialogPane.getScene().getWindow().sizeToScene();
      });
    });

    Platform.runLater(alert::showAndWait);
  }

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
    final ObservableEntityList entityList = new ObservableEntityList(property.getReferencedEntityID(),
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
    if (parsedValue instanceof Double && !Util.equal(parsedValue,
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
