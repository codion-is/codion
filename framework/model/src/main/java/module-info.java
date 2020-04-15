/**
 * Non-ui specific application model classes.
 */
module org.jminor.framework.model {
  requires org.slf4j;
  requires transitive org.jminor.common.model;
  requires transitive org.jminor.framework.db.core;

  exports org.jminor.framework.model;
}