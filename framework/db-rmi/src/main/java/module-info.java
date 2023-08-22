/**
 * RMI based database connection classes.<br>
 * <br>
 * {@link is.codion.framework.db.rmi.RemoteEntityConnection}<br>
 * {@link is.codion.framework.db.rmi.RemoteEntityConnectionProvider}<br>
 * @provides is.codion.framework.db.EntityConnectionProvider
 */
module is.codion.framework.db.rmi {
  requires org.slf4j;
  requires transitive is.codion.common.rmi;
  requires transitive is.codion.framework.db.core;

  exports is.codion.framework.db.rmi;

  provides is.codion.framework.db.EntityConnectionProvider.Builder
          with is.codion.framework.db.rmi.DefaultRemoteEntityConnectionProviderBuilder;
}