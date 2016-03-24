/*
 * Copyright (c) 2004 - 2016, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.javafx.framework.ui;

import org.jminor.common.db.exception.DatabaseException;
import org.jminor.common.model.Item;
import org.jminor.common.model.Value;
import org.jminor.framework.db.EntityConnectionProvider;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.Property;
import org.jminor.javafx.framework.model.EntityListModel;
import org.jminor.javafx.framework.ui.values.PropertyValues;
import org.jminor.javafx.framework.ui.values.StringValue;

import com.sun.javafx.collections.ImmutableObservableList;
import javafx.collections.ObservableList;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Control;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;

import java.sql.Types;
import java.time.LocalDate;

public final class PropertyInputDialog extends Dialog<Object> {

  public PropertyInputDialog(final Property property, final Object defaultValue,
                             final EntityConnectionProvider connectionProvider) {
    final Control control = createControl(property, connectionProvider);
    final Value value = createValue(property, control, defaultValue);
    initializeUI(property, control);
    setResultConverter((dialogButton) -> {
      final ButtonBar.ButtonData data = dialogButton == null ? null : dialogButton.getButtonData();
      return data == ButtonBar.ButtonData.OK_DONE ? value.get() : null;
    });
  }

  private void initializeUI(final Property property, final Control control) {
    final Label label = new Label(property.getCaption());

    final GridPane grid = new GridPane();
    grid.add(label, 0, 0);
    grid.add(control, 1, 0);

    getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

    getDialogPane().setContent(grid);
  }

  private Control createControl(final Property property, final EntityConnectionProvider connectionProvider) {
    if (property instanceof Property.ForeignKeyProperty) {
      return new ComboBox<>(createComboBoxModel((Property.ForeignKeyProperty) property, connectionProvider));
    }
    if (property instanceof Property.ValueListProperty) {
      return new ComboBox<>(createComboBoxModel((Property.ValueListProperty) property));
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
      final Value<Entity> entityValue = PropertyValues.selectedItemValue(((ComboBox<Entity>) control).getSelectionModel());
      entityValue.set((Entity) defaultValue);
      return entityValue;
    }
    if (property instanceof Property.ValueListProperty) {}

    switch (property.getType()) {
      case Types.BOOLEAN:
        final Value<Boolean> booleanValue = PropertyValues.booleanPropertyValue(((CheckBox) control).selectedProperty());
        booleanValue.set((Boolean) defaultValue);
        return booleanValue;
      case Types.DATE:
      case Types.TIMESTAMP:
      case Types.TIME:
        final StringValue<LocalDate> dateValue = FXUiUtil.createDateValue(property, (DatePicker) control);
        dateValue.set((LocalDate) defaultValue);
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

  private EntityListModel createComboBoxModel(final Property.ForeignKeyProperty property, final EntityConnectionProvider connectionProvider) {
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

  private ObservableList<Item> createComboBoxModel(final Property.ValueListProperty property) {
    final ObservableList<Item> model = new ImmutableObservableList<>(property.getValues().toArray(new Item[property.getValues().size()]));
    model.sort((o1, o2) -> o1.toString().compareTo(o2.toString()));

    return model;
  }
}
