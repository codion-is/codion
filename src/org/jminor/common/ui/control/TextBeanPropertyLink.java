/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.ui.control;

import org.jminor.common.model.Event;

import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;

public class TextBeanPropertyLink extends BeanPropertyLink implements DocumentListener {

  private final Document document;

  public TextBeanPropertyLink(final JTextComponent textComponent, final Object owner, final String propertyName,
                              final Class<?> valueClass, final Event propertyChangeEvent) {
    this(textComponent, owner, propertyName, valueClass, propertyChangeEvent, LinkType.READ_WRITE);
  }

  public TextBeanPropertyLink(final JTextComponent textComponent, final Object owner, final String propertyName,
                              final Class<?> valueClass, final Event propertyChangeEvent, final LinkType linkType) {
    super(owner, propertyName, valueClass, propertyChangeEvent, linkType);
    this.document = textComponent.getDocument();
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

  /** {@inheritDoc} */
  @Override
  protected void setUIPropertyValue(final Object propertyValue) {
    try {
      document.remove(0, document.getLength());
      if (propertyValue != null)
        document.insertString(0, getPropertyValueAsString(propertyValue), null);
    }
    catch (BadLocationException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * @return the value from the UI component
   */
  @Override
  protected Object getUIPropertyValue() {
    return getText();
  }

  protected String getPropertyValueAsString(final Object propertyValue) {
    return propertyValue != null ? propertyValue.toString() : null;
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
