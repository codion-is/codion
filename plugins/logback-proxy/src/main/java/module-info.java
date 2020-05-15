module is.codion.plugin.logback.proxy {
  requires org.slf4j;
  requires ch.qos.logback.classic;
  requires is.codion.common.core;

  exports is.codion.plugin.logback;

  provides is.codion.common.LoggerProxy
          with is.codion.plugin.logback.LogbackProxy;
}