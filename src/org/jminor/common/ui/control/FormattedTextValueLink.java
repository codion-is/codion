/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.ui.control;

import org.jminor.common.model.Value;

import javax.swing.JFormattedTextField;
import java.text.Format;
import java.text.ParseException;

/**
 * Binds a JFormattedField to a string based property.
 */
class FormattedTextValueLink extends TextValueLink {

  private final JFormattedTextField.AbstractFormatter formatter;

  /**
   * Instantiates a new FormattedTextValueLink.
   * @param textComponent the text component to link with the value
   * @param modelValue the model value
   * @param linkType the link type
   * @param format the format
   */
  FormattedTextValueLink(final JFormattedTextField textComponent, final Value modelValue,
                         final LinkType linkType, final Format format) {
    super(textComponent, modelValue, linkType, format, true);
    this.formatter = textComponent.getFormatter();
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
  protected final String translate(final String text) {
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
