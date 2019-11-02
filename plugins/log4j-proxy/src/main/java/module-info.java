module org.jminor.plugin.log4j.proxy {
  requires org.apache.logging.log4j.core;
  requires org.apache.logging.log4j;
  requires org.jminor.common.core;

  exports org.jminor.plugin.log4j;

  provides org.jminor.common.LoggerProxy
          with org.jminor.plugin.log4j.Log4jProxy;
}