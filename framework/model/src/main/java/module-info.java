/**
 * Framework application model classes, such as:<br>
 * <br>
 * {@link is.codion.framework.model.EntityModel}<br>
 * {@link is.codion.framework.model.EntityEditModel}<br>
 * {@link is.codion.framework.model.EntityTableModel}<br>
 * {@link is.codion.framework.model.EntityTableConditionModel}<br>
 * {@link is.codion.framework.model.EntityApplicationModel}<br>
 */
module is.codion.framework.model {
  requires org.slf4j;
  requires org.json;
  requires transitive is.codion.common.model;
  requires transitive is.codion.framework.db.core;

  exports is.codion.framework.model;
}