module org.jminor.framework.plugins.logback.proxy {
  requires org.slf4j;
  requires ch.qos.logback.classic;
  requires org.jminor.common.core;

  exports org.jminor.framework.plugins.logback;

  provides org.jminor.common.LoggerProxy
          with org.jminor.framework.plugins.logback.LogbackProxy;
}