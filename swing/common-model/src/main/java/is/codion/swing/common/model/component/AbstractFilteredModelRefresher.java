/*
 * Copyright (c) 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.model.component;

import is.codion.common.model.FilteredModel;
import is.codion.swing.common.model.worker.ProgressWorker;

import java.util.Collection;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * A default swing based {@link FilteredModel.Refresher}.
 * @param <T> the model row type
 */
public abstract class AbstractFilteredModelRefresher<T> extends FilteredModel.AbstractRefresher<T> {

  private ProgressWorker<Collection<T>, ?> refreshWorker;

  /**
   * @param rowSupplier the row supplier
   */
  protected AbstractFilteredModelRefresher(Supplier<Collection<T>> rowSupplier) {
    super(rowSupplier);
  }

  protected final void refreshAsync(Consumer<Collection<T>> afterRefresh) {
    cancelCurrentRefresh();
    refreshWorker = ProgressWorker.builder(getRowSupplier()::get)
            .onStarted(this::onRefreshStarted)
            .onResult(items -> onRefreshResult(items, afterRefresh))
            .onException(this::onRefreshFailedAsync)
            .execute();
  }

  protected final void refreshSync(Consumer<Collection<T>> afterRefresh) {
    onRefreshStarted();
    try {
      onRefreshResult(getRowSupplier().get(), afterRefresh);
    }
    catch (Exception e) {
      onRefreshFailedSync(e);
    }
  }

  /**
   * Handles the refresh result, by adding the given items to the model
   * @param items the items resulting from the refresh operation
   */
  protected abstract void handleRefreshResult(Collection<T> items);

  private void onRefreshStarted() {
    setRefreshing(true);
  }

  private void onRefreshFailedAsync(Throwable throwable) {
    refreshWorker = null;
    setRefreshing(false);
    fireRefreshFailedEvent(throwable);
  }

  private void onRefreshFailedSync(Throwable throwable) {
    setRefreshing(false);
    if (throwable instanceof RuntimeException) {
      throw (RuntimeException) throwable;
    }

    throw new RuntimeException(throwable);
  }

  private void onRefreshResult(Collection<T> items, Consumer<Collection<T>> afterRefresh) {
    refreshWorker = null;
    setRefreshing(false);
    handleRefreshResult(items);
    if (afterRefresh != null) {
      afterRefresh.accept(items);
    }
    fireRefreshEvent();
  }

  private void cancelCurrentRefresh() {
    ProgressWorker<?, ?> worker = refreshWorker;
    if (worker != null) {
      worker.cancel(true);
    }
  }
}
