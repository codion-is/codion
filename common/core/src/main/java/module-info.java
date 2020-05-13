/**
 * Common classes used throughout.<br>
 * <br>
 * Configuration values:<br>
 * {@link dev.codion.common.Text#DEFAULT_COLLATOR_LANGUAGE}<br>
 * @uses dev.codion.common.LoggerProxy
 * @uses dev.codion.common.CredentialsProvider
 */
module dev.codion.common.core {
  exports dev.codion.common;
  exports dev.codion.common.event;
  exports dev.codion.common.item;
  exports dev.codion.common.i18n;
  exports dev.codion.common.state;
  exports dev.codion.common.user;
  exports dev.codion.common.value;
  exports dev.codion.common.valuemap;
  exports dev.codion.common.version;

  uses dev.codion.common.LoggerProxy;
  uses dev.codion.common.CredentialsProvider;
}