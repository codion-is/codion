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
public class BooleanValueLink<K> extends AbstractValueMapLink<K, Object> {

  private final ButtonModel buttonModel;

  public BooleanValueLink(final ButtonModel buttonModel, final ValueChangeMapEditModel<K, Object> editModel,
                          final K key) {
    this(buttonModel, editModel, key, LinkType.READ_WRITE);
  }

  public BooleanValueLink(final ButtonModel buttonModel, final ValueChangeMapEditModel<K, Object> editModel,
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
    return buttonModel.isSelected();
  }

  /** {@inheritDoc} */
  @Override
  protected void setUIValue(final Object propertyValue) {
    buttonModel.setSelected(propertyValue != null && (Boolean) propertyValue);
  }
}
