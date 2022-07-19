/*
 * Copyright (c) 2020 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.tools.loadtest;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.Objects.requireNonNull;

/**
 * An abstract usage scenario.
 * @param <T> the type used to run this scenario
 */
public abstract class AbstractUsageScenario<T> implements UsageScenario<T> {

  private final String name;
  private final int maximumTime;
  private final AtomicInteger successfulRunCount = new AtomicInteger();
  private final AtomicInteger unsuccessfulRunCount = new AtomicInteger();
  private final List<Exception> exceptions = new ArrayList<>();

  /**
   * Instantiates a new UsageScenario using the simple class name as scenario name
   */
  public AbstractUsageScenario() {
    this.name = getClass().getSimpleName();
    this.maximumTime = 0;
  }

  /**
   * Instantiates a new UsageScenario with the given name
   * @param name the scenario name
   */
  public AbstractUsageScenario(String name) {
    this(name, 0);
  }

  /**
   * Instantiates a new UsageScenario with the given name
   * @param name the scenario name
   * @param maximumTimeMs the maximum time in milliseconds this scenario should take to run
   */
  public AbstractUsageScenario(String name, int maximumTimeMs) {
    this.name = requireNonNull(name, "name");
    if (maximumTimeMs < 0) {
      throw new IllegalArgumentException("Maximum time in ms must be a positive integer");
    }
    this.maximumTime = maximumTimeMs;
  }

  @Override
  public final String getName() {
    return this.name;
  }

  @Override
  public final int getMaximumTime() {
    return maximumTime;
  }

  @Override
  public final int getSuccessfulRunCount() {
    return successfulRunCount.get();
  }

  @Override
  public final int getUnsuccessfulRunCount() {
    return unsuccessfulRunCount.get();
  }

  @Override
  public final int getTotalRunCount() {
    return successfulRunCount.get() + unsuccessfulRunCount.get();
  }

  @Override
  public final List<Exception> getExceptions() {
    synchronized (exceptions) {
      return new ArrayList<>(exceptions);
    }
  }

  @Override
  public final void resetRunCount() {
    successfulRunCount.set(0);
    unsuccessfulRunCount.set(0);
  }

  @Override
  public final void clearExceptions() {
    synchronized (exceptions) {
      exceptions.clear();
    }
  }

  @Override
  public final String toString() {
    return name;
  }

  @Override
  public final void run(T application) {
    if (application == null) {
      throw new IllegalArgumentException("Can not run without an application");
    }
    try {
      prepare(application);
      perform(application);
      successfulRunCount.incrementAndGet();
    }
    catch (Exception e) {
      unsuccessfulRunCount.incrementAndGet();
      synchronized (exceptions) {
        exceptions.add(e);
      }
    }
    finally {
      cleanup(application);
    }
  }

  @Override
  public final int hashCode() {
    return name.hashCode();
  }

  @Override
  public final boolean equals(Object obj) {
    return obj instanceof UsageScenario && ((UsageScenario<T>) obj).getName().equals(name);
  }

  @Override
  public int getDefaultWeight() {
    return 1;
  }

  /**
   * Runs a set of actions on the given application.
   * @param application the application
   * @throws Exception in case of an exception
   */
  protected abstract void perform(T application) throws Exception;

  /**
   * Called before this scenario is run, override to prepare the application for each run
   * @param application the application
   */
  protected void prepare(T application) {/*Provided for subclasses*/}

  /**
   * Called after this scenario has been run, override to clean up the application after each run
   * @param application the application
   */
  protected void cleanup(T application) {/*Provided for subclasses*/}
}
