/*
 * Copyright (c) 2016 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.javafx.framework.ui;

import java.util.List;
import java.util.Optional;

/**
 * A View node for a tree structure, with a parent, siblings and children.
 * @param <T> the type of the sibling and child views
 */
public interface ViewTreeNode<T extends ViewTreeNode<T>> {

  /**
   * @return the parent view or an empty Optional if none exists
   */
  Optional<ViewTreeNode<T>> getParentView();

  /**
   * @return the previous sibling or an empty Optional if none exists, wraps around
   */
  Optional<T> getPreviousSiblingView();

  /**
   * @return the next sibling or an empty Optional if none exists, wraps around
   */
  Optional<T> getNextSiblingView();

  /**
   * @return a List containing the child views, empty if none exist
   */
  List<T> getChildViews();
}
