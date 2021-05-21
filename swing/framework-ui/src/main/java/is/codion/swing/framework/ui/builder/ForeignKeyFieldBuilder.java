/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.ui.builder;

import is.codion.common.state.StateObserver;
import is.codion.framework.domain.entity.Entity;

import javax.swing.JTextField;
import java.awt.Dimension;
import java.util.function.Consumer;

/**
 * Builds a read-only JTextField displaying a Entity instance.
 */
public interface ForeignKeyFieldBuilder extends ComponentBuilder<Entity, JTextField> {

  @Override
  ForeignKeyFieldBuilder preferredHeight(int preferredHeight);

  @Override
  ForeignKeyFieldBuilder preferredWidth(int preferredWidth);

  @Override
  ForeignKeyFieldBuilder preferredSize(Dimension preferredSize);

  @Override
  ForeignKeyFieldBuilder transferFocusOnEnter(boolean transferFocusOnEnter);

  @Override
  ForeignKeyFieldBuilder enabledState(StateObserver enabledState);

  @Override
  ForeignKeyFieldBuilder onBuild(Consumer<JTextField> onBuild);

  ForeignKeyFieldBuilder columns(int columns);
}
