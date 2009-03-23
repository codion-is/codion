/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.ui.control;

import org.jminor.common.model.Event;
import org.jminor.common.model.State;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * An abstract property linking class
 */
public abstract class AbstractPropertyLink extends Control {

  private final Object propertyOwner;
  private final LinkType linkType;

  private boolean isUpdatingUI = false;
  private boolean isUpdatingProperty = false;

  public AbstractPropertyLink(final Object propertyOwner, final String name, final Event propertyChangeEvent,
                              final LinkType linkType, final State enabledState) {
    super(name, enabledState);
    if (propertyOwner == null)
      throw new IllegalArgumentException("Property owner cannot be null");

    this.propertyOwner = propertyOwner;
    this.linkType = linkType;

    if (linkType != LinkType.WRITE_ONLY && propertyChangeEvent != null) {
      propertyChangeEvent.addListener(new ActionListener() {
        public void actionPerformed(final ActionEvent e) {
          refreshUI();
        }
      });
    }
  }

  /** {@inheritDoc} */
  public void actionPerformed(final ActionEvent e) {
    refreshProperty();
  }

  /**
   * @return Value for property 'propertyOwner'.
   */
  public Object getPropertyOwner() {
    return propertyOwner;
  }

  /**
   * @return Value for property 'linkType'.
   */
  public LinkType getLinkType() {
    return linkType;
  }

  public final void refreshProperty() {
    if (linkType != LinkType.READ_ONLY && !isUpdatingProperty && !isUpdatingUI) {
      try {
        isUpdatingProperty = true;
        setModelPropertyValue(getUiPropertyValue());
      }
      finally {
        isUpdatingProperty = false;
      }
    }
  }

  public final void refreshUI() {
    if (linkType != LinkType.WRITE_ONLY && !isUpdatingProperty) {
      try {
        isUpdatingUI = true;
        setUiPropertyValue(getModelPropertyValue());
      }
      finally {
        isUpdatingUI = false;
      }
    }
  }

  /**
   * @return the model value of the linked property
   */
  public abstract Object getModelPropertyValue();

  /**
   * Sets the property value in the model
   * @param value the value to set for property
   */
  public abstract void setModelPropertyValue(final Object value);

  /**
   * @return the property value according to the UI
   */
  protected abstract Object getUiPropertyValue();

  /**
   * Update the UI according to the property value in the model
   * @param propertyValue the value to represent in the UI
   */
  protected abstract void setUiPropertyValue(final Object propertyValue);
}