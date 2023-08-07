/*
 * Copyright (c) 2018 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.rmi.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputFilter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static is.codion.common.NullOrEmpty.nullOrEmpty;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

/**
 * Implements a serialization whitelist
 */
public final class SerializationWhitelist {

  private static final Logger LOG = LoggerFactory.getLogger(SerializationWhitelist.class);

  private static final String CLASSPATH_PREFIX = "classpath:";

  private SerializationWhitelist() {}

  /**
   * Configures a serialization whitelist, does nothing if {@code whitelist} is null or empty.
   * Supports 'classpath:' prefix for a whitelist in the classpath root.
   * @param whitelistFile the path to the file containing the whitelisted class names
   */
  public static void configure(String whitelistFile) {
    if (!nullOrEmpty(whitelistFile)) {
      ObjectInputFilter.Config.setSerialFilter(new SerializationFilter(whitelistFile));
      LOG.info("Serialization filter whitelist set: " + whitelistFile);
    }
  }

  /**
   * Configures a serialization whitelist for a dry run, does nothing if {@code dryRunFile} is null or empty.
   * Note that this will append to an existing file.
   * @param dryRunFile the dry-run results file to write to, appended to if it exists
   * @throws IllegalArgumentException in case of a classpath dry run file
   */
  public static void configureDryRun(String dryRunFile) {
    if (!nullOrEmpty(dryRunFile)) {
      ObjectInputFilter.Config.setSerialFilter(new SerializationFilterDryRun(dryRunFile));
      LOG.info("Serialization filter whitelist set for dry-run: " + dryRunFile);
    }
  }

  /**
   * Returns true if a serialization dry-run is active.
   * @return true if a dry-run is active.
   */
  public static boolean isSerializationDryRunActive() {
    return ObjectInputFilter.Config.getSerialFilter() instanceof SerializationFilterDryRun;
  }

  /**
   * Writes the class names collected during a dry-run to file.
   * If dry-run was not active this method has no effect.
   */
  public static void writeDryRunWhitelist() {
    ObjectInputFilter serialFilter = ObjectInputFilter.Config.getSerialFilter();
    if (serialFilter instanceof SerializationFilterDryRun) {
      ((SerializationFilterDryRun) serialFilter).writeToFile();
    }
  }

  private static final class SerializationFilterDryRun implements ObjectInputFilter {

    private final String whitelistFile;
    private final Set<Class<?>> deserializedClasses = new HashSet<>();

    private SerializationFilterDryRun(String whitelistFile) {
      if (requireNonNull(whitelistFile).toLowerCase().startsWith(CLASSPATH_PREFIX)) {
        throw new IllegalArgumentException("Filter dry run can not be performed with a classpath whitelist: " + whitelistFile);
      }
      if (new File(whitelistFile).exists()) {
        throw new IllegalArgumentException("Whitelist file to write to after dry-run already exists");
      }
      this.whitelistFile = requireNonNull(whitelistFile, "whitelistFile");
    }

    @Override
    public Status checkInput(FilterInfo filterInfo) {
      Class<?> clazz = filterInfo.serialClass();
      if (clazz != null) {
        deserializedClasses.add(clazz);
      }

      return Status.ALLOWED;
    }

    private void writeToFile() {
      try {
        File file = new File(whitelistFile);
        if (!file.createNewFile()) {
          throw new IOException("Whitelist file already exists: " + whitelistFile);
        }
        Files.write(file.toPath(), deserializedClasses.stream()
                .map(Class::getName)
                .sorted()
                .collect(toList()));
        LOG.debug("Serialization whitelist written: " + whitelistFile);
      }
      catch (Exception e) {
        LOG.error("Error while writing serialization filter dry run results: " + whitelistFile, e);
      }
    }
  }

  static final class SerializationFilter implements ObjectInputFilter {

    private static final String COMMENT = "#";
    private static final String WILDCARD = "*";

    private final Set<String> allowedClassnames = new HashSet<>();
    private final List<String> allowedWildcardClassnames = new ArrayList<>();

    SerializationFilter(String whitelistFile) {
      this(readWhitelistItems(whitelistFile));
    }

    SerializationFilter(Collection<String> whitelistItems) {
      addWhitelistItems(whitelistItems);
    }

    @Override
    public Status checkInput(FilterInfo filterInfo) {
      Class<?> clazz = filterInfo.serialClass();
      if (clazz == null) {
        return Status.ALLOWED;
      }

      return checkInput(clazz.getName());
    }

    Status checkInput(String classname) {
      if (allowedClassnames.contains(classname) || allowWildcard(classname)) {
        return Status.ALLOWED;
      }
      LOG.error("Serialization rejected: " + classname);

      return Status.REJECTED;
    }

    private void addWhitelistItems(Collection<String> whitelistItems) {
      requireNonNull(whitelistItems).forEach(whitelistItem -> {
        if (!whitelistItem.startsWith(COMMENT)) {
          if (whitelistItem.endsWith(WILDCARD)) {
            allowedWildcardClassnames.add(whitelistItem.substring(0, whitelistItem.length() - 1));
          }
          else {
            allowedClassnames.add(whitelistItem);
          }
        }
      });
    }

    private boolean allowWildcard(String classname) {
      if (allowedWildcardClassnames.isEmpty()) {
        return false;
      }

      for (int i = 0; i < allowedWildcardClassnames.size(); i++) {
        if (classname.startsWith(allowedWildcardClassnames.get(i))) {
          return true;
        }
      }

      return false;
    }

    private static Collection<String> readWhitelistItems(String whitelistFile) {
      if (requireNonNull(whitelistFile).startsWith(CLASSPATH_PREFIX)) {
        return readClasspathWhitelistItems(whitelistFile);
      }

      return readFileWhitelistItems(whitelistFile);
    }

    private static Collection<String> readClasspathWhitelistItems(String whitelistFile) {
      String path = classpathFilepath(whitelistFile);
      try (InputStream whitelistFileStream = SerializationWhitelist.class.getClassLoader().getResourceAsStream(path)) {
        if (whitelistFileStream == null) {
          throw new RuntimeException("Whitelist file not found on classpath: " + path);
        }
        return new BufferedReader(new InputStreamReader(whitelistFileStream, StandardCharsets.UTF_8))
                .lines()
                .collect(toSet());
      }
      catch (IOException e) {
        throw new RuntimeException("Unable to load whitelist from classpath: " + whitelistFile, e);
      }
    }

    private static Collection<String> readFileWhitelistItems(String whitelistFile) {
      try (Stream<String> stream = Files.lines(Paths.get(whitelistFile))) {
        return stream.collect(toSet());
      }
      catch (IOException e) {
        LOG.error("Unable to read serialization whitelist: " + whitelistFile);
        throw new RuntimeException(e);
      }
    }

    private static String classpathFilepath(String whitelistFile) {
      String path = whitelistFile.substring(CLASSPATH_PREFIX.length());
      if (path.startsWith("/")) {
        path = path.substring(1);
      }
      if (path.contains("/")) {
        throw new IllegalArgumentException("Whitelist file must be in the classpath root");
      }

      return path;
    }
  }
}
