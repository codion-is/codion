module org.jminor.plugin.log4j.proxy {
  requires java.logging;
  requires org.jminor.common.core;

  exports org.jminor.plugin.jul;

  provides org.jminor.common.LoggerProxy
          with org.jminor.plugin.jul.JulProxy;
}