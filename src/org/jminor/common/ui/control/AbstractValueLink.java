/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.ui.control;

import org.jminor.common.model.EventAdapter;
import org.jminor.common.model.StateObserver;
import org.jminor.common.model.Util;
import org.jminor.common.model.Value;

import javax.swing.SwingUtilities;
import java.awt.event.ActionEvent;

/**
 * An abstract base class for linking a UI component to a model value.
 * @param <V> the type of the value
 */
public abstract class AbstractValueLink<V> extends Control {

  /**
   * The Object wrapping the model value
   */
  private final Value<V> modelValue;

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
   * @param modelValue the value wrapper for the linked value
   * @param linkType the link Type
   */
  public AbstractValueLink(final Value<V> modelValue, final LinkType linkType) {
    this(modelValue, linkType, null);
  }

  /**
   * Instantiates a new AbstractValueLink
   * @param modelValue the value wrapper for the linked value
   * @param linkType the link Type
   * @param enabledObserver the state observer dictating the enable state of the control associated with this value link
   */
  public AbstractValueLink(final Value<V> modelValue, final LinkType linkType, final StateObserver enabledObserver) {
    super(null, enabledObserver);
    Util.rejectNullValue(modelValue, "modelValue");
    Util.rejectNullValue(linkType, "linkType");
    this.modelValue = modelValue;
    this.linkType = linkType;
    if (linkType != LinkType.WRITE_ONLY && modelValue.getChangeEvent() != null) {
      modelValue.getChangeEvent().addListener(new EventAdapter() {
        /** {@inheritDoc} */
        @Override
        public void eventOccurred() {
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
   * @return true if the underlying model value is null
   */
  public final boolean isModelValueNull() {
    return modelValue.get() == null;
  }

  /**
   * @return the type of this link
   */
  protected final LinkType getLinkType() {
    return linkType;
  }

  /**
   * Updates the model according to the UI.
   */
  protected final void updateModel() {
    if (linkType != LinkType.READ_ONLY && !isUpdatingModel && !isUpdatingUI) {
      try {
        isUpdatingModel = true;
        modelValue.set(getUIValue());
      }
      finally {
        isUpdatingModel = false;
      }
    }
  }

  /**
   * Updates the UI according to the model.
   */
  protected final void updateUI() {
    if (linkType != LinkType.WRITE_ONLY && !isUpdatingModel) {
      try {
        isUpdatingUI = true;
        if (!SwingUtilities.isEventDispatchThread()) {
          try {
            SwingUtilities.invokeAndWait(new Runnable() {
              /** {@inheritDoc} */
              @Override
              public void run() {
                setUIValue(modelValue.get());
              }
            });
          }
          catch (Exception e) {
            throw new RuntimeException(e);
          }
        }
        else {
          setUIValue(modelValue.get());
        }
      }
      finally {
        isUpdatingUI = false;
      }
    }
  }

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