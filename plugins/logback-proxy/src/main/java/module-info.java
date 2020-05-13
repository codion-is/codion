module dev.codion.plugin.logback.proxy {
  requires org.slf4j;
  requires ch.qos.logback.classic;
  requires dev.codion.common.core;

  exports dev.codion.plugin.logback;

  provides dev.codion.common.LoggerProxy
          with dev.codion.plugin.logback.LogbackProxy;
}