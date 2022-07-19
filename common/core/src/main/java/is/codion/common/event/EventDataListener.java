/*
 * Copyright (c) 2012 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.event;

/**
 * An event listener receiving some data on each event occurrence
 * @param <T> the type of data propagated with the event.
 */
public interface EventDataListener<T> {

  /**
   * Called when the event occurs.
   * @param data information relating to the event.
   */
  void onEvent(T data);
}
