/**
 * JavaFX ui application classes.
 */
module org.jminor.javafx.framework {
  requires json;
  requires transitive javafx.graphics;
  requires transitive javafx.controls;
  requires transitive org.jminor.framework.model;

  exports org.jminor.javafx.framework.model;
  exports org.jminor.javafx.framework.ui;
  exports org.jminor.javafx.framework.ui.values;
}