/**
 * JavaFX ui application classes.
 */
module dev.codion.javafx.framework {
  requires org.slf4j;
  requires org.json;
  requires transitive javafx.graphics;
  requires transitive javafx.controls;
  requires transitive dev.codion.framework.model;

  exports dev.codion.javafx.framework.model;
  exports dev.codion.javafx.framework.ui;
  exports dev.codion.javafx.framework.ui.values;
}