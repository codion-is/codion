/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.ui.valuemap;

import org.jminor.common.model.Util;
import org.jminor.common.model.valuemap.ValueChangeMapEditModel;
import org.jminor.common.ui.control.LinkType;
import org.jminor.common.ui.textfield.DoubleField;

import java.text.NumberFormat;

/**
 * A class for linking a DoubleField to a ValueChangeMapEditModel double property value.
 */
public final class DoubleValueLink<K> extends TextValueLink<K> {

  private static final ThreadLocal<NumberFormat> FORMAT = new ThreadLocal<NumberFormat>() {
    @Override
    protected NumberFormat initialValue() {
      final NumberFormat ret = NumberFormat.getNumberInstance();
      ret.setGroupingUsed(false);
      return ret;
    }
  };

  /**
   * Instantiates a new DoubleValueLink.
   * @param textField the double field to link
   * @param editModel the edit model
   * @param key the key of the property to link
   * @param immediateUpdate if true the model value is update on each keystroke
   * @param linkType the link type
   */
  public DoubleValueLink(final DoubleField textField, final ValueChangeMapEditModel<K, Object> editModel,
                         final K key, final boolean immediateUpdate, final LinkType linkType) {
    super(textField, editModel, key, immediateUpdate, linkType);
  }

  /** {@inheritDoc} */
  @Override
  protected Object getValueFromText(final String text) {
    try {
      return Util.getDouble(text);
    }
    catch (NumberFormatException nf) {
      throw new RuntimeException(nf);
    }
  }

  /** {@inheritDoc} */
  @Override
  protected String getValueAsText(final Object value) {
    return value == null ? "" : FORMAT.get().format(value);
  }
}
