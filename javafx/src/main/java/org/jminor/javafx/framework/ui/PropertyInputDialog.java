/*
 * Copyright (c) 2004 - 2016, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.javafx.framework.ui;

import org.jminor.common.model.Item;
import org.jminor.common.model.Value;
import org.jminor.common.model.Values;
import org.jminor.framework.db.EntityConnectionProvider;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.Property;
import org.jminor.javafx.framework.ui.values.PropertyValues;
import org.jminor.javafx.framework.ui.values.StringValue;

import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Control;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.TextField;

import java.sql.Date;
import java.sql.Types;
import java.time.LocalDate;

public final class PropertyInputDialog extends Dialog<PropertyInputDialog.InputResult> {

  private final Control control;

  public PropertyInputDialog(final Property property, final Object defaultValue,
                             final EntityConnectionProvider connectionProvider) {
    setTitle(property.getCaption());
    this.control = FXUiUtil.createControl(property, connectionProvider);
    final Value value = createValue(property, control, defaultValue);
    if (control instanceof TextField) {
      ((TextField) control).selectAll();
    }
    initializeUI(property, control);
    setResultConverter((dialogButton) -> new InputResult(dialogButton != null &&
            dialogButton.getButtonData() == ButtonBar.ButtonData.OK_DONE, value.get()));
  }

  public Control getControl() {
    return control;
  }

  private void initializeUI(final Property property, final Control control) {
    getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
    getDialogPane().setContent(control);
  }

  private Value createValue(final Property property, final Control control, final Object defaultValue) {
    if (property instanceof Property.ForeignKeyProperty) {
      final Value<Entity> entityValue = PropertyValues.selectedValue(((ComboBox<Entity>) control).getSelectionModel());
      entityValue.set((Entity) defaultValue);
      return entityValue;
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
        final Value<java.util.Date> dateValue = Values.value((Date) defaultValue);
        final StringValue<LocalDate> value = FXUiUtil.createDateValue(property, (DatePicker) control);
        Values.link(FXUiUtil.createLocalDateValue(dateValue), value);
        return dateValue;
      case Types.DOUBLE:
        final StringValue<Double> doubleValue = FXUiUtil.createDoubleValue(property, (TextField) control);
        doubleValue.set((Double) defaultValue);
        return doubleValue;
      case Types.INTEGER:
        final StringValue<Integer> integerValue = FXUiUtil.createIntegerValue(property, (TextField) control);
        integerValue.set((Integer) defaultValue);
        return integerValue;
      case Types.BIGINT:
        final StringValue<Long> longValue = FXUiUtil.createLongValue(property, (TextField) control);
        longValue.set((Long) defaultValue);
        return longValue;
      case Types.CHAR:
      case Types.VARCHAR:
        final StringValue<String> stringValue = FXUiUtil.createStringValue((TextField) control);
        stringValue.set((String) defaultValue);
        return stringValue;
      default:
        throw new IllegalArgumentException("Unsupported property type: " + property.getType());
    }
  }

  public static final class InputResult {
    private final boolean inputAccepted;
    private final Object value;

    public InputResult(final boolean inputAccepted, final Object value) {
      this.inputAccepted = inputAccepted;
      this.value = value;
    }

    public boolean isInputAccepted() {
      return inputAccepted;
    }

    public Object getValue() {
      return value;
    }
  }
}
