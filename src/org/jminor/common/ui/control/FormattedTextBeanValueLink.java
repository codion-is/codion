/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.ui.control;

import org.jminor.common.model.Event;

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

  public FormattedTextBeanValueLink(final JFormattedTextField textComponent, final Object owner,
                                    final String propertyName, final Class<?> valueClass,
                                    final Event valueChangeEvent, final LinkType linkType,
                                    final Format format) {
    super(textComponent, owner, propertyName, valueClass, valueChangeEvent, linkType);
    if (format == null) {
      throw new RuntimeException("Format is null");
    }
    this.format = format;
    this.placeholder = Character.toString((((MaskFormatter) textComponent.getFormatter()).getPlaceholderCharacter()));
    updateUI();
  }

  @Override
  protected final String getValueAsString(final Object value) {
    return value == null ? null : format.format(value);
  }

  /**
   * This is a very strict implementation, formatted values are considered
   * invalid until all placeholder characters have been replaced and null is returned
   * @return the value, if a formatter is present, the formatted value is returned
   */
  @Override
  protected Object getUIValue() {
    final String text = getText();
    if (text != null && text.contains(placeholder)) {
      return null;
    }

    try {
      return text != null ? format.parseObject(text) : null;
    }
    catch (ParseException nf) {
      return null;
    }
  }
}
