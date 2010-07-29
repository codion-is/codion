/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.ui.control;

import org.jminor.common.model.EventObserver;

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
                           final Class<?> valueClass, final EventObserver valueChangeEvent) {
    this(textComponent, owner, propertyName, valueClass, valueChangeEvent, LinkType.READ_WRITE);
  }

  public TextBeanValueLink(final JTextComponent textComponent, final Object owner, final String propertyName,
                           final Class<?> valueClass, final EventObserver valueChangeEvent, final LinkType linkType) {
    super(owner, propertyName, valueClass, valueChangeEvent, linkType);
    this.document = textComponent.getDocument();
    if (linkType == LinkType.READ_ONLY) {
      textComponent.setEditable(false);
    }
    this.document.addDocumentListener(this);
  }

  public final void insertUpdate(final DocumentEvent e) {
    updateModel();
  }

  public final void removeUpdate(final DocumentEvent e) {
    updateModel();
  }

  public final void changedUpdate(final DocumentEvent e) {}

  /**
   * @return the value from the UI component
   */
  @Override
  protected Object getUIValue() {
    final String text = getText();

    return text == null || text.length() == 0 ? null : text;
  }

  protected String getValueAsString(final Object value) {
    return value != null ? value.toString() : null;
  }

  @Override
  protected final void setUIValue(final Object value) {
    try {
      synchronized (document) {
        document.remove(0, document.getLength());
        if (value != null) {
          document.insertString(0, getValueAsString(value), null);
        }
      }
      handleSetUIValue(value);
    }
    catch (BadLocationException e) {
      throw new RuntimeException(e);
    }
  }

  protected final String getText() {
    try {
      synchronized (document) {
        return document.getText(0, document.getLength());
      }
    }
    catch (BadLocationException e) {
      throw new RuntimeException(e);
    }
  }

  protected void handleSetUIValue(final Object value) {}
}
