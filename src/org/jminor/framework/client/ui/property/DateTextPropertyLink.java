/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.ui.property;

import org.jminor.common.ui.control.LinkType;
import org.jminor.framework.client.model.EntityEditModel;
import org.jminor.framework.domain.Property;

import javax.swing.JFormattedTextField;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.util.Date;

/**
 * A class for linking a formatted text field to a EntityModel date property value
 */
public class DateTextPropertyLink extends FormattedTextPropertyLink {

  /**
   * Instantiates a new DateTextPropertyLink
   * @param textField the text field to link
   * @param editModel the EntityEditModel instance
   * @param property the property to link
   * @param linkType the link type
   * @param dateFormat the date format to use
   */
  public DateTextPropertyLink(final JFormattedTextField textField, final EntityEditModel editModel, final Property property,
                              final LinkType linkType, final DateFormat dateFormat) {
    super(textField, editModel, property, true, linkType, dateFormat);
    if (dateFormat == null)
      throw new IllegalArgumentException("DateTextPropertyLink must hava a date format");
  }

  /** {@inheritDoc} */
  @Override
  protected Object valueFromText(final String text) {
    final Date formatted = (Date) super.valueFromText(text);
    return formatted == null ? null : new Timestamp(formatted.getTime());
  }
}
