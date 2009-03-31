/*
 * Copyright (c) 2008, Bj�rn Darri Sigur�sson. All Rights Reserved.
 */
package org.jminor.framework.client.ui.property;

import org.jminor.common.model.Event;
import org.jminor.common.model.PropertyChangeEvent;
import org.jminor.common.model.PropertyListener;
import org.jminor.common.ui.control.LinkType;
import org.jminor.framework.client.model.EntityModel;
import org.jminor.framework.model.Property;

import javax.swing.JFormattedTextField;
import javax.swing.JTextField;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.util.Date;

/**
 * A class for linking a formatted text field to a EntityModel date property value
 */
public class DateTextPropertyLink extends TextPropertyLink {

  private final String fieldMaskString;
  private final Event updateValidColor = new Event("DateTextPropertyLink.updateValidColor");

  /**
   * Instantiates a new DateTextPropertyLink
   * @param entityModel the EntityModel instance
   * @param property the property to link
   * @param textField the text field to link
   * @param linkType the link type
   * @param dateFormat the date format to use
   * @param formatMaskString the date format mask string used by the formatted text field
   */
  public DateTextPropertyLink(final EntityModel entityModel, final Property property, final JFormattedTextField textField,
                              final LinkType linkType, final DateFormat dateFormat, final String formatMaskString) {
    super(entityModel, property, textField, true, linkType, dateFormat);
    if (dateFormat == null)
      throw new IllegalArgumentException("DateTextPropertyLink must hava a date format");

    this.fieldMaskString = formatMaskString.replaceAll("#","_");
    entityModel.getPropertyChangeEvent(property).addListener(new PropertyListener() {
      protected void propertyChanged(PropertyChangeEvent e) {
        updateFieldColor(textField);
      }
    });
    updateValidColor.addListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        updateFieldColor(textField);
      }
    });
    updateUI();
  }

  /** {@inheritDoc} */
  protected Object getParsedValue(final String text) {
    updateValidColor.fire();
    final Date formatted = (Date) super.getParsedValue(text);
    return formatted == null ? null : new Timestamp(formatted.getTime());
  }

  private void updateFieldColor(final JFormattedTextField textField) {
    final boolean validInput = !isModelPropertyValueNull() || fieldContainsMaskOnly();
    textField.setBackground(validInput ? (new JTextField()).getBackground() : Color.LIGHT_GRAY);
  }

  private boolean fieldContainsMaskOnly() {
    return getTextComponent().getText().equals(fieldMaskString);
  }
}
