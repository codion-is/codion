/*
 * This file is part of Codion.
 *
 * Codion is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Codion is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Codion.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (c) 2020 - 2023, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.model.tools.loadtest;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.Objects.requireNonNull;

/**
 * An abstract usage scenario.
 * @param <T> the application type used to run this scenario
 */
public abstract class AbstractUsageScenario<T> implements UsageScenario<T> {

  private static final int MAXIMUM_EXCEPTIONS = 50;

  private final String name;
  private final int maximumTime;
  private final AtomicInteger successfulRunCount = new AtomicInteger();
  private final AtomicInteger unsuccessfulRunCount = new AtomicInteger();
  private final LinkedList<Throwable> exceptions = new LinkedList<>();

  /**
   * Instantiates a new UsageScenario using the simple class name as scenario name
   */
  protected AbstractUsageScenario() {
    this.name = getClass().getSimpleName();
    this.maximumTime = 0;
  }

  /**
   * Instantiates a new UsageScenario with the given name
   * @param name the scenario name
   */
  protected AbstractUsageScenario(String name) {
    this(name, 0);
  }

  /**
   * Instantiates a new UsageScenario with the given name
   * @param name the scenario name
   * @param maximumTimeMs the maximum time in milliseconds this scenario should take to run
   */
  protected AbstractUsageScenario(String name, int maximumTimeMs) {
    this.name = requireNonNull(name, "name");
    if (maximumTimeMs < 0) {
      throw new IllegalArgumentException("Maximum time in ms must be a positive integer");
    }
    this.maximumTime = maximumTimeMs;
  }

  @Override
  public final String name() {
    return this.name;
  }

  @Override
  public final int maximumTime() {
    return maximumTime;
  }

  @Override
  public final int successfulRunCount() {
    return successfulRunCount.get();
  }

  @Override
  public final int unsuccessfulRunCount() {
    return unsuccessfulRunCount.get();
  }

  @Override
  public final int totalRunCount() {
    return successfulRunCount.get() + unsuccessfulRunCount.get();
  }

  @Override
  public final List<Throwable> exceptions() {
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
  public final RunResult run(T application) {
    if (application == null) {
      throw new IllegalArgumentException("Can not run without an application");
    }
    try {
      prepare(application);
      long startTime = System.currentTimeMillis();
      perform(application);
      successfulRunCount.incrementAndGet();

      return RunResult.success(name, (int) (System.currentTimeMillis() - startTime));
    }
    catch (Throwable e) {
      unsuccessfulRunCount.incrementAndGet();
      synchronized (exceptions) {
        exceptions.add(e);
        if (exceptions.size() > MAXIMUM_EXCEPTIONS) {
          exceptions.removeFirst();
        }
      }

      return RunResult.failure(name, e);
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
    return obj instanceof UsageScenario && ((UsageScenario<T>) obj).name().equals(name);
  }

  @Override
  public int defaultWeight() {
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

  static final class DefaultRunResult implements RunResult {

    private final String scenario;
    private final int duration;
    private final Throwable exception;

    DefaultRunResult(String scenario, int duration, Throwable exception) {
      this.scenario = scenario;
      this.duration = duration;
      this.exception = exception;
    }

    @Override
    public int duration() {
      return duration;
    }

    @Override
    public String scenario() {
      return scenario;
    }

    @Override
    public boolean successful() {
      return exception == null;
    }

    @Override
    public Optional<Throwable> exception() {
      return Optional.ofNullable(exception);
    }
  }
}
