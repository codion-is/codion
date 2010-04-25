/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.ui.control;

import org.jminor.common.model.Event;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * An abstract base class for linking a UI component to a model value.
 */
public abstract class AbstractValueLink<K, T> extends Control {

  /**
   * The Object that owns the linked property
   */
  private final K valueOwner;

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
   * @param valueOwner the owner of the property value
   * @param modelValueChangeEvent an Event on which the UI should be updated to reflect changes in the model
   * @param linkType the link Type
   */
  public AbstractValueLink(final K valueOwner, final Event modelValueChangeEvent,
                           final LinkType linkType) {
    if (valueOwner == null)
      throw new IllegalArgumentException("Property owner cannot be null");

    this.valueOwner = valueOwner;
    this.linkType = linkType;

    if (linkType != LinkType.WRITE_ONLY && modelValueChangeEvent != null) {
      modelValueChangeEvent.addListener(new ActionListener() {
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
  public K getValueOwner() {
    return valueOwner;
  }

  /**
   * @return the type of this link
   */
  public LinkType getLinkType() {
    return linkType;
  }

  public final void updateModel() {
    if (linkType != LinkType.READ_ONLY && !isUpdatingModel && !isUpdatingUI) {
      try {
        isUpdatingModel = true;
        setModelValue(getUIValue());
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
        setUIValue(getModelValue());
      }
      finally {
        isUpdatingUI = false;
      }
    }
  }

  /**
   * @return the model value of the linked property
   */
  public abstract T getModelValue();

  /**
   * Sets the value in the model
   * @param value the value to set for property
   */
  public abstract void setModelValue(final T value);

  /**
   * @return the value according to the UI
   */
  protected abstract T getUIValue();

  /**
   * Sets the value in the UI
   * @param value the value to represent in the UI
   */
  protected abstract void setUIValue(final T value);
}