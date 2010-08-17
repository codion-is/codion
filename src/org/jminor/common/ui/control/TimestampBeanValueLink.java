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
 * Binds a JFormattedField to a timestamp based bean property.
 */
public final class TimestampBeanValueLink extends FormattedTextBeanValueLink {

  /**
   * Instantiates a new TimesatmpBeanValueLink.
   * @param textComponent the text component to link with the value
   * @param owner the value owner
   * @param propertyName the property name
   * @param valueChangeEvent an EventObserver notified each time the value changes
   * @param linkType the link type
   * @param format the date format
   */
  public TimestampBeanValueLink(final JFormattedTextField textComponent, final Object owner,
                                final String propertyName, final EventObserver valueChangeEvent,
                                final LinkType linkType, final DateFormat format) {
    super(textComponent, owner, propertyName, Timestamp.class, valueChangeEvent, linkType, format);
  }

  /**
   * Translate the given Date value to a Timestamp value.
   * @param parsedValue the Date parsed from the text component
   * @return a Timestamp based on the Date received
   */
  @Override
  protected Object translate(final Object parsedValue) {
    final Date date = (Date) parsedValue;
    return date == null ? null : new Timestamp(date.getTime());
  }
}
