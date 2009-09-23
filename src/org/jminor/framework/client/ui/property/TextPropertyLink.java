/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.ui.property;

import org.jminor.common.ui.control.LinkType;
import org.jminor.framework.client.model.EntityEditModel;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.Property;

import javax.swing.JFormattedTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.JTextComponent;
import javax.swing.text.MaskFormatter;
import java.awt.event.ActionEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.io.Serializable;
import java.text.Format;
import java.text.ParseException;

/**
 * A class for linking a text component to a EntityModel text property value
 */
public class TextPropertyLink extends AbstractEntityPropertyLink implements DocumentListener, Serializable {

  private final JTextComponent textComponent;
  /**
   * If true the model value is updated on each keystroke, otherwise it is updated on focus lost and action performed
   */
  private final boolean immediateUpdate;
  private final String placeholder;
  private final Format format;

  /**
   * Instantiates a new TextPropertyLink
   * @param textComponent the text component to link
   * @param editModel the EntityEditModel instance
   * @param property the property to link
   * @param immediateUpdate if true then the underlying model value is updated on each keystroke,
   */
  public TextPropertyLink(final JTextComponent textComponent, final EntityEditModel editModel, final Property property,
                          final boolean immediateUpdate) {
    this(textComponent, editModel, property, immediateUpdate, LinkType.READ_WRITE);
  }

  /**
   * Instantiates a new TextPropertyLink
   * @param textComponent the text component to link
   * @param editModel the EntityEditModel instance
   * @param property the property to link
   * @param immediateUpdate if true then the underlying model value is updated on each keystroke,
   * otherwise it is updated on actionPerformed or focusLost
   * @param linkType the link type
   */
  public TextPropertyLink(final JTextComponent textComponent, final EntityEditModel editModel, final Property property,
                          final boolean immediateUpdate, final LinkType linkType) {
    this(textComponent, editModel, property, immediateUpdate, linkType, null);
  }

  /**
   * Instantiates a new TextPropertyLink
   * @param textComponent the text component to link
   * @param editModel the EntityEditModel instance
   * @param property the property to link
   * @param immediateUpdate if true then the underlying model value is updated on each keystroke,
   * otherwise it is updated on actionPerformed or focusLost
   * @param linkType the link type
   * @param format the format object to use when presenting the value in the text field
   */
  protected TextPropertyLink(final JTextComponent textComponent, final EntityEditModel editModel, final Property property,
                             final boolean immediateUpdate, final LinkType linkType, final Format format) {
    super(editModel, property, linkType);
    this.textComponent = textComponent;
    this.format = format;
    this.immediateUpdate = immediateUpdate;
    this.placeholder = textComponent instanceof JFormattedTextField ?
            Character.toString((((MaskFormatter) ((JFormattedTextField)
                    textComponent).getFormatter()).getPlaceholderCharacter())) : null;
    if (!this.immediateUpdate) {
      this.textComponent.addFocusListener(new FocusAdapter() {
        @Override
        public void focusLost(FocusEvent e) {
          actionPerformed(new ActionEvent(e.getSource(), e.getID(), "focusLost"));
        }
      });
    }
    if (linkType == LinkType.READ_ONLY)
      this.textComponent.setEnabled(false);

    updateUI();
    this.textComponent.getDocument().addDocumentListener(this);
  }

  /**
   * @return the format, if any
   */
  public Format getFormat() {
    return format;
  }

  /**
   * @return true if the underlying property should be updated on each keystroke
   */
  public boolean isImmediateUpdate() {
    return immediateUpdate;
  }

  /** {@inheritDoc} */
  public void insertUpdate(final DocumentEvent e) {
    if (isImmediateUpdate())
      updateModel();
  }

  /** {@inheritDoc} */
  public void removeUpdate(final DocumentEvent e) {
    if (isImmediateUpdate())
      updateModel();
  }

  /** {@inheritDoc} */
  public void changedUpdate(final DocumentEvent e) {
    if (isImmediateUpdate())
      updateModel();
  }

  /**
   * @return the linked text component
   */
  protected final JTextComponent getTextComponent() {
    return this.textComponent;
  }

  /**
   * Returns a valid property value based on the given text
   * @param text the text to use to create a valid value
   * @return a valid value
   */
  protected Object valueFromText(final String text) {
    if (placeholder != null && text != null && text.contains(placeholder))
      return null;

    if (format != null)
      return getParsedValue(text);

    return text;
  }

  /**
   * Parses the given text according to the format used by this TextPropertyLink
   * @param text the text to parse
   * @return an Object parsed from the given text
   */
  protected Object getParsedValue(final String text) {
    try {
      return text != null ? format.parseObject(text) : null;
    }
    catch (ParseException nf) {
      return null;
    }
  }

  /** {@inheritDoc} */
  @Override
  protected Object getUIPropertyValue() {
    return valueFromText(getText());
  }

  /** {@inheritDoc} */
  @Override
  protected void setUIPropertyValue(final Object propertyValue) {
    textComponent.setText(getValueAsString(propertyValue));
  }

  protected String getValueAsString(final Object value) {
    if (Entity.isValueNull(getProperty().getPropertyType(), value))
      return null;
    else
      return format == null ? value.toString() : format.format(value);
  }

  private String getText() {
    if (textComponent instanceof JFormattedTextField) {
      try {
        return (String) ((JFormattedTextField) textComponent).getFormatter().stringToValue(textComponent.getText());
      }
      catch (ParseException e) {
        return null;
      }
    }

    return textComponent.getText();
  }
}
