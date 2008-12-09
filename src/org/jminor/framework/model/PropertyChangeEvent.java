/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.model;

import java.awt.event.ActionEvent;

/**
 * Used when property change events are fired
 */
public class PropertyChangeEvent extends ActionEvent {

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

  public PropertyChangeEvent(final Property property, final Object newValue, final Object oldValue,
                             final boolean isModelChange, final boolean initialization) {
    super(property, 0, "propertyChange");
    this.newValue = newValue;
    this.oldValue = oldValue;
    this.isModelChange = isModelChange;
    this.initialization = initialization;
  }

  /**
   * @return the property who's value just changed
   */
  public Property getProperty() {
    return (Property) getSource();
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