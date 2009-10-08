package org.jminor.framework.client.ui.property;

import org.jminor.common.ui.control.LinkType;
import org.jminor.framework.client.model.EntityEditModel;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.Property;

import javax.swing.JFormattedTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.MaskFormatter;
import java.awt.Color;
import java.text.Format;
import java.text.ParseException;

public class FormattedTextPropertyLink extends TextPropertyLink {

  private final Format format;
  private final MaskFormatter formatter;

  /**
   * Instantiates a new FormattedTextPropertyLink
   * @param textComponent the text component to link
   * @param editModel the EntityEditModel instance
   * @param property the property to link
   * @param immediateUpdate if true then the underlying model value is updated on each keystroke,
   * otherwise it is updated on actionPerformed or focusLost
   * @param linkType the link type
   * @param format the format
   */
  public FormattedTextPropertyLink(final JFormattedTextField textComponent, final EntityEditModel editModel,
                                      final Property property, final boolean immediateUpdate, final LinkType linkType,
                                      final Format format) {
    super(textComponent, editModel, property, immediateUpdate, linkType);
    this.format = format;
    this.formatter = (MaskFormatter) textComponent.getFormatter();
    addColorUpdating(textComponent, editModel, property, textComponent.getText());
    updateUI();
  }

  /**
   * @return the format, if any
   */
  public Format getFormat() {
    return format;
  }

  /**
   * Returns a valid property value based on the given text
   * @param text the text to use to create a valid value
   * @return a valid value
   */
  @Override
  protected Object valueFromText(final String text) {
    if (text == null)
      return null;

    try {
      return format == null ? text : format.parseObject(text);
    }
    catch (ParseException nf) {
      return null;
    }
  }

  @Override
  protected String getValueAsString(final Object value) {
    if (Entity.isValueNull(getProperty().getPropertyType(), value))
      return null;

    return format == null ? value.toString() : format.format(value);
  }

  @Override
  protected String getText() {
    final String value = super.getText();
    if (value == null)
      return null;
    try {
      return (String) formatter.stringToValue((String) value);
    }
    catch (ParseException e) {
      return null;
    }
  }

  private void addColorUpdating(final JFormattedTextField textComponent, final EntityEditModel editModel,
                                final Property property, final String maskString) {
    final Color defaultTextFieldBackground = textComponent.getBackground();
    textComponent.getDocument().addDocumentListener(new DocumentListener() {
      public void changedUpdate(final DocumentEvent e) {
        updateFieldColor(textComponent, maskString, defaultTextFieldBackground);
      }
      public void insertUpdate(final DocumentEvent e) {
        updateFieldColor(textComponent, maskString, defaultTextFieldBackground);
      }
      public void removeUpdate(final DocumentEvent e) {
        updateFieldColor(textComponent, maskString, defaultTextFieldBackground);
      }
    });
    editModel.getPropertyChangeEvent(property).addListener(new Property.Listener() {
      @Override
      protected void propertyChanged(final Property.Event e) {
        updateFieldColor(textComponent, maskString, defaultTextFieldBackground);
      }
    });
  }

  private void updateFieldColor(final JFormattedTextField textComponent, final String maskString, final Color defaultTextFieldBackground) {
    final boolean validInput = !isModelPropertyValueNull() || textComponent.getText().equals(maskString);
    textComponent.setBackground(validInput ? defaultTextFieldBackground : Color.LIGHT_GRAY);
  }
}
