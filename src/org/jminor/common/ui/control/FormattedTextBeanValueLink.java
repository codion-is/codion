/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.ui.control;

import org.jminor.common.model.EventObserver;

import javax.swing.JFormattedTextField;
import java.text.Format;
import java.text.ParseException;

/**
 * Binds a JFormattedField to a string based bean property.
 */
public class FormattedTextBeanValueLink extends TextBeanValueLink {

  private final JFormattedTextField.AbstractFormatter formatter;

  /**
   * Instantiates a new FormattedTextBeanValueLink.
   * @param textComponent the text component to link with the value
   * @param owner the value owner
   * @param propertyName the property name
   * @param valueClass the value class
   * @param valueChangeEvent an EventObserver notified each time the value changes
   * @param linkType the link type
   * @param format the format
   */
  public FormattedTextBeanValueLink(final JFormattedTextField textComponent, final Object owner,
                                    final String propertyName, final Class<?> valueClass,
                                    final EventObserver valueChangeEvent, final LinkType linkType,
                                    final Format format) {
    super(textComponent, owner, propertyName, valueClass, valueChangeEvent, linkType, format);
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
