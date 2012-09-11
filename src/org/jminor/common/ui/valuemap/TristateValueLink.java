/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.ui.valuemap;

import org.jminor.common.model.checkbox.TristateButtonModel;
import org.jminor.common.model.valuemap.ValueMapEditModel;
import org.jminor.common.ui.control.LinkType;

/**
 * A class for linking a UI component to a boolean value.
 */
public final class TristateValueLink<K> extends BooleanValueLink<K> {

  /**
   * Instantiates a new TristateValueLink.
   * @param buttonModel the button model to link
   * @param editModel the edit model
   * @param key the key of the property to link
   */
  public TristateValueLink(final TristateButtonModel buttonModel, final ValueMapEditModel<K, Object> editModel,
                           final K key) {
    this(buttonModel, editModel, key, LinkType.READ_WRITE);
  }

  /**
   * Instantiates a new TristateValueLink.
   * @param buttonModel the button model to link
   * @param editModel the edit model
   * @param key the key of the property to link
   * @param linkType the link type
   */
  public TristateValueLink(final TristateButtonModel buttonModel, final ValueMapEditModel<K, Object> editModel,
                           final K key, final LinkType linkType) {
    super(buttonModel, editModel, key, linkType);
    updateUI();
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
