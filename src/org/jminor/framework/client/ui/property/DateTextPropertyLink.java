/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.ui.property;

import org.jminor.common.ui.control.LinkType;
import org.jminor.framework.client.model.EntityEditModel;
import org.jminor.framework.domain.Property;

import javax.swing.JFormattedTextField;
import java.awt.Color;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.util.Date;

/**
 * A class for linking a formatted text field to a EntityModel date property value
 */
public class DateTextPropertyLink extends TextPropertyLink {

  private final String fieldMaskString;
  private final Color defaultTextFieldBackground;

  /**
   * Instantiates a new DateTextPropertyLink
   * @param textField the text field to link
   * @param editModel the EntityEditModel instance
   * @param property the property to link
   * @param linkType the link type
   * @param dateFormat the date format to use
   * @param formatMaskString the date format mask string used by the formatted text field
   */
  public DateTextPropertyLink(final JFormattedTextField textField, final EntityEditModel editModel, final Property property,
                              final LinkType linkType, final DateFormat dateFormat, final String formatMaskString) {
    super(textField, editModel, property, true, linkType, dateFormat);
    if (dateFormat == null)
      throw new IllegalArgumentException("DateTextPropertyLink must hava a date format");

    this.fieldMaskString = formatMaskString.replaceAll("#","_");
    this.defaultTextFieldBackground = textField.getBackground();
    editModel.getPropertyChangeEvent(property).addListener(new Property.Listener() {
      @Override
      protected void propertyChanged(Property.Event e) {
        updateFieldColor();
      }
    });
    updateUI();
  }

  /** {@inheritDoc} */
  @Override
  protected Object getParsedValue(final String text) {
    updateFieldColor();
    final Date formatted = (Date) super.getParsedValue(text);
    return formatted == null ? null : new Timestamp(formatted.getTime());
  }

  private void updateFieldColor() {
    final boolean validInput = !isModelPropertyValueNull() || fieldContainsMaskOnly();
    getTextComponent().setBackground(validInput ? defaultTextFieldBackground : Color.LIGHT_GRAY);
  }

  private boolean fieldContainsMaskOnly() {
    return getTextComponent().getText().equals(fieldMaskString);
  }
}
