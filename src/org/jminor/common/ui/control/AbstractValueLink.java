/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.ui.control;

import org.jminor.common.model.EventObserver;
import org.jminor.common.model.Util;

import javax.swing.SwingUtilities;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * An abstract base class for linking a UI component to a model value.
 * @param <T> the type of the value owner
 * @param <V> the type of the value
 */
public abstract class AbstractValueLink<T, V> extends Control {

  /**
   * The Object that owns the linked property
   */
  private final T valueOwner;

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
   * Instantiates a new AbstractValueLink
   * @param valueOwner the owner of the property value
   * @param modelValueChangeEvent an EventObserver notified each time the UI should be updated to reflect changes
   * to the value in the model
   * @param linkType the link Type
   */
  public AbstractValueLink(final T valueOwner, final EventObserver modelValueChangeEvent, final LinkType linkType) {
    Util.rejectNullValue(valueOwner, "valueOwner");
    Util.rejectNullValue(linkType, "linkType");
    this.valueOwner = valueOwner;
    this.linkType = linkType;
    if (linkType != LinkType.WRITE_ONLY && modelValueChangeEvent != null) {
      modelValueChangeEvent.addListener(new ActionListener() {
        /** {@inheritDoc} */
        public void actionPerformed(final ActionEvent e) {
          updateUI();
        }
      });
    }
  }

  /** {@inheritDoc} */
  @Override
  public final void actionPerformed(final ActionEvent e) {
    updateModel();
  }

  /**
   * @return the owner of the linked property, the model
   */
  public final T getValueOwner() {
    return valueOwner;
  }

  /**
   * @return the type of this link
   */
  public final LinkType getLinkType() {
    return linkType;
  }

  /**
   * Updates the model according to the UI.
   */
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

  /**
   * Updates the UI according to the model.
   */
  public final void updateUI() {
    if (linkType != LinkType.WRITE_ONLY && !isUpdatingModel) {
      try {
        isUpdatingUI = true;
        if (!SwingUtilities.isEventDispatchThread()) {
          try {
            SwingUtilities.invokeAndWait(new Runnable() {
              /** {@inheritDoc} */
              public void run() {
                setUIValue(getModelValue());
              }
            });
          }
          catch (Exception e) {
            throw new RuntimeException(e);
          }
        }
        else {
          setUIValue(getModelValue());
        }
      }
      finally {
        isUpdatingUI = false;
      }
    }
  }

  /**
   * @return the model value of the linked property
   */
  public abstract V getModelValue();

  /**
   * Sets the value in the model
   * @param value the value to set for property
   */
  public abstract void setModelValue(final V value);

  /**
   * @return the value according to the UI
   */
  protected abstract V getUIValue();

  /**
   * Sets the value in the UI
   * @param value the value to represent in the UI
   */
  protected abstract void setUIValue(final V value);
}