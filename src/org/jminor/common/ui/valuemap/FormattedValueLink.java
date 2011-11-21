/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.ui.valuemap;

import org.jminor.common.model.valuemap.ValueChangeMapEditModel;
import org.jminor.common.ui.control.LinkType;

import javax.swing.JFormattedTextField;
import java.text.Format;
import java.text.ParseException;

/**
 * A class for linking a JFormattedTextField to a ValueChangeMapEditModel property value.
 */
public class FormattedValueLink<K> extends TextValueLink<K> {

  private final JFormattedTextField.AbstractFormatter formatter;

  /**
   * Instantiates a new FormattedValueLink
   * @param textComponent the text component to link
   * @param editModel the ValueChangeMapEditModel instance
   * @param key the key to link
   * @param format the format
   * @param immediateUpdate if true then the underlying model value is updated on each keystroke,
   * @param linkType the link type
   */
  public FormattedValueLink(final JFormattedTextField textComponent, final ValueChangeMapEditModel<K, Object> editModel,
                            final K key, final Format format, final boolean immediateUpdate,
                            final LinkType linkType) {
    super(textComponent, editModel, key, immediateUpdate, linkType, format);
    this.formatter = textComponent.getFormatter();
    updateUI();
  }

  /** {@inheritDoc} */
  @Override
  protected final Object getValueFromText(final String text) {
    if (text == null) {
      return null;
    }

    try {
      return translate(getFormat().parseObject(text));
    }
    catch (ParseException nf) {
      return null;
    }
  }

  /** {@inheritDoc} */
  @Override
  protected String translate(final String text) {
    try {
      return (String) formatter.stringToValue(text);
    }
    catch (ParseException e) {
      return null;
    }
  }

  /**
   * Allows for a hook into the value parsing mechanism, so that
   * a value returned by the format parsing can be replaced with, say
   * a subclass, or some more appropriate value.
   * By default this simple returns the value.
   * @param parsedValue the value to translate
   * @return a translated value
   */
  protected Object translate(final Object parsedValue) {
    return parsedValue;
  }
}
