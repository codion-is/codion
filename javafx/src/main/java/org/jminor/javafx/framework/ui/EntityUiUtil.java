/*
 * Copyright (c) 2004 - 2016, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.javafx.framework.ui;

import org.jminor.common.i18n.Messages;
import org.jminor.common.model.CancelException;
import org.jminor.common.model.EventObserver;
import org.jminor.common.model.State;
import org.jminor.common.model.StateObserver;
import org.jminor.common.model.States;
import org.jminor.common.model.User;
import org.jminor.common.model.Util;
import org.jminor.common.model.Value;
import org.jminor.common.model.Values;
import org.jminor.framework.domain.Property;
import org.jminor.javafx.framework.model.EntityEditModel;
import org.jminor.javafx.framework.ui.values.PropertyValues;
import org.jminor.javafx.framework.ui.values.StringValue;

import javafx.application.Platform;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.Format;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.Objects;
import java.util.Optional;

public final class EntityUiUtil {

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

    alert.getDialogPane().setExpandableContent(expandableContent);

    alert.showAndWait();
  }

  public static TextField createTextField(final Property property, final EntityEditModel editModel) {
    return createTextField(property, editModel, null);
  }

  public static TextField createTextField(final Property property, final EntityEditModel editModel,
                                          final StateObserver enabledState) {
    final TextField textField = createTextField(property, enabledState);
    final StringValue<String> propertyValue = PropertyValues.stringPropertyValue(textField.textProperty());
    Values.link(editModel.createValue(property.getPropertyID()), propertyValue);

    return textField;
  }

  public static TextField createLongField(final Property property, final EntityEditModel editModel) {
    return createLongField(property, editModel, null);
  }

  public static TextField createLongField(final Property property, final EntityEditModel editModel,
                                          final StateObserver enabledState) {
    final TextField textField = createTextField(property, enabledState);
    final StringValue<Long> propertyValue = PropertyValues.longPropertyValue(textField.textProperty(),
            (NumberFormat) property.getFormat());

    textField.textFormatterProperty().setValue(new TextFormatter(propertyValue.getConverter()));
    Values.link(editModel.createValue(property.getPropertyID()), propertyValue);

    return textField;
  }

  public static TextField createIntegerField(final Property property, final EntityEditModel editModel) {
    return createIntegerField(property, editModel, null);
  }

  public static TextField createIntegerField(final Property property, final EntityEditModel editModel,
                                             final StateObserver enabledState) {
    final TextField textField = createTextField(property, enabledState);
    final StringValue<Integer> propertyValue = PropertyValues.integerPropertyValue(textField.textProperty(),
            (NumberFormat) property.getFormat());

    textField.textFormatterProperty().setValue(new TextFormatter(propertyValue.getConverter()));
    Values.link(editModel.createValue(property.getPropertyID()), propertyValue);

    return textField;
  }

  public static TextField createDoubleField(final Property property, final EntityEditModel editModel) {
    return createDoubleField(property, editModel, null);
  }

  public static TextField createDoubleField(final Property property, final EntityEditModel editModel,
                                            final StateObserver enabledState) {
    final TextField textField = createTextField(property, enabledState);
    final StringValue<Double> propertyValue = PropertyValues.doublePropertyValue(textField.textProperty(),
            (NumberFormat) property.getFormat());

    textField.textFormatterProperty().setValue(new TextFormatter(propertyValue.getConverter()));
    Values.link(editModel.createValue(property.getPropertyID()), propertyValue);

    return textField;
  }

  public static DatePicker createDatePicker(final Property property, final EntityEditModel editModel) {
    return createDatePicker(property, editModel, null);
  }

  public static DatePicker createDatePicker(final Property property, final EntityEditModel editModel,
                                            final StateObserver enabledState) {
    final DatePicker picker = createDatePicker(property, enabledState);
    final SimpleDateFormat dateFormat = (SimpleDateFormat) property.getFormat();
    final StringValue<LocalDate> dateValue = PropertyValues.datePropertyValue(picker.getEditor().textProperty(), dateFormat);

    picker.setConverter(dateValue.getConverter());
    picker.setPromptText(dateFormat.toPattern().toLowerCase());

    Values.link(new LocalDateValue(editModel.createValue(property.getPropertyID())), dateValue);

    return picker;
  }

  public static void linkToEnabledState(final Node node, final StateObserver enabledState) {
    Objects.requireNonNull(node);
    Objects.requireNonNull(enabledState);
    node.setDisable(!enabledState.isActive());
    enabledState.addInfoListener(active -> node.setDisable(!active));
  }

  private static TextField createTextField(final Property property, final StateObserver enabledState) {
    final TextField textField = new TextField();
    textField.textProperty().addListener(new ValidationChangeListener(property, textField.textProperty()));
    if (enabledState != null) {
      linkToEnabledState(textField, enabledState);
    }

    return textField;
  }

  private static DatePicker createDatePicker(final Property property, final StateObserver enabledState) {
    final DatePicker picker = new DatePicker();
    if (enabledState != null) {
      linkToEnabledState(picker, enabledState);
    }

    return picker;
  }

  public static User showLoginDialog(final String applicationTitle, final String defaultUserName, final ImageView icon) {
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

    final TextField username = new TextField(defaultUserName == null ? "" : defaultUserName);
    username.setPromptText(Messages.get(Messages.USERNAME));
    final PasswordField password = new PasswordField();
    password.setPromptText(Messages.get(Messages.PASSWORD));

    grid.add(new Label(Messages.get(Messages.USERNAME)), 0, 0);
    grid.add(username, 1, 0);
    grid.add(new Label(Messages.get(Messages.PASSWORD)), 0, 1);
    grid.add(password, 1, 1);

    final Node loginButton = dialog.getDialogPane().lookupButton(loginButtonType);
    loginButton.setDisable(true);

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

  private static final class LocalDateValue implements Value<LocalDate> {

    private final Value modelValue;

    private LocalDateValue(final Value<Date> modelValue) {
      this.modelValue = modelValue;
    }

    @Override
    public void set(final LocalDate value) {
      if (value == null) {
        modelValue.set(null);
      }
      else {
        modelValue.set(Date.from(value.atStartOfDay(ZoneId.systemDefault()).toInstant()));
      }
    }

    @Override
    public LocalDate get() {
      return toLocalDate((Date) modelValue.get());
    }

    @Override
    public EventObserver<LocalDate> getObserver() {
      return modelValue.getObserver();
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
