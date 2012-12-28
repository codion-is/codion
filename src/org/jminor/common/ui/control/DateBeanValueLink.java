/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.ui.control;

import org.jminor.common.model.Util;

import javax.swing.JFormattedTextField;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.util.Date;

/**
 * Binds a JFormattedField to a date based bean property.
 */
final class DateBeanValueLink extends FormattedTextBeanValueLink {

  private final boolean isTimestamp;

  /**
   * Instantiates a new DateBeanValueLink.
   * @param textComponent the text component to link with the value
   * @param modelValue the model value
   * @param linkType the link type
   * @param dateFormat the date format
   */
  DateBeanValueLink(final JFormattedTextField textComponent, final ModelValue modelValue,
                    final LinkType linkType, final DateFormat dateFormat, final boolean isTimestamp) {
    super(textComponent, modelValue, linkType, Util.rejectNullValue(dateFormat, "dateFormat"));
    this.isTimestamp = isTimestamp;
  }

  /**
   * Translates a Date value into a Timestamp if applicable
   * @param parsedValue the value to translate
   * @return a Timestamp if applicable
   */
  @Override
  protected Object translate(final Object parsedValue) {
    final Date formatted = (Date) parsedValue;
    return formatted == null ? null : isTimestamp ? new Timestamp(formatted.getTime()) : new Date(formatted.getTime());
  }
}
