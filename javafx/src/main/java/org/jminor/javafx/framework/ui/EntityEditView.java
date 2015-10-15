/*
 * Copyright (c) 2004 - 2015, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.javafx.framework.ui;

import org.jminor.common.db.exception.DatabaseException;
import org.jminor.common.model.Values;
import org.jminor.framework.domain.Entities;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.Property;
import org.jminor.javafx.framework.model.EntityEditModel;
import org.jminor.javafx.framework.model.ObservableEntityList;
import org.jminor.javafx.framework.ui.values.PropertyValues;
import org.jminor.javafx.framework.ui.values.StringValue;

import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.layout.BorderPane;

public abstract class EntityEditView extends BorderPane {

  private final EntityEditModel editModel;
  private boolean initialized = false;

  public EntityEditView(final EntityEditModel editModel) {
    this.editModel = editModel;
  }

  public final EntityEditView initializePanel() {
    if (!initialized) {
      initializeUI();
      initialized = true;
    }

    return this;
  }

  public EntityEditModel getModel() {
    return editModel;
  }

  protected abstract Node initializeEditPanel();

  protected final ComboBox<Entity> createComboBox(final String propertyID) {
    final Property.ForeignKeyProperty property = Entities.getForeignKeyProperty(getModel().getEntityID(), propertyID);

    final ComboBox<Entity> box = new ComboBox<>(editModel.createForeignKeyList(propertyID));
    Values.link(editModel.createValue(propertyID), PropertyValues.selectedItemValue(box.getSelectionModel()));
    try {
      ((ObservableEntityList) box.getItems()).refresh();
    }
    catch (final DatabaseException e) {
      throw new RuntimeException(e);
    }

    return box;
  }

  protected final TextField createTextField(final String propertyID) {
    final TextField textField = createTextField(Entities.getProperty(getModel().getEntityID(), propertyID));
    final StringValue<String> propertyValue = PropertyValues.stringPropertyValue(textField.textProperty());

    Values.link(editModel.createValue(propertyID), propertyValue);

    return textField;
  }

  protected final TextField createIntegerField(final String propertyID) {
    final TextField textField = createTextField(Entities.getProperty(getModel().getEntityID(), propertyID));
    final StringValue<Integer> propertyValue = PropertyValues.integerPropertyValue(textField.textProperty());
    textField.textFormatterProperty().setValue(new TextFormatter(propertyValue.getConverter()));

    Values.link(editModel.createValue(propertyID), propertyValue);

    return textField;
  }

  protected final TextField createDoubleField(final String propertyID) {
    final TextField textField = createTextField(Entities.getProperty(getModel().getEntityID(), propertyID));
    final StringValue<Double> propertyValue = PropertyValues.doublePropertyValue(textField.textProperty());
    textField.textFormatterProperty().setValue(new TextFormatter(propertyValue.getConverter()));

    Values.link(editModel.createValue(propertyID), propertyValue);

    return textField;
  }

  private TextField createTextField(final Property property) {
    final TextField textField = new TextField();

    return textField;
  }

  private void initializeUI() {
    setCenter(initializeEditPanel());
  }
}
