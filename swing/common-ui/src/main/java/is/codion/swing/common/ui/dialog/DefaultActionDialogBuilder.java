/*
 * Copyright (c) 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.dialog;

import is.codion.swing.common.ui.KeyEvents;
import is.codion.swing.common.ui.component.button.ButtonPanelBuilder;
import is.codion.swing.common.ui.component.panel.BorderLayoutPanelBuilder;
import is.codion.swing.common.ui.component.panel.PanelBuilder;
import is.codion.swing.common.ui.control.Controls;
import is.codion.swing.common.ui.layout.Layouts;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.WindowConstants;
import javax.swing.border.Border;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Arrays;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

import static is.codion.swing.common.ui.dialog.DefaultComponentDialogBuilder.createDialog;
import static java.awt.event.KeyEvent.VK_ESCAPE;
import static java.util.Objects.requireNonNull;
import static javax.swing.JComponent.WHEN_IN_FOCUSED_WINDOW;

class DefaultActionDialogBuilder<B extends ActionDialogBuilder<B>> extends AbstractDialogBuilder<B> implements ActionDialogBuilder<B> {

  private final Controls controls = Controls.controls();
  private final JComponent component;

  private Action defaultAction;
  private Action escapeAction;
  private Border buttonPanelBorder = BorderFactory.createEmptyBorder(10, 10, 10, 10);
  private int buttonPanelConstraints = FlowLayout.TRAILING;
  private boolean modal = true;
  private boolean resizable = true;
  private Dimension size;
  private Consumer<JDialog> onShown;

  DefaultActionDialogBuilder(JComponent component) {
    this.component = requireNonNull(component);
  }

  @Override
  public B action(Action action) {
    controls.add(action);
    return (B) this;
  }

  @Override
  public B defaultAction(Action defaultAction) {
    this.defaultAction = requireNonNull(defaultAction);
    if (!controls.actions().contains(defaultAction)) {
      controls.add(defaultAction);
    }
    return (B) this;
  }

  @Override
  public B escapeAction(Action escapeAction) {
    this.escapeAction = requireNonNull(escapeAction);
    if (!controls.actions().contains(escapeAction)) {
      controls.add(escapeAction);
    }
    return (B) this;
  }

  @Override
  public final B modal(boolean modal) {
    this.modal = modal;
    return (B) this;
  }

  @Override
  public final B resizable(boolean resizable) {
    this.resizable = resizable;
    return (B) this;
  }

  @Override
  public final B size(Dimension size) {
    this.size = requireNonNull(size);
    return (B) this;
  }

  @Override
  public final B buttonPanelConstraints(int buttonPanelConstraints) {
    this.buttonPanelConstraints = buttonPanelConstraints;
    return (B) this;
  }

  @Override
  public final B buttonPanelBorder(Border buttonPanelBorder) {
    this.buttonPanelBorder = requireNonNull(buttonPanelBorder);
    return (B) this;
  }

  @Override
  public final B onShown(Consumer<JDialog> onShown) {
    this.onShown = onShown;
    return (B) this;
  }

  @Override
  public final JDialog show() {
    JDialog dialog = build();
    dialog.setVisible(true);

    return dialog;
  }

  @Override
  public JDialog build() {
    if (controls.isEmpty()) {
      throw new IllegalStateException("No controls have been specified");
    }

    JPanel buttonPanel = ButtonPanelBuilder.builder(controls).build();
    JPanel panel = BorderLayoutPanelBuilder.builder(new BorderLayout())
            .centerComponent(component)
            .southComponent(PanelBuilder.builder(Layouts.flowLayout(buttonPanelConstraints))
                    .add(buttonPanel)
                    .border(buttonPanelBorder)
                    .build())
            .build();

    JDialog dialog = createDialog(owner, titleProvider, icon, panel, size, locationRelativeTo, location, modal, resizable, onShown);
    dialog.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
    if (defaultAction != null) {
      Arrays.stream(buttonPanel.getComponents())
              .filter(new IsButton())
              .map(new CastToButtonFunction())
              .filter(new IsActionButton(defaultAction))
              .findFirst()
              .ifPresent(new SetDefaultButton(dialog));
    }
    if (escapeAction != null) {
      dialog.addWindowListener(new EscapeOnWindowClosingListener(escapeAction));
      KeyEvents.builder(VK_ESCAPE)
              .condition(WHEN_IN_FOCUSED_WINDOW)
              .action(escapeAction)
              .enable(dialog.getRootPane());
    }

    return dialog;
  }

  protected final Controls controls() {
    return controls;
  }

  protected final JComponent component() {
    return component;
  }

  private static final class EscapeOnWindowClosingListener extends WindowAdapter {

    private final Action escapeAction;

    private EscapeOnWindowClosingListener(Action escapeAction) {
      this.escapeAction = escapeAction;
    }

    @Override
    public void windowClosing(WindowEvent e) {
      escapeAction.actionPerformed(null);
    }
  }

  private static final class IsButton implements Predicate<Component> {

    @Override
    public boolean test(Component component) {
      return component instanceof JButton;
    }
  }

  private static final class CastToButtonFunction implements Function<Component, JButton> {

    @Override
    public JButton apply(Component component) {
      return (JButton) component;
    }
  }

  private static final class IsActionButton implements Predicate<JButton> {

    private final Action defaultAction;

    private IsActionButton(Action defaultAction) {
      this.defaultAction = defaultAction;
    }

    @Override
    public boolean test(JButton button) {
      return button.getAction() == defaultAction;
    }
  }

  private static final class SetDefaultButton implements Consumer<JButton> {

    private final JDialog dialog;

    private SetDefaultButton(JDialog dialog) {
      this.dialog = dialog;
    }

    @Override
    public void accept(JButton button) {
      dialog.getRootPane().setDefaultButton(button);
    }
  }
}
