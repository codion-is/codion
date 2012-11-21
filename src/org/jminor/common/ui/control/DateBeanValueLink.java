/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.ui.control;

import org.jminor.common.model.EventObserver;

import javax.swing.JFormattedTextField;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.util.Date;

/**
 * Binds a JFormattedField to a date based bean property.
 */
public final class DateBeanValueLink extends FormattedTextBeanValueLink {

  private final boolean isTimestamp;

  /**
   * Instantiates a new DateBeanValueLink.
   * @param textComponent the text component to link with the value
   * @param owner the value owner
   * @param propertyName the property name
   * @param valueChangeEvent an EventObserver notified each time the value changes
   * @param linkType the link type
   * @param format the date format
   */
  public DateBeanValueLink(final JFormattedTextField textComponent, final Object owner,
                           final String propertyName, final EventObserver valueChangeEvent,
                           final LinkType linkType, final DateFormat format, final boolean isTimestamp) {
    super(textComponent, owner, propertyName, isTimestamp ? Timestamp.class : Date.class, valueChangeEvent, linkType, format);
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
