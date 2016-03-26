/*
 * Copyright (c) 2004 - 2015, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.javafx.framework.ui;

import org.jminor.common.model.Conjunction;
import org.jminor.common.model.Item;
import org.jminor.common.model.State;
import org.jminor.common.model.States;
import org.jminor.framework.domain.Entities;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.Property;
import org.jminor.framework.i18n.FrameworkMessages;
import org.jminor.javafx.framework.model.EntityEditModel;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Control;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;

import java.sql.Types;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public abstract class EntityEditView extends BorderPane {

  private static final KeyCode INSERT_KEY_CODE = KeyCode.getKeyCode(FrameworkMessages.get(FrameworkMessages.INSERT_MNEMONIC));
  private static final KeyCode UPDATE_KEY_CODE = KeyCode.getKeyCode(FrameworkMessages.get(FrameworkMessages.UPDATE_MNEMONIC));
  private static final KeyCode DELETE_KEY_CODE = KeyCode.getKeyCode(FrameworkMessages.get(FrameworkMessages.DELETE_MNEMONIC));
  private static final KeyCode CLEAR_KEY_CODE = KeyCode.getKeyCode(FrameworkMessages.get(FrameworkMessages.CLEAR_MNEMONIC));
  private static final KeyCode REFRESH_KEY_CODE = KeyCode.getKeyCode(FrameworkMessages.get(FrameworkMessages.REFRESH_MNEMONIC));

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

  public final EntityEditModel getEditModel() {
    return editModel;
  }

  public final void requestInitialFocus() {
    if (initialFocusPropertyID != null && controls.containsKey(initialFocusPropertyID)) {
      controls.get(initialFocusPropertyID).requestFocus();
    }
    else {
      requestFocus();
    }
  }

  public final Node getButtonPanel() {
    final Button insert = createInsertButton();
    insert.maxWidthProperty().setValue(Double.MAX_VALUE);
    final Button update = createUpdateButton();
    update.maxWidthProperty().setValue(Double.MAX_VALUE);
    final Button delete = createDeleteButton();
    delete.maxWidthProperty().setValue(Double.MAX_VALUE);
    final Button clear = createClearButton();
    clear.maxWidthProperty().setValue(Double.MAX_VALUE);
    final Button refresh = createRefreshButton();
    refresh.maxWidthProperty().setValue(Double.MAX_VALUE);

    final VBox box = new VBox(insert, update, delete, clear, refresh);

    return box;
  }

  public void selectInputControl() {
    final List<Property> properties = Entities.getProperties(editModel.getEntityID(), controls.keySet());
    Collections.sort(properties, (o1, o2) -> o1.toString().compareToIgnoreCase(o2.toString()));
    final ListView<Property> propertyList = new ListView<>(FXCollections.observableArrayList(properties));
    propertyList.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
    final Dialog<Property> dialog = new Dialog<>();
    dialog.getDialogPane().setContent(propertyList);
    dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
    dialog.setResultConverter(buttonType -> {
      if (buttonType != null && buttonType.getButtonData() == ButtonBar.ButtonData.OK_DONE) {
        return propertyList.getSelectionModel().getSelectedItem();
      }

      return null;
    });

    Platform.runLater(propertyList::requestFocus);
    final Optional<Property> result = dialog.showAndWait();
    if (result.isPresent()) {
      controls.get(result.get().getPropertyID()).requestFocus();
    }
  }

  protected final void setInitialFocusProperty(final String initialFocusPropertyID) {
    this.initialFocusPropertyID = initialFocusPropertyID;
  }

  protected abstract Node initializeEditPanel();

  protected final ComboBox<Entity> createForeignKeyComboBox(final String foreignKeyPropertyID) {
    checkControl(foreignKeyPropertyID);
    final ComboBox<Entity> box = FXUiUtil.createForeignKeyComboBox(Entities.getForeignKeyProperty(editModel.getEntityID(),
            foreignKeyPropertyID), editModel);

    controls.put(foreignKeyPropertyID, box);

    return box;
  }

  protected final ComboBox<Item> createItemComboBox(final String propertyID) {
    checkControl(propertyID);
    final ComboBox<Item> box = FXUiUtil.createItemComboBox((Property.ValueListProperty)
            Entities.getProperty(editModel.getEntityID(), propertyID), editModel);

    controls.put(propertyID, box);

    return box;
  }

  protected final TextField createTextField(final String propertyID) {
    checkControl(propertyID);
    final Property property = Entities.getProperty(editModel.getEntityID(), propertyID);
    final TextField textField;
    switch (property.getType()) {
      case Types.INTEGER:
        textField = FXUiUtil.createIntegerField(Entities.getProperty(editModel.getEntityID(), propertyID), editModel);
        break;
      case Types.BIGINT:
        textField = FXUiUtil.createLongField(Entities.getProperty(editModel.getEntityID(), propertyID), editModel);
        break;
      case Types.DOUBLE:
        textField = FXUiUtil.createDoubleField(Entities.getProperty(editModel.getEntityID(), propertyID), editModel);
        break;
      case Types.VARCHAR:
        textField = FXUiUtil.createTextField(Entities.getProperty(editModel.getEntityID(), propertyID), editModel);
        break;
      default:
        throw new IllegalArgumentException("Text field type for property: " + propertyID + " is not defined");
    }

    controls.put(propertyID, textField);

    return textField;
  }

  protected final DatePicker createDatePicker(final String propertyID) {
    checkControl(propertyID);
    final DatePicker picker = FXUiUtil.createDatePicker(Entities.getProperty(editModel.getEntityID(), propertyID), editModel);
    controls.put(propertyID, picker);

    return picker;
  }

  protected final Label createLabel(final String propertyID) {
    return new Label(Entities.getProperty(getEditModel().getEntityID(), propertyID).getCaption());
  }

  private void initializeUI() {
    setCenter(initializeEditPanel());
    addKeyEvents();
  }

  private void addKeyEvents() {
    setOnKeyReleased(event -> {
      if (event.isAltDown()) {
        if (event.getCode().equals(INSERT_KEY_CODE)) {
          insert();
          event.consume();
        }
        else if (event.getCode().equals(UPDATE_KEY_CODE)) {
          update();
          event.consume();
        }
        else if (event.getCode().equals(DELETE_KEY_CODE)) {
          delete();
          event.consume();
        }
        else if (event.getCode().equals(CLEAR_KEY_CODE)) {
          editModel.clear();
          event.consume();
        }
        else if (event.getCode().equals(REFRESH_KEY_CODE)) {
          editModel.refresh();
          event.consume();
        }
      }
    });
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
            editModel.getEntityNewObserver().getReversedObserver(),
            editModel.getModifiedObserver());
    FXUiUtil.link(button.disableProperty(), existingAndModifiedState.getReversedObserver());

    return button;
  }

  private Button createDeleteButton() {
    final Button button = new Button(FrameworkMessages.get(FrameworkMessages.DELETE));
    button.setOnAction(event -> delete());
    FXUiUtil.link(button.disableProperty(), editModel.getEntityNewObserver());

    return button;
  }

  private Button createClearButton() {
    final Button button = new Button(FrameworkMessages.get(FrameworkMessages.CLEAR));
    button.setOnAction(event -> editModel.clear());

    return button;
  }

  private Button createRefreshButton() {
    final Button button = new Button(FrameworkMessages.get(FrameworkMessages.REFRESH));
    button.setOnAction(event -> editModel.refresh());

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
    if (FXUiUtil.confirm(FrameworkMessages.get(FrameworkMessages.CONFIRM_UPDATE))) {
      try {
        editModel.update();
      }
      catch (final Exception e) {
        throw new RuntimeException(e);
      }
    }
  }

  private void delete() {
    if (FXUiUtil.confirm(FrameworkMessages.get(FrameworkMessages.CONFIRM_DELETE_ENTITY))) {
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

  private void checkControl(final String propertyID) {
    if (controls.containsKey(propertyID)) {
      throw new IllegalStateException("Control has already been created for property: " + propertyID);
    }
  }
}
