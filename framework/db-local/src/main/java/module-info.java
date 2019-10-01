/**
 * Framework database connection classes for local JDBC connections.
 * @provides org.jminor.framework.db.EntityConnectionProvider
 */
module org.jminor.framework.db.local {
  requires transitive org.jminor.framework.db.core;

  exports org.jminor.framework.db.local;

  provides org.jminor.framework.db.EntityConnectionProvider
          with org.jminor.framework.db.local.LocalEntityConnectionProvider;
}