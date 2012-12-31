/*
 * Copyright (c) 2004 - 2012, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.ui.control;

import javax.swing.ButtonModel;

/**
 * A Control based on a boolean property
 */
public final class ToggleControl extends Control {

  private final ButtonModel buttonModel;

  /**
   * @param name the name
   * @param buttonModel the button model
   */
  public ToggleControl(final String name, final ButtonModel buttonModel) {
    super(name);
    this.buttonModel = buttonModel;
  }

  /**
   * @return the button model
   */
  public ButtonModel getButtonModel() {
    return buttonModel;
  }
}
