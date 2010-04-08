/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.tools.profiling;

import org.jminor.common.db.User;
import org.jminor.common.model.LoadTestModel;
import org.jminor.framework.Configuration;
import org.jminor.framework.client.model.EntityApplicationModel;
import org.jminor.framework.client.model.EntityTableModel;

import java.util.ArrayList;
import java.util.List;

/**
 * A class for running multiple EntityApplicationModel instances for testing/profiling purposes.
 */
public abstract class ProfilingModel extends LoadTestModel {

  public ProfilingModel(final User user) {
    super(user, Integer.parseInt(System.getProperty(Configuration.PROFILING_THINKTIME, "2000")),
            Integer.parseInt(System.getProperty(Configuration.PROFILING_LOGIN_WAIT, "2")),
            Integer.parseInt(System.getProperty(Configuration.PROFILING_BATCH_SIZE, "10")), 200);
    loadDomainModel();
  }

  public static void selectRandomRow(final EntityTableModel model) {
    if (model.getRowCount() == 0)
      return;

    model.setSelectedItemIndex(random.nextInt(model.getRowCount()));
  }

  public static void selectRandomRows(final EntityTableModel model, final int count) {
    if (model.getRowCount() == 0)
      return;

    final List<Integer> indexes = new ArrayList<Integer>();
    for (int i = 0; i < count; i++)
      indexes.add(random.nextInt(model.getRowCount()));

    model.setSelectedItemIndexes(indexes);
  }

  public static void selectRandomRows(final EntityTableModel model, final double ratio) {
    if (model.getRowCount() == 0)
      return;

    final int toSelect = ratio > 0 ? (int) Math.floor(model.getRowCount()/ratio) : 1;
    final List<Integer> indexes = new ArrayList<Integer>();
    for (int i = 0; i < toSelect; i++)
      indexes.add(i);

    model.setSelectedItemIndexes(indexes);
  }

  @Override
  protected void disconnectApplication(final Object applicationModel) {
    ((EntityApplicationModel) applicationModel).getDbProvider().disconnect();
  }

  protected abstract void loadDomainModel();
}
