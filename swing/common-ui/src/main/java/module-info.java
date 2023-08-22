/**
 * Common Swing UI classes, such as:<br>
 * <br>
 * {@link is.codion.swing.common.ui.KeyEvents}<br>
 * {@link is.codion.swing.common.ui.component.Components}<br>
 * {@link is.codion.swing.common.ui.component.builder.ComponentBuilder}<br>
 * {@link is.codion.swing.common.ui.component.builder.AbstractComponentBuilder}<br>
 * {@link is.codion.swing.common.ui.component.value.ComponentValue}<br>
 * {@link is.codion.swing.common.ui.component.value.AbstractComponentValue}<br>
 * {@link is.codion.swing.common.ui.dialog.Dialogs}<br>
 * {@link is.codion.swing.common.ui.laf.LookAndFeelProvider}<br>
 * {@link is.codion.swing.common.ui.layout.Layouts}<br>
 */
module is.codion.swing.common.ui {
  requires java.rmi;
  requires transitive is.codion.swing.common.model;
  requires transitive org.kordamp.ikonli.core;
  requires org.kordamp.ikonli.swing;

  exports is.codion.swing.common.ui;
  exports is.codion.swing.common.ui.component;
  exports is.codion.swing.common.ui.component.builder;
  exports is.codion.swing.common.ui.component.button;
  exports is.codion.swing.common.ui.component.calendar;
  exports is.codion.swing.common.ui.component.combobox;
  exports is.codion.swing.common.ui.component.label;
  exports is.codion.swing.common.ui.component.list;
  exports is.codion.swing.common.ui.component.panel;
  exports is.codion.swing.common.ui.component.progressbar;
  exports is.codion.swing.common.ui.component.scrollpane;
  exports is.codion.swing.common.ui.component.slider;
  exports is.codion.swing.common.ui.component.spinner;
  exports is.codion.swing.common.ui.component.splitpane;
  exports is.codion.swing.common.ui.component.tabbedpane;
  exports is.codion.swing.common.ui.component.table;
  exports is.codion.swing.common.ui.component.text;
  exports is.codion.swing.common.ui.component.value;
  exports is.codion.swing.common.ui.border;
  exports is.codion.swing.common.ui.control;
  exports is.codion.swing.common.ui.dialog;
  exports is.codion.swing.common.ui.icon;
  exports is.codion.swing.common.ui.laf;
  exports is.codion.swing.common.ui.layout;
}