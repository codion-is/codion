/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.ui.control;

import org.jminor.common.model.DocumentAdapter;
import org.jminor.common.model.EventObserver;
import org.jminor.common.model.Util;

import javax.swing.event.DocumentEvent;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import java.text.Format;

/**
 * Binds a JTextComponent to a text based bean property.
 */
public class TextBeanValueLink extends AbstractBeanValueLink {

  private final Document document;

  /**
   * The format to use when presenting values in the linked text field
   */
  private final Format format;

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
    this(textComponent, owner, propertyName, valueClass, valueChangeEvent, linkType, null);
  }

  /**
   * Instantiates a new TextBeanValueLink.
   * @param textComponent the text component to link with the value
   * @param owner the value owner
   * @param propertyName the property name
   * @param valueClass the value class
   * @param valueChangeEvent an EventObserver notified each time the value changes
   * @param linkType the link type
   * @param format the format to use when displaying the linked value,
   * null if no formatting should be performed
   */
  public TextBeanValueLink(final JTextComponent textComponent, final Object owner, final String propertyName,
                           final Class<?> valueClass, final EventObserver valueChangeEvent, final LinkType linkType,
                           final Format format) {
    super(owner, propertyName, valueClass, valueChangeEvent, linkType);
    this.document = textComponent.getDocument();
    this.format = format == null ? new Util.NullFormat() : format;
    if (linkType == LinkType.READ_ONLY) {
      textComponent.setEditable(false);
    }
    updateUI();
    this.document.addDocumentListener(new DocumentAdapter() {
      /** {@inheritDoc} */
      @Override
      public final void contentsChanged(final DocumentEvent e) {
        updateModel();
      }
    });
  }

  /**
   * @return the value from the UI component
   */
  @Override
  protected final Object getUIValue() {
    return getValueFromText(getText());
  }

  /** {@inheritDoc} */
  @Override
  protected final void setUIValue(final Object value) {
    try {
      synchronized (document) {
        document.remove(0, document.getLength());
        if (value != null) {
          document.insertString(0, getValueAsText(value), null);
        }
      }
      handleSetUIValue(value);
    }
    catch (BadLocationException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * @return the text from the linked text component
   */
  protected final String getText() {
    try {
      return translate(document.getText(0, document.getLength()));
    }
    catch (BadLocationException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Returns a String representation of the given value object, using the format,
   * an empty string is returned in case of a null value
   * @param value the value to return as String
   * @return a formatted String representation of the given value, an empty string if the value is null
   */
  protected final String getValueAsText(final Object value) {
    return value == null ? "" : format.format(value);
  }

  /**
   * @return the format object being used by this value link
   */
  protected final Format getFormat() {
    return format;
  }

  /**
   * Provides a hook into the value setting mechanism.
   * @param text the value returned from the UI component
   * @return the translated value
   */
  protected String translate(final String text) {
    return text;
  }

  /**
   * Returns a property value based on the given text, if the text can not
   * be parsed into a valid value, null is returned
   * @param text the text from which to parse a value
   * @return a value from the given text, or null if the parsing did not yield a valid value
   */
  protected Object getValueFromText(final String text) {
    if (text != null && text.isEmpty()) {
      return null;
    }

    return text;
  }

  /**
   * Called after the values has been set in the UI
   * @param value the value
   */
  protected void handleSetUIValue(final Object value) {}
}
