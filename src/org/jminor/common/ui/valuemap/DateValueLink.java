/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.ui.valuemap;

import org.jminor.common.model.Util;
import org.jminor.common.model.valuemap.ValueChangeMapEditModel;
import org.jminor.common.ui.control.LinkType;

import javax.swing.JFormattedTextField;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.util.Date;

/**
 * A class for linking a formatted text field to a ValueChangeMapEditModel date property value.
 */
public final class DateValueLink<K> extends FormattedValueLink<K> {

  private final boolean isTimestamp;

  /**
   * Instantiates a new DateValueLink.
   * @param textField the text field to link
   * @param editModel the ValueChangeMapEditModel instance
   * @param key the key to link
   * @param linkType the link type
   * @param dateFormat the date format to use
   * @param isTimestamp true if the date being linked is a timestamp
   */
  public DateValueLink(final JFormattedTextField textField, final ValueChangeMapEditModel<K, Object> editModel,
                       final K key, final LinkType linkType, final DateFormat dateFormat, final boolean isTimestamp) {
    super(textField, editModel, key, dateFormat, true, linkType);
    this.isTimestamp = isTimestamp;
    Util.rejectNullValue(dateFormat, "dateFormat");
  }

  /** {@inheritDoc} */
  @Override
  protected Object translate(final Object parsedValue) {
    final Date formatted = (Date) parsedValue;
    return formatted == null ? null : isTimestamp ? new Timestamp(formatted.getTime()) : new Date(formatted.getTime());
  }
}
