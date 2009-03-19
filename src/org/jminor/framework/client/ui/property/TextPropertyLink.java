/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.ui.property;

import org.jminor.common.model.State;
import org.jminor.common.ui.control.LinkType;
import org.jminor.framework.client.model.EntityModel;
import org.jminor.framework.model.EntityUtil;
import org.jminor.framework.model.Property;

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

public class TextPropertyLink extends AbstractEntityPropertyLink implements DocumentListener, Serializable {

  private final JTextComponent textComponent;
  /**
   * If true the model value is updated on each keystroke, otherwise it is updated on focus lost and action performed
   */
  private final boolean immediateUpdate;
  private final String placeholder;
  private final Format format;

  public TextPropertyLink(final EntityModel owner, final Property property, final JTextComponent textComponent,
                          final boolean immediateUpdate) {
    this(owner, property, textComponent, immediateUpdate, LinkType.READ_WRITE);
  }

  public TextPropertyLink(final EntityModel owner, final Property property, final JTextComponent textComponent,
                          final boolean immediateUpdate,
                          final LinkType linkType) {
    this(owner, property, textComponent, immediateUpdate, linkType, null);
  }

  public TextPropertyLink(final EntityModel owner, final Property property, final JTextComponent textComponent,
                          final boolean immediateUpdate,
                          final LinkType linkType, final Format format) {
    this(owner, property, textComponent, immediateUpdate, linkType, format, null);
  }

  protected TextPropertyLink(final EntityModel entityModel, final Property property, final JTextComponent textComponent,
                             final boolean immediateUpdate, final LinkType linkType,
                             final Format format, final State enableState) {
    super(entityModel, property, linkType, enableState);
    this.textComponent = textComponent;
    this.format = format;
    this.immediateUpdate = immediateUpdate;
    this.placeholder = textComponent instanceof JFormattedTextField ?
            Character.toString((((MaskFormatter) ((JFormattedTextField)
                    textComponent).getFormatter()).getPlaceholderCharacter())) : null;
    if (!this.immediateUpdate) {
      this.textComponent.addFocusListener(new FocusAdapter() {
        public void focusLost(FocusEvent e) {
          actionPerformed(new ActionEvent(e.getSource(), e.getID(), "focusLost"));
        }
      });
    }
    initConstants();
    if (linkType == LinkType.READ_ONLY)
      textComponent.setEnabled(false);

    updateUI();
    this.textComponent.getDocument().addDocumentListener(this);
  }

  public void checkValidValue() {}

  /**
   * @return Value for property 'format'.
   */
  public Format getFormat() {
    return format;
  }

  public static boolean isDigitString(final String str) {
    for (int i = 0; i < str.length(); i++)
      if (!Character.isDigit(str.charAt(i)))
        return false;

    return true;
  }

  /**
   * @return Value for property 'immediateUpdate'.
   */
  public boolean isImmediateUpdate() {
    return immediateUpdate;
  }

  /** {@inheritDoc} */
  public void insertUpdate(final DocumentEvent e) {
    if (isImmediateUpdate())
      refreshProperty();
  }

  /** {@inheritDoc} */
  public void removeUpdate(final DocumentEvent e) {
    if (isImmediateUpdate())
      refreshProperty();
  }

  /** {@inheritDoc} */
  public void changedUpdate(final DocumentEvent e) {
    if (isImmediateUpdate())
      refreshProperty();
  }

  /**
   * @return Value for property 'textComponent'.
   */
  protected final JTextComponent getTextComponent() {
    return this.textComponent;
  }

  protected void initConstants() {}

  protected Object valueFromText(final String text) {
    if (placeholder != null && text != null && text.contains(placeholder))
      return null;

    if (format != null)
      return getFormattedValue(text);

    return text;
  }

  protected Object getFormattedValue(final String text) {
    try {
      return text != null ? format.parseObject(text) : null;
    }
    catch (ParseException nf) {
      return null;
    }
  }

  /** {@inheritDoc} */
  protected void updateProperty() {
    setPropertyValue(valueFromText(getText()));
  }

  /** {@inheritDoc} */
  protected void updateUI() {
    textComponent.setText(getValueAsString(getPropertyValue()));
  }

  protected String getValueAsString(final Object value) {
    if (EntityUtil.isValueNull(getProperty().getPropertyType(), value))
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
