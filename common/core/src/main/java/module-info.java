module org.jminor.common.core {
  requires slf4j.api;

  exports org.jminor.common;
  exports org.jminor.common.i18n;

  uses org.jminor.common.LoggerProxy;
  uses org.jminor.common.CredentialsProvider;
}