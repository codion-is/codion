module org.jminor.framework.db.local {
  requires slf4j.api;
  requires transitive org.jminor.framework.db.core;

  exports org.jminor.framework.db.local;

  provides org.jminor.framework.db.EntityConnectionProvider
          with org.jminor.framework.db.local.LocalEntityConnectionProvider;
}