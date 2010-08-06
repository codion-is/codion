/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.ui.control;

import org.jminor.common.model.EventObserver;

import javax.swing.JFormattedTextField;
import java.text.DateFormat;
import java.util.Date;

/**
 * User: Björn Darri
 * Date: 25.7.2010
 * Time: 18:05:09
 */
public final class DateBeanValueLink extends FormattedTextBeanValueLink {

  public DateBeanValueLink(final JFormattedTextField textComponent, final Object owner,
                           final String propertyName, final EventObserver valueChangeEvent,
                           final LinkType linkType, final DateFormat format) {
    super(textComponent, owner, propertyName, Date.class, valueChangeEvent, linkType, format);
  }
}
