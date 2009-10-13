package org.jminor.common.model.formats;

import org.jminor.framework.Configuration;

import java.text.SimpleDateFormat;

/**
 * A SimpleDateFormat based on Configuration.DEFAULT_DATE_FORMAT
 * @see Configuration#DEFAULT_DATE_FORMAT
 */
public class DefaultDateFormat extends SimpleDateFormat {

  public DefaultDateFormat() {
    super((String) Configuration.getValue(Configuration.DEFAULT_DATE_FORMAT));
  }
}
