/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.ui.control;

import org.jminor.common.model.Event;

import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;

/**
 * Binds a JTextComponent to a text based bean property.
 */
public class TextBeanValueLink extends AbstractBeanValueLink implements DocumentListener {

  private final Document document;

  public TextBeanValueLink(final JTextComponent textComponent, final Object owner, final String propertyName,
                           final Class<?> valueClass, final Event valueChangeEvent) {
    this(textComponent, owner, propertyName, valueClass, valueChangeEvent, LinkType.READ_WRITE);
  }

  public TextBeanValueLink(final JTextComponent textComponent, final Object owner, final String propertyName,
                           final Class<?> valueClass, final Event valueChangeEvent, final LinkType linkType) {
    super(owner, propertyName, valueClass, valueChangeEvent, linkType);
    this.document = textComponent.getDocument();
    if (linkType == LinkType.READ_ONLY)
      textComponent.setEditable(false);
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
  protected void setUIValue(final Object propertyValue) {
    try {
      synchronized (document) {
        document.remove(0, document.getLength());
        if (propertyValue != null)
          document.insertString(0, getValueAsString(propertyValue), null);
      }
    }
    catch (BadLocationException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * @return the value from the UI component
   */
  @Override
  protected Object getUIValue() {
    return getText();
  }

  protected String getValueAsString(final Object value) {
    return value != null ? value.toString() : null;
  }

  protected String getText() {
    try {
      synchronized (document) {
        return document.getText(0, document.getLength());
      }
    }
    catch (BadLocationException e) {
      throw new RuntimeException(e);
    }
  }
}
