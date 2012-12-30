/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.ui.control;

import org.jminor.common.model.StateObserver;
import org.jminor.common.model.Value;

import javax.swing.ButtonModel;

/**
 * Binds a ButtonModel to a boolean based property.
 */
public final class ToggleValueLink extends ValueLink {

  private final ButtonModel buttonModel;

  /**
   * Instantiates a new ToggleValueLink.
   * @param buttonModel the button model to link with the value
   * @param modelValue the model value
   * @param caption the check box caption, if any
   * @param linkType the link type
   * @param enabledObserver the state observer dictating the enable state of the control associated with this value link
   */
  ToggleValueLink(final ButtonModel buttonModel, final Value modelValue, final Value uiValue, final String caption,
                  final LinkType linkType, final StateObserver enabledObserver) {
    super(modelValue, uiValue, linkType, enabledObserver);
    this.buttonModel = buttonModel;
    setName(caption);
  }

  /**
   * @return the button model
   */
  public final ButtonModel getButtonModel() {
    return buttonModel;
  }
}
