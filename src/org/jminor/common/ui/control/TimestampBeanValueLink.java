/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.ui.control;

import org.jminor.common.model.Event;

import javax.swing.JFormattedTextField;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.util.Date;

/**
 * User: Björn Darri
 * Date: 25.7.2010
 * Time: 18:05:19
 */
public final class TimestampBeanValueLink extends FormattedTextBeanValueLink {

  public TimestampBeanValueLink(final JFormattedTextField textComponent, final Object owner,
                                final String propertyName, final Event valueChangeEvent,
                                final LinkType linkType, final DateFormat format) {
    super(textComponent, owner, propertyName, Timestamp.class, valueChangeEvent, linkType, format);
  }

  @Override
  protected Object translate(final Object parsedValue) {
    final Date date = (Date) parsedValue;
    return date == null ? null : new Timestamp(date.getTime());
  }
}
