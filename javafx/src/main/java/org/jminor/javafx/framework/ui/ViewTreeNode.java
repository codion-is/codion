/*
 * Copyright (c) 2004 - 2018, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.javafx.framework.ui;

import java.util.List;

/**
 * A View node for a tree structure, with a parent, siblings and children.
 * @param <T> the type of the sibling and child views
 */
public interface ViewTreeNode<T extends ViewTreeNode> {

  /**
   * @return the parent view or null if none exists
   */
  ViewTreeNode<T> getParentView();

  /**
   * @return the previous sibling or null if none exists, wraps around
   */
  T getPreviousSiblingView();

  /**
   * @return the next sibling or null if none exists, wraps around
   */
  T getNextSiblingView();

  /**
   * @return a List containing the child views, empty if none exist
   */
  List<T> getChildViews();
}
