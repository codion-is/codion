module org.jminor.framework.db.remote {
  requires slf4j.api;
  requires transitive org.jminor.common.remote;
  requires transitive org.jminor.framework.db.core;

  exports org.jminor.framework.db.remote;

  provides org.jminor.framework.db.EntityConnectionProvider
          with org.jminor.framework.db.remote.RemoteEntityConnectionProvider;
}