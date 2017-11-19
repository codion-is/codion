module org.jminor.framework.plugins.log4j.proxy {
  requires log4j.core;
  requires log4j.api;
  requires org.jminor.common.core;

  exports org.jminor.framework.plugins.log4j;
}