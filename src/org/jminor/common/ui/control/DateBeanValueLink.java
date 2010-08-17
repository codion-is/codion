/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.ui.control;

import org.jminor.common.model.EventObserver;

import javax.swing.JFormattedTextField;
import java.text.DateFormat;
import java.util.Date;

/**
 * Binds a JFormattedField to a date based bean property.
 */
public final class DateBeanValueLink extends FormattedTextBeanValueLink {

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
                           final LinkType linkType, final DateFormat format) {
    super(textComponent, owner, propertyName, Date.class, valueChangeEvent, linkType, format);
  }
}
