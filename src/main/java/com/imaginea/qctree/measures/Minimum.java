package com.imaginea.qctree.measures;

import java.io.Serializable;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class Minimum implements Aggregable, Serializable  {

  private static final Log LOG = LogFactory.getLog(Minimum.class);
  private double min = Double.MAX_VALUE;

  @Override
  public void aggregate(List<Double> measures) {
    if (LOG.isDebugEnabled()) {
      LOG.debug("Computing Minimum Aggregate");
    }
    for (double value : measures) {
      this.min = Math.min(this.min, value);
    }
  }

  @Override
  public double getAggregateValue() {
    return this.min;
  }

  @Override
  public void accumalate(Aggregable other) {
    Minimum otherMin = (Minimum) other;
    this.min = Math.min(this.min, otherMin.min);
  }

  @Override
  public String toString() {
    return String.valueOf(Aggregates.FORMAT.format(min));
  }
}
