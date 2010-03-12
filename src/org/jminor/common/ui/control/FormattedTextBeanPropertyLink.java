/*
 * Copyright (c) 2009, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.ui.control;

import org.jminor.common.model.Event;

import javax.swing.JFormattedTextField;
import javax.swing.text.MaskFormatter;
import java.text.Format;
import java.text.ParseException;

public class FormattedTextBeanPropertyLink extends TextBeanPropertyLink {

  private final Format format;
  private final String placeholder;

  public FormattedTextBeanPropertyLink(final JFormattedTextField textComponent, final Object owner,
                                       final String propertyName, final Class<?> valueClass,
                                       final Event propertyChangeEvent, final LinkType linkType,
                                       final Format format) {
    super(textComponent, owner, propertyName, valueClass, propertyChangeEvent, linkType);
    if (format == null)
      throw new RuntimeException("Format is null");
    this.format = format;
    this.placeholder = Character.toString((((MaskFormatter) textComponent.getFormatter()).getPlaceholderCharacter()));
    updateUI();
  }

  @Override
  protected String getPropertyValueAsString(final Object propertyValue) {
    return propertyValue == null ? null : format.format(propertyValue);
  }

  /**
   * This is a very strict implementation, formatted values are considered
   * invalid until all placeholder characters have been replaced and null is returned
   * @return the value, if a formatter is present, the formatted value is returned
   */
  @Override
  protected Object getUIPropertyValue() {
    final String text = getText();
    if (text != null && text.contains(placeholder))
      return null;

    try {
      return text != null ? format.parseObject(text) : null;
    }
    catch (ParseException nf) {
      return null;
    }
  }
}
