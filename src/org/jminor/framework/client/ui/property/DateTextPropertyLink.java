/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.ui.property;

import org.jminor.common.model.Event;
import org.jminor.common.model.PropertyChangeEvent;
import org.jminor.common.model.PropertyListener;
import org.jminor.common.model.State;
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

public class DateTextPropertyLink extends TextPropertyLink {

  private final String fieldMaskString;
  private final Event updateValidColor = new Event("DateTextPropertyLink.updateValidColor");

  public DateTextPropertyLink(final EntityModel entityModel, final Property property, final JFormattedTextField textField,
                              final LinkType linkType, final State enableState,
                              final DateFormat dateFormat, final String formatMaskString) {
    super(entityModel, property, textField, true, linkType, dateFormat, enableState);
    if (dateFormat == null)
      throw new IllegalArgumentException("DateTextProperty must hava a date format");

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
  protected Object getFormattedValue(final String text) {
    updateValidColor.fire();
    final Date formatted = (Date) super.getFormattedValue(text);
    return formatted == null ? null : new Timestamp(formatted.getTime());
  }

  private void updateFieldColor(final JFormattedTextField textField) {
    final boolean validInput = !isValueNull() || fieldContainsMaskOnly();
    textField.setBackground(validInput ? (new JTextField()).getBackground() : Color.LIGHT_GRAY);
  }

  private boolean fieldContainsMaskOnly() {
    return getTextComponent().getText().equals(fieldMaskString);
  }
}
