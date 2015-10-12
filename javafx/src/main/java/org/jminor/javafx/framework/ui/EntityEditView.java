/*
 * Copyright (c) 2004 - 2015, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.javafx.framework.ui;

import org.jminor.common.db.exception.DatabaseException;
import org.jminor.common.model.Event;
import org.jminor.common.model.EventInfoListener;
import org.jminor.common.model.Events;
import org.jminor.common.model.Values;
import org.jminor.common.model.valuemap.ValueChange;
import org.jminor.framework.db.EntityConnectionProvider;
import org.jminor.framework.domain.Entities;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.Property;
import org.jminor.javafx.framework.model.ObservableEntityList;
import org.jminor.javafx.framework.ui.values.PropertyValues;
import org.jminor.javafx.framework.ui.values.StringValue;

import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.layout.BorderPane;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public abstract class EntityEditView extends BorderPane {

  private final Entity entity;
  private final Map<String, Event<ValueChange<String, ?>>> valueChangeEventMap = new HashMap<>();
  private final EntityConnectionProvider connectionProvider;

  private boolean initialized = false;

  public EntityEditView(final String entityID, final EntityConnectionProvider connectionProvider) {
    this.entity = Entities.entity(entityID);
    this.connectionProvider = connectionProvider;
    bindEvents();
  }

  public final EntityEditView initializePanel() {
    if (!initialized) {
      initializeUI();
      initialized = true;
    }

    return this;
  }

  public final String getEntityID() {
    return entity.getEntityID();
  }

  public final void addValueListener(final String propertyID, final EventInfoListener<ValueChange<String, ?>> listener) {
    getValueChangeEvent(propertyID).addInfoListener(listener);
  }

  public final void setEntity(final Entity entity) {
    this.entity.setAs(entity);
  }

  public void update() throws DatabaseException {
    update(entity);
  }

  protected void update(final Entity entity) throws DatabaseException {
    if (entity.isModified()) {
      connectionProvider.getConnection().update(Collections.singletonList(entity));
    }
  }

  protected abstract Node initializeEditPanel();

  protected final ComboBox<Entity> createComboBox(final String propertyID) {
    final Property.ForeignKeyProperty property = Entities.getForeignKeyProperty(getEntityID(), propertyID);

    final ComboBox<Entity> box = new ComboBox<>(new ObservableEntityList(property.getReferencedEntityID(), connectionProvider));
    Values.link(PropertyValues.entityValue(propertyID, entity), PropertyValues.selectedItemValue(box.getSelectionModel()));
    try {
      ((ObservableEntityList) box.getItems()).refresh();
    }
    catch (final DatabaseException e) {
      throw new RuntimeException(e);
    }

    return box;
  }

  protected final TextField createTextField(final String propertyID) {
    final TextField textField = createTextField(Entities.getProperty(getEntityID(), propertyID));
    final StringValue<String> propertyValue = PropertyValues.stringPropertyValue(textField.textProperty());

    Values.link(PropertyValues.entityValue(propertyID, entity), propertyValue);

    return textField;
  }

  protected final TextField createIntegerField(final String propertyID) {
    final TextField textField = createTextField(Entities.getProperty(getEntityID(), propertyID));
    final StringValue<Integer> propertyValue = PropertyValues.integerPropertyValue(textField.textProperty());
    textField.textFormatterProperty().setValue(new TextFormatter(propertyValue.getConverter()));

    Values.link(PropertyValues.entityValue(propertyID, entity), propertyValue);

    return textField;
  }

  protected final TextField createDoubleField(final String propertyID) {
    final TextField textField = createTextField(Entities.getProperty(getEntityID(), propertyID));
    final StringValue<Double> propertyValue = PropertyValues.doublePropertyValue(textField.textProperty());
    textField.textFormatterProperty().setValue(new TextFormatter(propertyValue.getConverter()));

    Values.link(PropertyValues.entityValue(propertyID, entity), propertyValue);

    return textField;
  }

  private TextField createTextField(final Property property) {
    final TextField textField = new TextField();

    return textField;
  }

  private void initializeUI() {
    setCenter(initializeEditPanel());
  }

  private void bindEvents() {
    entity.addValueListener(valueChange -> {
      final Event<ValueChange<String, ?>> valueChangeEvent = valueChangeEventMap.get(valueChange.getKey());
      if (valueChangeEvent != null) {
        valueChangeEvent.fire(valueChange);
      }
    });
  }

  private Event<ValueChange<String, ?>> getValueChangeEvent(final String propertyID) {
    if (!valueChangeEventMap.containsKey(propertyID)) {
      valueChangeEventMap.put(propertyID, Events.<ValueChange<String, ?>>event());
    }

    return valueChangeEventMap.get(propertyID);
  }
}
