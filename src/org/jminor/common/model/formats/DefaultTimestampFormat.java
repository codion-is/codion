package org.jminor.common.model.formats;

import org.jminor.framework.Configuration;

import java.text.SimpleDateFormat;

/**
 * A SimpleDateFormat based on Configuration.DEFAULT_TIMESTAMP_FORMAT
 * @see Configuration#DEFAULT_TIMESTAMP_FORMAT
 */
public class DefaultTimestampFormat extends SimpleDateFormat {

  public DefaultTimestampFormat() {
    super((String) Configuration.getValue(Configuration.DEFAULT_TIMESTAMP_FORMAT));
  }
}
