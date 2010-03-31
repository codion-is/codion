/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
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
 * A class for linking a formatted text field to a EntityEditModel date property value
 */
public class DatePropertyLink extends FormattedPropertyLink {

  /**
   * Instantiates a new DateTextPropertyLink
   * @param textField the text field to link
   * @param editModel the EntityEditModel instance
   * @param property the property to link
   * @param linkType the link type
   * @param dateFormat the date format to use
   */
  public DatePropertyLink(final JFormattedTextField textField, final EntityEditModel editModel, final Property property,
                          final LinkType linkType, final DateFormat dateFormat) {
    super(textField, editModel, property, dateFormat, true, linkType);
    if (dateFormat == null)
      throw new IllegalArgumentException("DatePropertyLink must have a date format");
  }

  /** {@inheritDoc} */
  @Override
  protected Object valueFromText(final String text) {
    final Date formatted = (Date) super.valueFromText(text);
    return formatted == null ? null : new Timestamp(formatted.getTime());
  }
}
