/**
 * Framework database connection classes for local JDBC connections.
 * @provides dev.codion.framework.db.EntityConnectionProvider
 */
module dev.codion.framework.db.local {
  requires org.slf4j;
  requires transitive dev.codion.framework.db.core;

  exports dev.codion.framework.db.local;

  provides dev.codion.framework.db.EntityConnectionProvider
          with dev.codion.framework.db.local.LocalEntityConnectionProvider;
}