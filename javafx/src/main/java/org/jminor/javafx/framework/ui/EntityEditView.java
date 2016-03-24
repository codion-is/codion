/*
 * Copyright (c) 2004 - 2015, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.javafx.framework.ui;

import org.jminor.common.db.exception.DatabaseException;
import org.jminor.common.model.Conjunction;
import org.jminor.common.model.State;
import org.jminor.common.model.States;
import org.jminor.common.model.Values;
import org.jminor.framework.domain.Entities;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.Property;
import org.jminor.framework.i18n.FrameworkMessages;
import org.jminor.javafx.framework.model.EntityEditModel;
import org.jminor.javafx.framework.model.EntityTableModel;
import org.jminor.javafx.framework.ui.values.PropertyValues;

import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Control;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;

import java.sql.Types;
import java.util.HashMap;
import java.util.Map;

public abstract class EntityEditView extends BorderPane {

  private final EntityEditModel editModel;
  private final Map<String, Control> controls = new HashMap<>();
  private boolean initialized = false;

  private String initialFocusPropertyID;

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

  public final EntityEditModel getModel() {
    return editModel;
  }

  public final Node getButtonPanel() {
    final GridPane buttonPane = new GridPane();
    buttonPane.addRow(0, createInsertButton());
    buttonPane.addRow(1, createUpdateButton());
    buttonPane.addRow(2, createDeleteButton());
    buttonPane.addRow(3, createClearButton());

    return buttonPane;
  }

  protected final void setInitialFocusProperty(final String initialFocusPropertyID) {
    this.initialFocusPropertyID = initialFocusPropertyID;
  }

  protected abstract Node initializeEditPanel();

  protected final ComboBox<Entity> createComboBox(final String propertyID) {
    checkControl(propertyID);
    final ComboBox<Entity> box = new ComboBox<>(editModel.createForeignKeyList(propertyID));
    Values.link(editModel.createValue(propertyID), PropertyValues.selectedItemValue(box.getSelectionModel()));
    try {
      ((EntityTableModel) box.getItems()).refresh();
    }
    catch (final DatabaseException e) {
      throw new RuntimeException(e);
    }

    controls.put(propertyID, box);

    return box;
  }

  protected final TextField createTextField(final String propertyID) {
    checkControl(propertyID);
    final Property property = Entities.getProperty(editModel.getEntityID(), propertyID);
    final TextField textField;
    switch (property.getType()) {
      case Types.INTEGER:
        textField = EntityUiUtil.createIntegerField(Entities.getProperty(getModel().getEntityID(), propertyID), editModel);
        break;
      case Types.BIGINT:
        textField = EntityUiUtil.createLongField(Entities.getProperty(getModel().getEntityID(), propertyID), editModel);
        break;
      case Types.DOUBLE:
        textField = EntityUiUtil.createDoubleField(Entities.getProperty(getModel().getEntityID(), propertyID), editModel);
        break;
      case Types.VARCHAR:
        textField = EntityUiUtil.createTextField(Entities.getProperty(getModel().getEntityID(), propertyID), editModel);
        break;
      default:
        throw new IllegalArgumentException("Text field type for property: " + propertyID + " is not defined");
    }

    controls.put(propertyID, textField);

    return textField;
  }

  protected final DatePicker createDatePicker(final String propertyID) {
    return EntityUiUtil.createDatePicker(Entities.getProperty(getModel().getEntityID(), propertyID), editModel);
  }

  private void initializeUI() {
    setCenter(initializeEditPanel());
  }

  private Button createInsertButton() {
    final Button button = new Button(FrameworkMessages.get(FrameworkMessages.INSERT));
    button.setOnAction(event -> insert());

    return button;
  }

  private Button createUpdateButton() {
    final Button button = new Button(FrameworkMessages.get(FrameworkMessages.UPDATE));
    button.setOnAction(event -> update());
    final State existingAndModifiedState = States.aggregateState(Conjunction.AND,
            getModel().getEntityNewObserver().getReversedObserver(),
            getModel().getModifiedObserver());
    EntityUiUtil.linkToEnabledState(button, existingAndModifiedState.getObserver());

    return button;
  }

  private Button createDeleteButton() {
    final Button button = new Button(FrameworkMessages.get(FrameworkMessages.DELETE));
    button.setOnAction(event -> delete());
    EntityUiUtil.linkToEnabledState(button, getModel().getEntityNewObserver().getReversedObserver());

    return button;
  }

  private Button createClearButton() {
    final Button button = new Button(FrameworkMessages.get(FrameworkMessages.CLEAR));
    button.setOnAction(event -> editModel.clear());

    return button;
  }

  private void insert() {
    try {
      editModel.insert();
      clearAfterInsert();
    }
    catch (final Exception e) {
      throw new RuntimeException(e);
    }
  }

  private void update() {
    if (EntityUiUtil.confirm(FrameworkMessages.get(FrameworkMessages.CONFIRM_UPDATE))) {
      try {
        editModel.update();
      }
      catch (final Exception e) {
        throw new RuntimeException(e);
      }
    }
  }

  private void delete() {
    if (EntityUiUtil.confirm(FrameworkMessages.get(FrameworkMessages.CONFIRM_DELETE_ENTITY))) {
      try {
        editModel.delete();
      }
      catch (final Exception e) {
        throw new RuntimeException(e);
      }
    }
  }

  private void clearAfterInsert() {
    editModel.clear();
    requestInitialFocus();
  }

  private void requestInitialFocus() {
    if (initialFocusPropertyID != null && controls.containsKey(initialFocusPropertyID)) {
      controls.get(initialFocusPropertyID).requestFocus();
    }
  }

  private void checkControl(final String propertyID) {
    if (controls.containsKey(propertyID)) {
      throw new IllegalStateException("Control has already been created for property: " + propertyID);
    }
  }
}
