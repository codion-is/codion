/**
 * Common classes used throughout.
 * @uses org.jminor.common.LoggerProxy
 * @uses org.jminor.common.CredentialsProvider
 */
module org.jminor.common.core {
  requires org.slf4j;

  exports org.jminor.common;
  exports org.jminor.common.i18n;

  uses org.jminor.common.LoggerProxy;
  uses org.jminor.common.CredentialsProvider;
}