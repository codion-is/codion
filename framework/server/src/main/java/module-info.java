module org.jminor.framework.server {
  requires java.rmi;
  requires java.sql;
  requires java.management;
  requires jdk.management;
  requires slf4j.api;
  requires org.jminor.common.core;
  requires org.jminor.common.db;
  requires org.jminor.common.remote;
  requires org.jminor.framework.db.core;
  requires org.jminor.framework.db.local;
  requires org.jminor.framework.db.remote;
  exports org.jminor.framework.server;
}