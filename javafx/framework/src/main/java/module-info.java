/**
 * JavaFX ui application classes.
 */
module is.codion.javafx.framework {
  requires org.slf4j;
  requires org.json;
  requires transitive javafx.graphics;
  requires transitive javafx.controls;
  requires transitive is.codion.framework.i18n;
  requires transitive is.codion.framework.model;

  exports is.codion.javafx.framework.model;
  exports is.codion.javafx.framework.ui;
  exports is.codion.javafx.framework.ui.values;
}