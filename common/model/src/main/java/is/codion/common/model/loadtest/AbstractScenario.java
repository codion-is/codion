/*
 * Copyright (c) 2020 - 2024, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.model.loadtest;

import is.codion.common.model.loadtest.LoadTest.Scenario;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static java.util.Objects.requireNonNull;

/**
 * An abstract usage scenario.
 * @param <T> the application type used to run this scenario
 */
public abstract class AbstractScenario<T> implements Scenario<T> {

  private final String name;

  /**
   * Instantiates a new Scenario using the simple class name as scenario name
   */
  protected AbstractScenario() {
    this.name = getClass().getSimpleName();
  }

  /**
   * Instantiates a new Scenario with the given name
   * @param name the scenario name
   */
  protected AbstractScenario(String name) {
    this.name = requireNonNull(name, "name");
  }

  @Override
  public final String name() {
    return this.name;
  }

  @Override
  public final String toString() {
    return name;
  }

  @Override
  public final Result run(T application) {
    requireNonNull(application, "Can not run without an application");
    try {
      prepare(application);
      long startTime = System.nanoTime();
      perform(application);

      return Result.success(name, (int) TimeUnit.NANOSECONDS.toMicros(System.nanoTime() - startTime));
    }
    catch (Throwable e) {
      return Result.failure(name, e);
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
    return obj instanceof Scenario && ((Scenario<T>) obj).name().equals(name);
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

  static final class DefaultRunResult implements Result {

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
