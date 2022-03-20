/**
 * Framework database connection classes for connection via RMI.
 * @provides is.codion.framework.db.EntityConnectionProvider
 */
module is.codion.framework.db.rmi {
  requires org.slf4j;
  requires transitive is.codion.common.rmi;
  requires transitive is.codion.framework.db.core;

  exports is.codion.framework.db.rmi;

  provides is.codion.framework.db.EntityConnectionProvider.Builder
          with is.codion.framework.db.rmi.DefaultRemoteEntityConnectionProvider.DefaultBuilder;
}