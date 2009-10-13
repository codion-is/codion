/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.ui.property;

import org.jminor.common.ui.control.LinkType;
import org.jminor.framework.Configuration;
import org.jminor.framework.client.model.EntityEditModel;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.Property;

import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;

/**
 * A class for linking a text component to a EntityModel text property value
 */
public class TextPropertyLink extends AbstractEntityPropertyLink implements DocumentListener {

  private final Document document;
  /**
   * If true the model value is updated on each keystroke, otherwise it is updated on focus lost and action performed
   */
  private final boolean immediateUpdate;

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
    super(editModel, property, linkType);
    this.document = textComponent.getDocument();
    this.immediateUpdate = immediateUpdate;
    if (!this.immediateUpdate) {
      textComponent.addFocusListener(new FocusAdapter() {
        @Override
        public void focusLost(FocusEvent e) {
          updateModel();
        }
      });
    }
    if (linkType == LinkType.READ_ONLY)
      textComponent.setEnabled(false);
    addValidator(textComponent, editModel);
    updateUI();
    this.document.addDocumentListener(this);
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
  public void changedUpdate(final DocumentEvent e) {}

  /** {@inheritDoc} */
  @Override
  protected Object getUIPropertyValue() {
    return valueFromText(getText());
  }

  /** {@inheritDoc} */
  @Override
  protected void setUIPropertyValue(final Object propertyValue) {
    try {
      document.remove(0, document.getLength());
      if (propertyValue != null)
        document.insertString(0, getValueAsString(propertyValue), null);
    }
    catch (BadLocationException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * @return the text from the linked text component
   */
  protected String getText() {
    try {
      return document.getText(0, document.getLength());
    }
    catch (BadLocationException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Returns a valid property value based on the given text
   * @param text the text to use to create a valid value
   * @return a valid value
   */
  protected Object valueFromText(final String text) {
    return text;
  }

  /**
   * Returns a String version of the given value
   * @param value the value to return as String
   * @return a String representation of the given value
   */
  protected String getValueAsString(final Object value) {
    return Entity.isValueNull(getProperty().getPropertyType(), value) ? null : value.toString();
  }

  /**
   * Adds validation functionality to the given text componect, which is responsible for coloring the
   * component according to the validity of the value of the linked property and providing a
   * validation message via the components tooltip
   * @param textComponent the text component
   * @param editModel the underlying edit model
   * @see Configuration#INVALID_VALUE_BACKGROUND_COLOR
   * @see EntityEditModel#isValid(org.jminor.framework.domain.Property, Object)
   */
  protected void addValidator(final JTextComponent textComponent, final EntityEditModel editModel) {
    final Color validBackgroundColor = textComponent.getBackground();
    final Color invalidBackgroundColor = (Color) Configuration.getValue(Configuration.INVALID_VALUE_BACKGROUND_COLOR);
    final String defaultTooltTip = textComponent.getToolTipText();
    updateValidityInfo(textComponent, editModel, validBackgroundColor, invalidBackgroundColor, defaultTooltTip);
    editModel.getPropertyChangeEvent(getProperty()).addListener(new ActionListener() {
      public void actionPerformed(final ActionEvent e) {
        updateValidityInfo(textComponent, editModel, validBackgroundColor, invalidBackgroundColor, defaultTooltTip);
      }
    });
  }

  private void updateValidityInfo(final JTextComponent textComponent, final EntityEditModel editModel,
                                  final Color validBackgroundColor, final Color invalidBackgroundColor,
                                  final String defaultToolTip) {
    final String validationMessage = getValidationMessage(editModel);
    textComponent.setBackground(validationMessage == null ? validBackgroundColor : invalidBackgroundColor);
    textComponent.setToolTipText(validationMessage == null ? defaultToolTip :
            (defaultToolTip != null && defaultToolTip.length() > 0 ? defaultToolTip + ": " : "") + validationMessage);
  }
}
