/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.ui.control;

import org.jminor.common.model.Event;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * An abstract base class for linking a UI component to a property value.
 */
public abstract class AbstractPropertyLink extends Control {

  /**
   * The Object that owns the linked property
   */
  private final Object propertyOwner;

  /**
   * The link type
   */
  private final LinkType linkType;

  /**
   * True while the UI is being updated
   */
  private boolean isUpdatingUI = false;

  /**
   * True while the model is being updated
   */
  private boolean isUpdatingModel = false;

  /**
   * Instantiates a new AbstractPropertyLink
   * @param propertyOwner the owner of the property value
   * @param modelPropertyValueChangeEvent an Event on which the UI should be updated to reflect changes in the model
   * @param linkType the link Type
   */
  public AbstractPropertyLink(final Object propertyOwner, final Event modelPropertyValueChangeEvent,
                              final LinkType linkType) {
    if (propertyOwner == null)
      throw new IllegalArgumentException("Property owner cannot be null");

    this.propertyOwner = propertyOwner;
    this.linkType = linkType;

    if (linkType != LinkType.WRITE_ONLY && modelPropertyValueChangeEvent != null) {
      modelPropertyValueChangeEvent.addListener(new ActionListener() {
        public void actionPerformed(final ActionEvent e) {
          updateUI();
        }
      });
    }
  }

  /** {@inheritDoc} */
  @Override
  public void actionPerformed(final ActionEvent e) {
    updateModel();
  }

  /**
   * @return the owner of the linked property, the model
   */
  public Object getPropertyOwner() {
    return propertyOwner;
  }

  /**
   * @return the type of this property link
   */
  public LinkType getLinkType() {
    return linkType;
  }

  public final void updateModel() {
    if (linkType != LinkType.READ_ONLY && !isUpdatingModel && !isUpdatingUI) {
      try {
        isUpdatingModel = true;
        setModelPropertyValue(getUIPropertyValue());
      }
      finally {
        isUpdatingModel = false;
      }
    }
  }

  public final void updateUI() {
    if (linkType != LinkType.WRITE_ONLY && !isUpdatingModel) {
      try {
        isUpdatingUI = true;
        setUIPropertyValue(getModelPropertyValue());
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
  protected abstract Object getUIPropertyValue();

  /**
   * Sets the property value in the UI
   * @param propertyValue the value to represent in the UI
   */
  protected abstract void setUIPropertyValue(final Object propertyValue);
}