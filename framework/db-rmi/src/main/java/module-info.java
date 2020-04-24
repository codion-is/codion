/**
 * Framework database connection classes for connection via RMI.
 * @provides org.jminor.framework.db.EntityConnectionProvider
 */
module org.jminor.framework.db.rmi {
  requires org.slf4j;
  requires transitive org.jminor.common.rmi;
  requires transitive org.jminor.framework.db.core;

  exports org.jminor.framework.db.rmi;

  provides org.jminor.framework.db.EntityConnectionProvider
          with org.jminor.framework.db.rmi.RemoteEntityConnectionProvider;
}