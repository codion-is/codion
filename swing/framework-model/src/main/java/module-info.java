/**
 * Framework Swing model classes, such as:<br>
 * <br>
 * {@link is.codion.swing.framework.model.SwingEntityModel}<br>
 * {@link is.codion.swing.framework.model.SwingEntityEditModel}<br>
 * {@link is.codion.swing.framework.model.SwingEntityTableModel}<br>
 * {@link is.codion.swing.framework.model.SwingEntityApplicationModel}<br>
 * {@link is.codion.swing.framework.model.component.EntityComboBoxModel}<br>
 */
module is.codion.swing.framework.model {
  requires org.slf4j;
  requires org.json;
  requires transitive is.codion.framework.model;
  requires transitive is.codion.swing.common.model;

  exports is.codion.swing.framework.model;
  exports is.codion.swing.framework.model.component;
}