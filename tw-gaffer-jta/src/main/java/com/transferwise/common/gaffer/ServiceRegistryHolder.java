package com.transferwise.common.gaffer;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

public class ServiceRegistryHolder {

  private static volatile ServiceRegistry serviceRegistry;
  private static volatile Configuration configuration;

  @SuppressFBWarnings(value = "MS", justification = "Meant to expose internal representation.")
  public static Configuration getConfiguration() {
    if (configuration == null) {
      synchronized (ServiceRegistryHolder.class) {
        if (configuration == null) {
          createConfiguration();
        }
      }
    }
    return configuration;
  }

  @SuppressFBWarnings("MS")
  public static ServiceRegistry getServiceRegistry() {
    if (serviceRegistry == null) {
      synchronized (ServiceRegistryHolder.class) {
        if (serviceRegistry == null) {
          createServiceRegistry();
        }
      }
    }
    return serviceRegistry;
  }

  public static void destroyServiceRegistry() {
    if (serviceRegistry != null) {
      serviceRegistry.destroy();
      serviceRegistry = null;
    }
  }

  private static void createServiceRegistry() {
    serviceRegistry = new ServiceRegistry(getConfiguration());
  }

  private static void createConfiguration() {
    configuration = new Configuration();
  }
}
