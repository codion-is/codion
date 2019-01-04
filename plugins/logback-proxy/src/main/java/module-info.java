module org.jminor.framework.plugins.logback.proxy {
  requires slf4j.api;
  requires logback.classic;
  requires org.jminor.common.core;

  exports org.jminor.framework.plugins.logback;
}