/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.ui.control;

import org.jminor.common.model.EventObserver;

import javax.swing.JFormattedTextField;
import javax.swing.text.MaskFormatter;
import java.text.Format;
import java.text.ParseException;

/**
 * Binds a JFormattedField to a string based bean property.
 */
public class FormattedTextBeanValueLink extends TextBeanValueLink {

  private final Format format;
  private final String placeholder;

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
    super(textComponent, owner, propertyName, valueClass, valueChangeEvent, linkType);
    if (format == null) {
      throw new IllegalArgumentException("Format is null");
    }
    this.format = format;
    this.placeholder = Character.toString((((MaskFormatter) textComponent.getFormatter()).getPlaceholderCharacter()));
    updateUI();
  }

  /** {@inheritDoc} */
  @Override
  protected final String getValueAsString(final Object value) {
    return value == null ? null : format.format(value);
  }

  /**
   * This is a very strict implementation, formatted values are considered
   * invalid until all placeholder characters have been replaced, resulting in a null return value
   * @return the value, if a formatter is present, the formatted value is returned
   */
  @Override
  protected final Object getUIValue() {
    final String text = getText();
    if (text != null && text.contains(placeholder)) {
      return null;
    }

    try {
      return text != null ? translate(format.parseObject(text)) : null;
    }
    catch (ParseException nf) {
      return null;
    }
  }

  /**
   * Provides a hook into the UI value retrieval.
   * Override to manipulate the value coming from the UI before it
   * is set in the model.
   * @param parsedValue the value returned directly from the UI component
   * @return the translated value
   */
  protected Object translate(final Object parsedValue) {
    return parsedValue;
  }
}
