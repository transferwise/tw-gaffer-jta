package com.transferwise.common.gaffer.util;

import java.lang.management.ManagementFactory;
import javax.management.MBeanServer;
import javax.management.ObjectName;

public class MBeanUtil {

  private static final FormatLogger log = new FormatLogger(MBeanUtil.class);

  public static void registerMBeanQuietly(Object manBean, String objectNameSt) {
    MBeanServer manBeanServer = ManagementFactory.getPlatformMBeanServer();
    try {
      ObjectName objectName = new ObjectName(objectNameSt);
      boolean registered = manBeanServer.isRegistered(objectName);

      log.info("%s MBean for '%s'.", registered ? "Reregistering" : "Registering", objectNameSt);

      if (registered) {
        manBeanServer.unregisterMBean(objectName);
      }
      manBeanServer.registerMBean(manBean, new ObjectName(objectNameSt));
    } catch (Exception e) {
      log.error("Failed to register MBean '%s'." + objectNameSt, e);
    }
  }
}
