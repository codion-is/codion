module org.jminor.plugin.logback.proxy {
  requires org.slf4j;
  requires ch.qos.logback.classic;
  requires org.jminor.common.core;

  exports org.jminor.plugin.logback;

  provides org.jminor.common.LoggerProxy
          with org.jminor.plugin.logback.LogbackProxy;
}