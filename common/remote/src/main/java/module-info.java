module org.jminor.common.remote {
  requires slf4j.api;
  requires transitive java.rmi;
  requires transitive org.jminor.common.core;

  exports org.jminor.common.remote;
}