/*
 * Copyright (c) 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component;

import javax.swing.Action;
import javax.swing.JToolBar;
import javax.swing.SwingConstants;
import java.util.ArrayList;
import java.util.List;

import static java.util.Objects.requireNonNull;

final class DefaultToolBarBuilder extends AbstractComponentBuilder<Void, JToolBar, ToolBarBuilder> implements ToolBarBuilder {

  private final List<Action> actions = new ArrayList<>();

  private boolean floatable = true;
  private int orientation = SwingConstants.HORIZONTAL;
  private boolean rollover = false;
  private boolean borderPainted = true;

  @Override
  public ToolBarBuilder floatable(boolean floatable) {
    this.floatable = floatable;
    return this;
  }

  @Override
  public ToolBarBuilder orientation(int orientation) {
    this.orientation = orientation;
    return this;
  }

  @Override
  public ToolBarBuilder rollover(boolean rollover) {
    this.rollover = rollover;
    return this;
  }

  @Override
  public ToolBarBuilder borderPainted(boolean borderPainted) {
    this.borderPainted = borderPainted;
    return this;
  }

  @Override
  public ToolBarBuilder action(Action action) {
    actions.add(requireNonNull(action));
    return this;
  }

  @Override
  public ToolBarBuilder separator() {
    actions.add(null);
    return this;
  }

  @Override
  protected JToolBar createComponent() {
    JToolBar toolBar = new JToolBar();
    toolBar.setFloatable(floatable);
    toolBar.setOrientation(orientation);
    toolBar.setRollover(rollover);
    toolBar.setBorderPainted(borderPainted);
    for (Action action : actions) {
      if (action == null) {
        toolBar.addSeparator();
      }
      else {
        toolBar.add(action);
      }
    }

    return toolBar;
  }

  @Override
  protected ComponentValue<Void, JToolBar> createComponentValue(JToolBar component) {
    throw new UnsupportedOperationException("A ComponentValue can not be based on a JToolBar");
  }

  @Override
  protected void setInitialValue(JToolBar component, Void initialValue) {}
}