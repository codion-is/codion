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
public class ValueLink<V> extends Control {

  /**
   * The Object wrapping the model value
   */
  private final Value<V> modelValue;

  /**
   * The Object wrapping the ui value
   */
  private final Value<V> uiValue;

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
   * Instantiates a new ValueLink
   * @param modelValue the value wrapper for the linked value
   * @param linkType the link Type
   */
  public ValueLink(final Value<V> modelValue, final Value<V> uiValue, final LinkType linkType) {
    this(modelValue, uiValue, linkType, null);
  }

  /**
   * Instantiates a new ValueLink
   * @param modelValue the value wrapper for the linked value
   * @param linkType the link Type
   * @param enabledObserver the state observer dictating the enable state of the control associated with this value link
   */
  public ValueLink(final Value<V> modelValue, final Value<V> uiValue, final LinkType linkType, final StateObserver enabledObserver) {
    super(null, enabledObserver);
    Util.rejectNullValue(modelValue, "modelValue");
    Util.rejectNullValue(uiValue, "uiValue");
    Util.rejectNullValue(linkType, "linkType");
    this.modelValue = modelValue;
    this.uiValue = uiValue;
    this.linkType = linkType;
    updateUI();
    bindEvents(modelValue, uiValue, linkType);
  }

  /** {@inheritDoc} */
  @Override
  public final void actionPerformed(final ActionEvent e) {
    updateModel();
  }

  /**
   * Updates the model according to the UI.
   */
  protected final void updateModel() {
    if (linkType != LinkType.READ_ONLY && !isUpdatingModel && !isUpdatingUI) {
      try {
        isUpdatingModel = true;
        modelValue.set(uiValue.get());
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
                uiValue.set(modelValue.get());
              }
            });
          }
          catch (Exception e) {
            throw new RuntimeException(e);
          }
        }
        else {
          uiValue.set(modelValue.get());
        }
      }
      finally {
        isUpdatingUI = false;
      }
    }
  }

  private void bindEvents(final Value<V> modelValue, final Value<V> uiValue, final LinkType linkType) {
    if (linkType != LinkType.WRITE_ONLY && modelValue.getChangeEvent() != null) {
      modelValue.getChangeEvent().addListener(new EventAdapter() {
        /** {@inheritDoc} */
        @Override
        public void eventOccurred() {
          updateUI();
        }
      });
    }
    if (linkType != LinkType.READ_ONLY && uiValue.getChangeEvent() != null) {
      uiValue.getChangeEvent().addListener(new EventAdapter() {
        /** {@inheritDoc} */
        @Override
        public void eventOccurred() {
          updateModel();
        }
      });
    }
  }
}