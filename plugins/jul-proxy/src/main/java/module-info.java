module is.codion.plugin.log4j.proxy {
  requires java.logging;
  requires is.codion.common.core;

  exports is.codion.plugin.jul;

  provides is.codion.common.LoggerProxy
          with is.codion.plugin.jul.JulProxy;
}