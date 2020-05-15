module is.codion.plugin.log4j.proxy {
  requires org.apache.logging.log4j.core;
  requires org.apache.logging.log4j;
  requires is.codion.common.core;

  exports is.codion.plugin.log4j;

  provides is.codion.common.LoggerProxy
          with is.codion.plugin.log4j.Log4jProxy;
}