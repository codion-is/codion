/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.ui.control;

import org.jminor.common.model.Value;

import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * Binds a SpinnerNumberModel to a int based property.
 */
final class IntSpinnerValueLink extends AbstractValueLink {

  private final SpinnerNumberModel spinnerModel;

  /**
   * Instantiates a new IntSpinnerValueLink.
   * @param modelValue the model value
   * @param linkType the link type
   * @param spinnerModel the spinner model to use
   */
  IntSpinnerValueLink(final Value modelValue, final LinkType linkType, final SpinnerNumberModel spinnerModel) {
    super(modelValue, linkType);
    this.spinnerModel = spinnerModel;
    this.spinnerModel.addChangeListener(new ChangeListener() {
      /** {@inheritDoc} */
      @Override
      public void stateChanged(final ChangeEvent e) {
        updateModel();
      }
    });
    updateUI();
  }

  /** {@inheritDoc} */
  @Override
  protected Object getUIValue() {
    return spinnerModel.getValue();
  }

  /** {@inheritDoc} */
  @Override
  protected void setUIValue(final Object value) {
    spinnerModel.setValue(value);
  }
}
