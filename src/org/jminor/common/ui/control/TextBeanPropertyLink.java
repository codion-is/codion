/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.ui.control;

import org.jminor.common.model.Event;
import org.jminor.common.model.State;

import javax.swing.JFormattedTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.JTextComponent;
import javax.swing.text.MaskFormatter;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.Format;
import java.text.ParseException;

public class TextBeanPropertyLink extends BeanPropertyLink implements DocumentListener {

  private final JTextComponent textComponent;
  private final String placeholder;
  private final Format format;

  public TextBeanPropertyLink(final JTextComponent textComponent, final Object owner, final String propertyName,
                              final Class<?> valueClass, final Event propertyChangeEvent, final String text) {
    this(textComponent, owner, propertyName, valueClass, propertyChangeEvent, text, LinkType.READ_WRITE, null, null);
  }

  public TextBeanPropertyLink(final JTextComponent textComponent, final Object owner, final String propertyName,
                              final Class<?> valueClass, final Event propertyChangeEvent, final String text,
                              final LinkType linkType, final Format format, final State enabledState) {
    super(owner, propertyName, valueClass, propertyChangeEvent, text, linkType, enabledState);
    this.textComponent = textComponent;
    this.format = format;
    this.placeholder = textComponent instanceof JFormattedTextField ?
            Character.toString(
                    (((MaskFormatter) ((JFormattedTextField)textComponent).getFormatter()).getPlaceholderCharacter()))
            : null;
    if (enabledState != null) {
      textComponent.setEnabled(enabledState.isActive());
      enabledState.evtStateChanged.addListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          textComponent.setEnabled(enabledState.isActive());
        }
      });
    }
    else if (linkType == LinkType.READ_ONLY)
      textComponent.setEditable(false);
    refreshUI();
    textComponent.getDocument().addDocumentListener(this);
  }

  /** {@inheritDoc} */
  public void insertUpdate(final DocumentEvent e) {
    refreshProperty();
  }

  /** {@inheritDoc} */
  public void removeUpdate(final DocumentEvent e) {
    refreshProperty();
  }

  /** {@inheritDoc} */
  public void changedUpdate(final DocumentEvent e) {}

  protected String getPropertyValueAsString(final Object propertyValue) {
    if (propertyValue == null)
      return null;

    return format == null ? propertyValue.toString() : format.format(propertyValue);
  }

  /** {@inheritDoc} */
  protected void setUiPropertyValue(final Object propertyValue) {
    textComponent.setText(getPropertyValueAsString(propertyValue));
  }

  /**
   * This is a very strict implementation, formatted values are considered
   * invalid (null) until all placeholder characters have been replaced
   * @return the value, if a formatter is present, the formatted value is returned
   */
  protected Object getUiPropertyValue() {
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
      return textComponent.getDocument().getText(0, textComponent.getDocument().getLength());
    }
    catch (BadLocationException e) {
      throw new RuntimeException(e);
    }
  }
}
