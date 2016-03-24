/*
 * Copyright (c) 2004 - 2016, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.javafx.framework.ui;

import org.jminor.common.db.exception.DatabaseException;
import org.jminor.common.model.Item;
import org.jminor.common.model.Value;
import org.jminor.common.model.Values;
import org.jminor.framework.db.EntityConnectionProvider;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.Property;
import org.jminor.javafx.framework.model.EntityListModel;
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

public final class PropertyInputDialog extends Dialog<Object> {

  public PropertyInputDialog(final Property property, final Object defaultValue,
                             final EntityConnectionProvider connectionProvider) {
    setTitle(property.getCaption());
    final Control control = createControl(property, connectionProvider);
    final Value value = createValue(property, control, defaultValue);
    initializeUI(property, control);
    setResultConverter((dialogButton) -> {
      final ButtonBar.ButtonData data = dialogButton == null ? null : dialogButton.getButtonData();
      return data == ButtonBar.ButtonData.OK_DONE ? value.get() : null;
    });
  }

  private void initializeUI(final Property property, final Control control) {
    getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
    getDialogPane().setContent(control);
  }

  private Control createControl(final Property property, final EntityConnectionProvider connectionProvider) {
    if (property instanceof Property.ForeignKeyProperty) {
      return new ComboBox<>(createEntityComboBoxModel((Property.ForeignKeyProperty) property, connectionProvider));
    }
    if (property instanceof Property.ValueListProperty) {
      return new ComboBox<>(FXUiUtil.createValueListComboBoxModel((Property.ValueListProperty) property));
    }

    switch (property.getType()) {
      case Types.BOOLEAN:
        return FXUiUtil.createCheckBox(property);
      case Types.DATE:
      case Types.TIMESTAMP:
      case Types.TIME:
        return FXUiUtil.createDatePicker(property);
      case Types.DOUBLE:
      case Types.INTEGER:
      case Types.BIGINT:
      case Types.CHAR:
      case Types.VARCHAR:
        return FXUiUtil.createTextField(property);
      default:
        throw new IllegalArgumentException("Unsupported property type: " + property.getType());
    }
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

  private EntityListModel createEntityComboBoxModel(final Property.ForeignKeyProperty property, final EntityConnectionProvider connectionProvider) {
    final EntityListModel tableModel = new EntityListModel(property.getReferencedEntityID(), connectionProvider);
    tableModel.setSortAfterRefresh(true);
    try {
      tableModel.refresh();
    }
    catch (final DatabaseException e) {
      throw new RuntimeException(e);
    }

    return tableModel;
  }
}
