/**
 * Common classes used throughout, such as:<br>
 * <br>
 * {@link is.codion.common.event.Event}<br>
 * {@link is.codion.common.event.EventObserver}<br>
 * {@link is.codion.common.state.State}<br>
 * {@link is.codion.common.state.StateObserver}<br>
 * {@link is.codion.common.value.Value}<br>
 * {@link is.codion.common.value.ValueObserver}<br>
 * <br>
 * Configuration values:<br>
 * {@link is.codion.common.Text#DEFAULT_COLLATOR_LANGUAGE}<br>
 * @uses is.codion.common.logging.LoggerProxy
 */
module is.codion.common.core {
  exports is.codion.common;
  exports is.codion.common.event;
  exports is.codion.common.format;
  exports is.codion.common.item;
  exports is.codion.common.logging;
  exports is.codion.common.property;
  exports is.codion.common.proxy;
  exports is.codion.common.scheduler;
  exports is.codion.common.state;
  exports is.codion.common.user;
  exports is.codion.common.value;
  exports is.codion.common.version;

  uses is.codion.common.logging.LoggerProxy;
}