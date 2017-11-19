module org.jminor.framework.plugins.logback.proxy {
  requires logback.classic;
  requires slf4j.api;
  requires org.jminor.common.core;

  exports org.jminor.framework.plugins.logback;
}