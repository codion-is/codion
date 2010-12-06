/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.ui.control;

import org.jminor.common.model.EventObserver;
import org.jminor.common.model.Util;

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

  /**
   * Instantiates a new TextBeanValueLink, with String as value class.
   * @param textComponent the text component to link with the value
   * @param owner the value owner
   * @param propertyName the property name
   * @param valueChangeEvent an EventObserver notified each time the value changes
   */
  public TextBeanValueLink(final JTextComponent textComponent, final Object owner, final String propertyName,
                           final EventObserver valueChangeEvent) {
    this(textComponent, owner, propertyName, String.class, valueChangeEvent, LinkType.READ_WRITE);
  }

  /**
   * Instantiates a new TextBeanValueLink.
   * @param textComponent the text component to link with the value
   * @param owner the value owner
   * @param propertyName the property name
   * @param valueClass the value class
   * @param valueChangeEvent an EventObserver notified each time the value changes
   */
  public TextBeanValueLink(final JTextComponent textComponent, final Object owner, final String propertyName,
                           final Class<?> valueClass, final EventObserver valueChangeEvent) {
    this(textComponent, owner, propertyName, valueClass, valueChangeEvent, LinkType.READ_WRITE);
  }

  /**
   * Instantiates a new TextBeanValueLink.
   * @param textComponent the text component to link with the value
   * @param owner the value owner
   * @param propertyName the property name
   * @param valueClass the value class
   * @param valueChangeEvent an EventObserver notified each time the value changes
   * @param linkType the link type
   */
  public TextBeanValueLink(final JTextComponent textComponent, final Object owner, final String propertyName,
                           final Class<?> valueClass, final EventObserver valueChangeEvent, final LinkType linkType) {
    super(owner, propertyName, valueClass, valueChangeEvent, linkType);
    this.document = textComponent.getDocument();
    if (linkType == LinkType.READ_ONLY) {
      textComponent.setEditable(false);
    }
    this.document.addDocumentListener(this);
  }

  /** {@inheritDoc} */
  public final void insertUpdate(final DocumentEvent e) {
    updateModel();
  }

  /** {@inheritDoc} */
  public final void removeUpdate(final DocumentEvent e) {
    updateModel();
  }

  /** {@inheritDoc} */
  public final void changedUpdate(final DocumentEvent e) {}

  /**
   * @return the value from the UI component
   */
  @Override
  protected Object getUIValue() {
    final String text = getText();

    return Util.nullOrEmpty(text) ? null : text;
  }

  /**
   * Returns a String representation of the given value, used when setting the value in the UI.
   * This default implementation simply returns value.toString() or null if the value is null.
   * Override to provide specific (such as formatted) string representations of the model value
   * when setting the value in the UI.
   * @param value the value
   * @return the value as a string
   */
  protected String getValueAsString(final Object value) {
    return value != null ? value.toString() : null;
  }

  /** {@inheritDoc} */
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

  /**
   * @return the from the input component
   */
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

  /**
   * Called after the values has been set in the UI
   * @param value the value
   */
  protected void handleSetUIValue(final Object value) {}
}
