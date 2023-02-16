/**
 * Common classes used throughout.<br>
 * <br>
 * Configuration values:<br>
 * {@link is.codion.common.Text#DEFAULT_COLLATOR_LANGUAGE}<br>
 * @uses is.codion.common.logging.LoggerProxy
 * @uses is.codion.common.credentials.CredentialsProvider
 */
module is.codion.common.core {
  exports is.codion.common;
  exports is.codion.common.credentials;
  exports is.codion.common.event;
  exports is.codion.common.formats;
  exports is.codion.common.item;
  exports is.codion.common.i18n;
  exports is.codion.common.logging;
  exports is.codion.common.properties;
  exports is.codion.common.proxy;
  exports is.codion.common.scheduler;
  exports is.codion.common.state;
  exports is.codion.common.user;
  exports is.codion.common.value;
  exports is.codion.common.version;

  uses is.codion.common.logging.LoggerProxy;
  uses is.codion.common.credentials.CredentialsProvider;
}