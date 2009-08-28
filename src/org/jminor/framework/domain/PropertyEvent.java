/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.domain;

import java.awt.event.ActionEvent;

/**
 * Used when property change events are fired
 */
public class PropertyEvent extends ActionEvent {

  /**
   * The ID of the entity owning the property
   */
  private final String entityID;

  /**
   * The property
   */
  private final Property property;

  /**
   * The new property value
   */
  private final Object newValue;

  /**
   * The old property value
   */
  private final Object oldValue;

  /**
   * True if this change event is coming from the model, false if it is coming from the UI
   */
  private final boolean isModelChange;

  /**
   * True if this property change indicates an initialization, that is, the property did not
   * have a value before this value change
   */
  private final boolean initialization;

  /**
   * Instantiates a new PropertyEvent
   * @param source the source of the property value change
   * @param entityID the ID of the entity which owns the property
   * @param property the property
   * @param newValue the new value
   * @param oldValue the old value
   * @param isModelChange true if the value change originates from the model, false if it originates in the UI
   * @param initialization true if the property value was being initialized
   */
  public PropertyEvent(final Object source, final String entityID, final Property property, final Object newValue,
                       final Object oldValue, final boolean isModelChange, final boolean initialization) {
    super(source, 0, property.propertyID);
    this.entityID = entityID;
    this.property = property;
    this.newValue = newValue;
    this.oldValue = oldValue;
    this.isModelChange = isModelChange;
    this.initialization = initialization;
  }

  /**
   * @return the ID of the entity owning the property
   */
  public String getEntityID() {
    return entityID;
  }

  /**
   * @return the property which value just changed
   */
  public Property getProperty() {
    return property;
  }

  /**
   * @return the property's old value
   */
  public Object getOldValue() {
    return oldValue;
  }

  /**
   * @return the property's new value
   */
  public Object getNewValue() {
    return newValue;
  }

  /**
   * @return true if this property change is coming from the model,
   * false if it is coming from the UI
   */
  public boolean isModelChange() {
    return isModelChange;
  }

  /**
   * @return true if this property change is coming from the UI,
   * false if it is coming from the model
   */
  public boolean isUIChange() {
    return !isModelChange;
  }

  /**
   * @return true if this property did not have a value prior to this value change
   */
  public boolean isInitialization() {
    return initialization;
  }
}