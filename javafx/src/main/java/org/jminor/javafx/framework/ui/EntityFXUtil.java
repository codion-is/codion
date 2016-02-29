/*
 * Copyright (c) 2004 - 2016, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.javafx.framework.ui;

import org.jminor.common.model.EventObserver;
import org.jminor.common.model.State;
import org.jminor.common.model.StateObserver;
import org.jminor.common.model.States;
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
import javafx.scene.Node;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;

import java.text.Format;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.Objects;

public final class EntityFXUtil {

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
        Platform.runLater(new Runnable() {
          @Override
          public void run() {
            try {
              ignoreChange.setActive(true);
              stringProperty.setValue(oldValue);
            }
            finally {
              ignoreChange.setActive(false);
            }
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
