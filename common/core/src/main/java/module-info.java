/**
 * Common classes used throughout.<br>
 * <br>
 * Configuration values:<br>
 * {@link org.jminor.common.Text#DEFAULT_COLLATOR_LANGUAGE}<br>
 * @uses org.jminor.common.LoggerProxy
 * @uses org.jminor.common.CredentialsProvider
 */
module org.jminor.common.core {
  requires transitive org.slf4j;

  exports org.jminor.common;
  exports org.jminor.common.event;
  exports org.jminor.common.item;
  exports org.jminor.common.i18n;
  exports org.jminor.common.state;
  exports org.jminor.common.user;
  exports org.jminor.common.value;
  exports org.jminor.common.valuemap;
  exports org.jminor.common.version;

  uses org.jminor.common.LoggerProxy;
  uses org.jminor.common.CredentialsProvider;
}