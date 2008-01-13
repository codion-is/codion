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
        updateProperty();
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
        updateUI();
      }
      finally {
        isUpdatingUI = false;
      }
    }
  }

  /**
   * @param value Value to set for property.
   * @throws org.jminor.common.model.UserException in case of an error
   */
  public abstract void setPropertyValue(final Object value);

  /**
   * @return Value the property value.
   * @throws org.jminor.common.model.UserException in case of an error
   */
  public abstract Object getPropertyValue();

  protected abstract void updateUI();

  protected abstract void updateProperty();
}