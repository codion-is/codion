module dev.codion.plugin.log4j.proxy {
  requires org.apache.logging.log4j.core;
  requires org.apache.logging.log4j;
  requires dev.codion.common.core;

  exports dev.codion.plugin.log4j;

  provides dev.codion.common.LoggerProxy
          with dev.codion.plugin.log4j.Log4jProxy;
}