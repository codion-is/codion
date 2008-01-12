/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 *
 */
package org.jminor.framework.model;

import java.awt.event.ActionEvent;

/**
 * Used when property change events are fired
 */
public class PropertyChangeEvent extends ActionEvent {

  private final Object newValue;
  private final Object oldValue;
  private final boolean isModelChange;
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
   * @return Value for property 'property'.
   */
  public Property getProperty() {
    return (Property) getSource();
  }

  /**
   * @return Value for property 'oldValue'.
   */
  public Object getOldValue() {
    return oldValue;
  }

  /**
   * @return Value for property 'newValue'.
   */
  public Object getNewValue() {
    return newValue;
  }

  /**
   * @return Value for property 'modelChange'.
   */
  public boolean isModelChange() {
    return isModelChange;
  }

  /**
   * @return Value for property 'UIChange'.
   */
  public boolean isUIChange() {
    return !isModelChange;
  }

  /**
   * @return Value for property 'initialization'.
   */
  public boolean isInitialization() {
    return initialization;
  }
}