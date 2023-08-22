/**
 * RMI client/server classes, such as:<br>
 * <br>
 * {@link is.codion.common.rmi.client.ConnectionRequest}<br>
 * {@link is.codion.common.rmi.server.Server}<br>
 * {@link is.codion.common.rmi.server.ServerConfiguration}<br>
 * {@link is.codion.common.rmi.server.LoginProxy}<br>
 * {@link is.codion.common.rmi.server.RemoteClient}<br>
 */
module is.codion.common.rmi {
  requires org.slf4j;
  requires jdk.management;
  requires nl.altindag.ssl;
  requires transitive java.rmi;
  requires transitive is.codion.common.core;

  exports is.codion.common.rmi.client;
  exports is.codion.common.rmi.server;
  exports is.codion.common.rmi.server.exception;

  uses is.codion.common.rmi.server.AuxiliaryServerFactory;
  uses is.codion.common.rmi.server.LoginProxy;
}