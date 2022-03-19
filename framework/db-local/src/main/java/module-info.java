/**
 * Framework database connection classes for local JDBC connections.
 * @provides is.codion.framework.db.EntityConnectionProvider
 */
module is.codion.framework.db.local {
  requires org.slf4j;
  requires transitive is.codion.framework.db.core;

  exports is.codion.framework.db.local;

  provides is.codion.framework.db.EntityConnectionProvider.Builder
          with is.codion.framework.db.local.LocalEntityConnectionProvider.DefaultBuilder;
}