module org.jminor.framework.plugins.log4j.proxy {
  requires org.apache.logging.log4j.core;
  requires org.apache.logging.log4j;
  requires org.jminor.common.core;

  exports org.jminor.framework.plugins.log4j;

  provides org.jminor.common.LoggerProxy
          with org.jminor.framework.plugins.log4j.Log4jProxy;
}