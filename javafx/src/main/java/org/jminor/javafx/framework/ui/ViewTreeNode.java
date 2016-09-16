/*
 * Copyright (c) 2004 - 2016, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.javafx.framework.ui;

import java.util.List;

/**
 * A View node for a tree structure, with a parent, siblings and children.
 */
public interface ViewTreeNode {

  /**
   * @return the parent view or null if none exists
   */
  ViewTreeNode getParentView();

  /**
   * @return the previous sibling or null if none exists, wraps around
   */
  ViewTreeNode getPreviousSiblingView();

  /**
   * @return the next sibling or null if none exists, wraps around
   */
  ViewTreeNode getNextSiblingView();

  /**
   * @return a List containing the child views, empty if none exist
   */
  List<? extends ViewTreeNode> getChildViews();
}
