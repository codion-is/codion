/*
 * Copyright (c) 2004 - 2012, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.ui.control;

import org.jminor.common.model.StateObserver;
import org.jminor.common.model.checkbox.TristateButtonModel;

final class TristateValueLink extends ToggleValueLink {

  TristateValueLink(final TristateButtonModel buttonModel, final ModelValue modelValue, final String caption,
                    final LinkType linkType, final StateObserver enabledObserver) {
    super(buttonModel, modelValue, caption, linkType, enabledObserver);
  }

  /** {@inheritDoc} */
  @Override
  protected Object getUIValue() {
    if (((TristateButtonModel) getButtonModel()).isIndeterminate()) {
      return null;
    }

    return super.getUIValue();
  }

  /** {@inheritDoc} */
  @Override
  protected void setUIValue(final Object value) {
    if (value == null) {
      ((TristateButtonModel) getButtonModel()).setIndeterminate();
    }
    else {
      super.setUIValue(value);
    }
  }
}
