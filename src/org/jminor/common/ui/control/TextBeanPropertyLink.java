/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.ui.control;

import org.jminor.common.model.Event;

import javax.swing.JFormattedTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import javax.swing.text.MaskFormatter;
import java.text.Format;
import java.text.ParseException;

public class TextBeanPropertyLink extends BeanPropertyLink implements DocumentListener {

  private final Document document;
  private final String placeholder;
  private final Format format;

  public TextBeanPropertyLink(final JTextComponent textComponent, final Object owner, final String propertyName,
                              final Class<?> valueClass, final Event propertyChangeEvent, final String name) {
    this(textComponent, owner, propertyName, valueClass, propertyChangeEvent, name, LinkType.READ_WRITE, null);
  }

  public TextBeanPropertyLink(final JTextComponent textComponent, final Object owner, final String propertyName,
                              final Class<?> valueClass, final Event propertyChangeEvent, final String name,
                              final LinkType linkType, final Format format) {
    super(owner, propertyName, valueClass, propertyChangeEvent, name, linkType);
    this.document = textComponent.getDocument();
    this.format = format;
    this.placeholder = textComponent instanceof JFormattedTextField ?
            Character.toString(
                    (((MaskFormatter) ((JFormattedTextField)textComponent).getFormatter()).getPlaceholderCharacter()))
            : null;
    if (linkType == LinkType.READ_ONLY)
      textComponent.setEditable(false);
    updateUI();
    this.document.addDocumentListener(this);
  }

  /** {@inheritDoc} */
  public void insertUpdate(final DocumentEvent e) {
    updateModel();
  }

  /** {@inheritDoc} */
  public void removeUpdate(final DocumentEvent e) {
    updateModel();
  }

  /** {@inheritDoc} */
  public void changedUpdate(final DocumentEvent e) {}

  protected String getPropertyValueAsString(final Object propertyValue) {
    if (propertyValue == null)
      return null;

    return format == null ? propertyValue.toString() : format.format(propertyValue);
  }

  /** {@inheritDoc} */
  @Override
  protected void setUIPropertyValue(final Object propertyValue) {
    try {
      document.remove(0, document.getLength());
      document.insertString(0, getPropertyValueAsString(propertyValue), null);
    }
    catch (BadLocationException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * This is a very strict implementation in case a formatter is being used, formatted values are considered
   * invalid until all placeholder characters have been replaced and null is returned
   * @return the value, if a formatter is present, the formatted value is returned
   */
  @Override
  protected Object getUIPropertyValue() {
    final String text = getText();
    if (placeholder != null && text != null && text.contains(placeholder))
      return null;

    if (format != null)
      return getFormattedValue();

    return text;
  }

  protected Object getFormattedValue() {
    final String text = getText();
    try {
      if (text != null) {
        return format.parseObject(text);
      }
      else {
        return null;
      }
    }
    catch (ParseException nf) {
      return null;
    }
  }

  protected String getText() {
    try {
      return document.getText(0, document.getLength());
    }
    catch (BadLocationException e) {
      throw new RuntimeException(e);
    }
  }
}
