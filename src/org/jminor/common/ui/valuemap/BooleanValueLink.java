/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.ui.valuemap;

import org.jminor.common.model.valuemap.ValueChangeMapEditModel;
import org.jminor.common.ui.control.LinkType;

import javax.swing.ButtonModel;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

/**
 * A class for linking a UI component to a boolean value.
 */
public final class BooleanValueLink<K> extends AbstractValueMapLink<K, Object> {

  private final ButtonModel buttonModel;

  /**
   * Instantiates a new BooleanValueLink.
   * @param buttonModel the button model to link
   * @param editModel the edit model
   * @param key the key of the property to link
   */
  public BooleanValueLink(final ButtonModel buttonModel, final ValueChangeMapEditModel<K, Object> editModel,
                          final K key) {
    this(buttonModel, editModel, key, LinkType.READ_WRITE);
  }

  /**
   * Instantiates a new BooleanValueLink.
   * @param buttonModel the button model to link
   * @param editModel the edit model
   * @param key the key of the property to link
   * @param linkType the link type
   */
  public BooleanValueLink(final ButtonModel buttonModel, final ValueChangeMapEditModel<K, Object> editModel,
                          final K key, final LinkType linkType) {
    super(editModel, key, linkType);
    this.buttonModel = buttonModel;
    this.buttonModel.addItemListener(new ItemListener() {
      /** {@inheritDoc} */
      public void itemStateChanged(final ItemEvent e) {
        updateModel();
      }
    });
    updateUI();
  }

  /** {@inheritDoc} */
  @Override
  protected Object getUIValue() {
    return buttonModel.isSelected();
  }

  /** {@inheritDoc} */
  @Override
  protected void setUIValue(final Object value) {
    buttonModel.setSelected(value != null && (Boolean) value);
  }
}
