/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component;

import javax.swing.JComponent;
import javax.swing.JPanel;
import java.awt.LayoutManager;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static java.util.Objects.requireNonNull;

final class DefaultPanelBuilder extends AbstractComponentBuilder<Void, JPanel, PanelBuilder> implements PanelBuilder {

  private final JPanel panel;
  private final List<ComponentConstraints> componentConstraints = new ArrayList<>();

  private LayoutManager layout;

  DefaultPanelBuilder(final JPanel panel) {
    this.panel = requireNonNull(panel);
  }

  DefaultPanelBuilder(final LayoutManager layout) {
    this.layout = layout;
    this.panel = null;
  }

  @Override
  public PanelBuilder layout(final LayoutManager layoutManager) {
    this.layout = requireNonNull(layoutManager);
    return this;
  }

  @Override
  public PanelBuilder add(final JComponent component) {
    componentConstraints.add(new ComponentConstraints(requireNonNull(component)));
    return this;
  }

  @Override
  public PanelBuilder addConstrained(final JComponent component, final Object constraints) {
    componentConstraints.add(new ComponentConstraints(requireNonNull(component), requireNonNull(constraints)));
    return this;
  }

  @Override
  public PanelBuilder add(final JComponent... components) {
    add(Arrays.asList(components));
    return this;
  }

  @Override
  public PanelBuilder add(final Collection<? extends JComponent> components) {
    requireNonNull(components).forEach(this::add);
    return this;
  }

  @Override
  protected JPanel buildComponent() {
    final JPanel component = panel == null ? new JPanel() : panel;
    if (layout != null) {
      component.setLayout(layout);
    }
    componentConstraints.forEach(componentConstraint -> {
      if (componentConstraint.constraints != null) {
        component.add(componentConstraint.component, componentConstraint.constraints);
      }
      else {
        component.add(componentConstraint.component);
      }
    });

    return component;
  }

  @Override
  protected ComponentValue<Void, JPanel> buildComponentValue(final JPanel component) {
    throw new UnsupportedOperationException("A ComponentValue can not be based on a JPanel");
  }

  @Override
  protected void setInitialValue(final JPanel component, final Void initialValue) {}

  private static final class ComponentConstraints {

    private final JComponent component;
    private final Object constraints;

    private ComponentConstraints(final JComponent component) {
      this(component, null);
    }

    private ComponentConstraints(final JComponent component, final Object constraints) {
      this.component = component;
      this.constraints = constraints;
    }
  }
}
