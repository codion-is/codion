/**
 * Common classes used throughout.<br>
 * <br>
 * Configuration values:<br>
 * {@link org.jminor.common.TextUtil#DEFAULT_COLLATOR_LANGUAGE}<br>
 * {@link org.jminor.common.LoggerProxy#LOGGER_PROXY_IMPLEMENTATION}
 * @uses org.jminor.common.LoggerProxy
 * @uses org.jminor.common.CredentialsProvider
 */
module org.jminor.common.core {
  requires transitive org.slf4j;

  exports org.jminor.common;
  exports org.jminor.common.event;
  exports org.jminor.common.i18n;
  exports org.jminor.common.state;
  exports org.jminor.common.value;

  uses org.jminor.common.LoggerProxy;
  uses org.jminor.common.CredentialsProvider;
}