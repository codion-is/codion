/**
 * Common classes used throughout.<br>
 * <br>
 * Configuration values:<br>
 * {@link is.codion.common.Text#DEFAULT_COLLATOR_LANGUAGE}<br>
 * @uses is.codion.common.LoggerProxy
 * @uses is.codion.common.CredentialsProvider
 */
module is.codion.common.core {
  exports is.codion.common;
  exports is.codion.common.event;
  exports is.codion.common.item;
  exports is.codion.common.i18n;
  exports is.codion.common.logging;
  exports is.codion.common.state;
  exports is.codion.common.user;
  exports is.codion.common.value;
  exports is.codion.common.version;

  uses is.codion.common.LoggerProxy;
  uses is.codion.common.CredentialsProvider;
}