module is.codion.plugin.jul.proxy {
  requires java.logging;
  requires is.codion.common.core;

  exports is.codion.plugin.jul;

  provides is.codion.common.logging.LoggerProxy
          with is.codion.plugin.jul.JulProxy;
}