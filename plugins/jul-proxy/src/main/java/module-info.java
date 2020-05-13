module dev.codion.plugin.log4j.proxy {
  requires java.logging;
  requires dev.codion.common.core;

  exports dev.codion.plugin.jul;

  provides dev.codion.common.LoggerProxy
          with dev.codion.plugin.jul.JulProxy;
}