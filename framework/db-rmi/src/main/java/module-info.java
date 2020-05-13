/**
 * Framework database connection classes for connection via RMI.
 * @provides dev.codion.framework.db.EntityConnectionProvider
 */
module dev.codion.framework.db.rmi {
  requires org.slf4j;
  requires transitive dev.codion.common.rmi;
  requires transitive dev.codion.framework.db.core;

  exports dev.codion.framework.db.rmi;

  provides dev.codion.framework.db.EntityConnectionProvider
          with dev.codion.framework.db.rmi.RemoteEntityConnectionProvider;
}