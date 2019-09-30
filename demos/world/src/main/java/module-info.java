module org.jminor.framework.demos.world {
  requires org.jminor.swing.framework.ui;

  exports org.jminor.framework.demos.world.beans.ui
          to org.jminor.swing.framework.ui;
  exports org.jminor.framework.demos.world.domain
          to org.jminor.framework.db.local;
}