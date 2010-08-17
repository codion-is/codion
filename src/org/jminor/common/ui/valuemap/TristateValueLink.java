/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.ui.valuemap;

import org.jminor.common.model.checkbox.TristateButtonModel;
import org.jminor.common.model.valuemap.ValueChangeMapEditModel;
import org.jminor.common.ui.control.LinkType;

import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

/**
 * A class for linking a UI component to a boolean value.
 */
public final class TristateValueLink<K> extends AbstractValueMapLink<K, Object>{

  private final TristateButtonModel buttonModel;

  /**
   * Instantiates a new TristateValueLink.
   * @param buttonModel the button model to link
   * @param editModel the edit model
   * @param key the key of the property to link
   */
  public TristateValueLink(final TristateButtonModel buttonModel, final ValueChangeMapEditModel<K, Object> editModel,
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
  public TristateValueLink(final TristateButtonModel buttonModel, final ValueChangeMapEditModel<K, Object> editModel,
                           final K key, final LinkType linkType) {
    super(editModel, key, linkType);
    this.buttonModel = buttonModel;
    this.buttonModel.addItemListener(new ItemListener() {
      public void itemStateChanged(final ItemEvent e) {
        updateModel();
      }
    });
    updateUI();
  }

  /** {@inheritDoc} */
  @Override
  protected Object getUIValue() {
    if (buttonModel.isIndeterminate()) {
      return null;
    }

    return buttonModel.isSelected();
  }

  /** {@inheritDoc} */
  @Override
  protected void setUIValue(final Object value) {
    if (value == null) {
      buttonModel.setIndeterminate();
    }
    else {
      buttonModel.setSelected((Boolean) value);
    }
  }
}
