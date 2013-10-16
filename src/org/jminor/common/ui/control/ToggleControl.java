/*
 * Copyright (c) 2004 - 2012, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.ui.control;

import org.jminor.common.model.StateObserver;
import org.jminor.common.model.checkbox.TristateButtonModel;

import javax.swing.ButtonModel;
import java.awt.event.ActionEvent;

/**
 * A Control based on a boolean property
 */
public final class ToggleControl extends Control {

  private final ButtonModel buttonModel;

  /**
   * @param name the name
   * @param buttonModel the button model
   */
  public ToggleControl(final String name, final ButtonModel buttonModel, final StateObserver enabledObserver) {
    super(name, enabledObserver);
    this.buttonModel = buttonModel;
  }

  /**
   * @return the button model
   */
  public ButtonModel getButtonModel() {
    return buttonModel;
  }

  @Override
  public void actionPerformed(final ActionEvent e) {
    if (buttonModel instanceof TristateButtonModel) {
      ((TristateButtonModel) buttonModel).iterateState();
    }
    else {
      buttonModel.setSelected(!buttonModel.isSelected());
    }
  }

  @Override
  protected Control doSetMnemonic(final int mnemonic) {
    this.buttonModel.setMnemonic(mnemonic);
    return super.doSetMnemonic(mnemonic);
  }
}
