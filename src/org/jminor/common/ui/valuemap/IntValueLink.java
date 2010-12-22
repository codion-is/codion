/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.ui.valuemap;

import org.jminor.common.model.Util;
import org.jminor.common.model.valuemap.ValueChangeMapEditModel;
import org.jminor.common.ui.control.LinkType;
import org.jminor.common.ui.textfield.IntField;

import java.text.NumberFormat;

/**
 * A class for linking a IntField to a ValueChangeMapEditor int key value.
 */
public final class IntValueLink<K> extends TextValueLink<K> {

  private final ThreadLocal<NumberFormat> format = new ThreadLocal<NumberFormat>() {
    @Override
    protected NumberFormat initialValue() {
      final NumberFormat ret = NumberFormat.getIntegerInstance();
      ret.setGroupingUsed(false);
      return ret;
    }
  };

  /**
   * Instantiates a new IntValueLink.
   * @param textField the int field to link
   * @param editModel the edit model
   * @param key the key of the property to link
   * @param immediateUpdate if true the model value is update on each keystroke
   * @param linkType the link type
   */
  public IntValueLink(final IntField textField, final ValueChangeMapEditModel<K, Object> editModel,
                      final K key, final boolean immediateUpdate, final LinkType linkType) {
    super(textField, editModel, key, immediateUpdate, linkType);
  }

  /** {@inheritDoc} */
  @Override
  protected Object getValueFromText(final String text) {
    try {
      return Util.getInt(text);
    }
    catch (NumberFormatException nf) {
      throw new RuntimeException(nf);
    }
  }

  /** {@inheritDoc} */
  @Override
  protected String getValueAsText(final Object value) {
    return value == null ? "" : format.get().format(value);
  }
}
