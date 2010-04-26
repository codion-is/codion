/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.ui.valuemap;

import org.jminor.common.model.valuemap.ChangeValueMapEditModel;
import org.jminor.common.ui.control.LinkType;

import javax.swing.JFormattedTextField;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.util.Date;

/**
 * A class for linking a formatted text field to a ChangeValueMapEditModel date property value.
 */
public class DateValueLink extends FormattedValueLink {

  private final boolean isTimestamp;

  /**
   * Instantiates a new DateTextPropertyLink.
   * @param textField the text field to link
   * @param editModel the ChangeValueMapEditModel instance
   * @param key the key to link
   * @param linkType the link type
   * @param dateFormat the date format to use
   * @param isTimestamp true if the date being linked is a timestamp
   */
  public DateValueLink(final JFormattedTextField textField, final ChangeValueMapEditModel<String, Object> editModel,
                       final String key, final LinkType linkType, final DateFormat dateFormat, final boolean isTimestamp) {
    super(textField, editModel, key, dateFormat, true, linkType);
    this.isTimestamp = isTimestamp;
    if (dateFormat == null)
      throw new IllegalArgumentException("DateValueLink must have a date format");
  }

  /** {@inheritDoc} */
  @Override
  protected Object valueFromText(final String text) {
    final Date formatted = (Date) super.valueFromText(text);
    return formatted == null ? null : isTimestamp ? new Timestamp(formatted.getTime()) : new Date(formatted.getTime());
  }
}
