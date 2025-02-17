package com.transferwise.common.gaffer.test;

import com.transferwise.common.baseutils.meters.cache.IMeterCache;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class MetricsTestHelper {

  private static final Pattern TAGS_PATTERN = Pattern.compile("tag\\((.*?)=(.*?)\\)");

  @Autowired
  @Getter
  private MeterRegistry meterRegistry;

  @Autowired
  private IMeterCache meterCache;

  public Counter getCounter(String name, List<Tag> tags) {
    return meterRegistry.find(name).tags(tags).counter();
  }

  public Counter getCounter(String name, String tags) {
    return getCounter(name, getTagsList(tags));
  }

  public double getCount(String name, String tags) {
    var counter = getCounter(name, tags);

    return counter == null ? 0 : counter.count();
  }

  public Gauge getGauge(String name, List<Tag> tags) {
    return meterRegistry.find(name).tags(tags).gauge();
  }

  public Gauge getGauge(String name, String tags) {
    return getGauge(name, getTagsList(tags));
  }

  public double getGaugeValue(String name, Double defaultValue, String tags) {
    var gauge = getGauge(name, tags);
    return gauge == null ? defaultValue : gauge.value();
  }

  protected List<Tag> getTagsList(String tagsSt) {
    var m = TAGS_PATTERN.matcher(tagsSt);
    var tags = new ArrayList<Tag>();

    while (m.find()) {
      tags.add(Tag.of(m.group(1), m.group(2)));
    }

    return tags;
  }

  public void cleanup() {
    for (Meter meter : meterRegistry.getMeters()) {
      if (!(meter instanceof Gauge)) {
        meterRegistry.remove(meter);
      }
    }
    meterCache.clear();
  }

}
