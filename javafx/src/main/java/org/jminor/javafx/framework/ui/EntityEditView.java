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
import org.jminor.javafx.framework.model.ObservableEntityList;
import org.jminor.javafx.framework.ui.values.PropertyValues;

import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;

public abstract class EntityEditView extends BorderPane {

  private final EntityEditModel editModel;
  private boolean initialized = false;

  private Node initialFocusNode;

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

  public Node getButtonPanel() {
    final GridPane buttonPane = new GridPane();
    buttonPane.addRow(0, createInsertButton());
    buttonPane.addRow(1, createUpdateButton());
    buttonPane.addRow(2, createDeleteButton());
    buttonPane.addRow(3, createClearButton());

    return buttonPane;
  }

  protected void setInitialFocusNode(final Node initialFocusNode) {
    this.initialFocusNode = initialFocusNode;
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
    return EntityFXUtil.createTextField(Entities.getProperty(getModel().getEntityID(), propertyID), editModel);
  }

  protected final TextField createIntegerField(final String propertyID) {
    return EntityFXUtil.createIntegerField(Entities.getProperty(getModel().getEntityID(), propertyID), editModel);
  }

  protected final TextField createDoubleField(final String propertyID) {
    return EntityFXUtil.createDoubleField(Entities.getProperty(getModel().getEntityID(), propertyID), editModel);
  }

  protected final DatePicker createDatePicker(final String propertyID) {
    return EntityFXUtil.createDatePicker(Entities.getProperty(getModel().getEntityID(), propertyID), editModel);
  }

  private void initializeUI() {
    setCenter(initializeEditPanel());
  }

  private Button createInsertButton() {
    final Button button = new Button(FrameworkMessages.get(FrameworkMessages.INSERT));
    button.setOnAction(event -> {
      try {
        editModel.insert();
        clearAfterInsert();
      }
      catch (final Exception e) {
        throw new RuntimeException(e);
      }
    });

    return button;
  }

  private Button createUpdateButton() {
    final Button button = new Button(FrameworkMessages.get(FrameworkMessages.UPDATE));
    button.setOnAction(event -> {
      try {
        editModel.update();
      }
      catch (final Exception e) {
        throw new RuntimeException(e);
      }
    });
    final State notNewAndModifiedState = States.aggregateState(Conjunction.AND,
            getModel().getEntityNewObserver().getReversedObserver(),
            getModel().getModifiedObserver());
    EntityFXUtil.linkToEnabledState(button, notNewAndModifiedState.getObserver());

    return button;
  }

  private Button createDeleteButton() {
    final Button button = new Button(FrameworkMessages.get(FrameworkMessages.DELETE));
    button.setOnAction(event -> {
      try {
        editModel.delete();
      }
      catch (final Exception e) {
        throw new RuntimeException(e);
      }
    });
    EntityFXUtil.linkToEnabledState(button, getModel().getEntityNewObserver());

    return button;
  }

  private Button createClearButton() {
    final Button button = new Button(FrameworkMessages.get(FrameworkMessages.CLEAR));
    button.setOnAction(event -> {
      try {
        editModel.clear();
      }
      catch (final Exception e) {
        throw new RuntimeException(e);
      }
    });

    return button;
  }

  private void clearAfterInsert() {
    editModel.clear();
    requestInitialFocus();
  }

  private void requestInitialFocus() {
    if (initialFocusNode != null) {
      initialFocusNode.requestFocus();
    }
  }
}
