module org.jminor.framework.server {
  requires slf4j.api;
  requires java.management;
  requires jdk.management;
  requires org.jminor.framework.db.local;
  requires org.jminor.framework.db.remote;

  exports org.jminor.framework.server;
}