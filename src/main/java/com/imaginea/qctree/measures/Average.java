package com.imaginea.qctree.measures;

import java.io.Serializable;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


public class Average implements Aggregable, Serializable {

  private static final Log LOG = LogFactory.getLog(Average.class);

  private int noOfrows;
  private double sum;

  @Override
  public void aggregate(List<Double> measures) {
    if (LOG.isDebugEnabled()) {
      LOG.debug("Computing Average Aggregate");
    }
    for (Double value : measures) {
      sum += value;
      ++noOfrows;
    }
  }

  @Override
  public void accumalate(Aggregable other) {
    Average otherAvg = (Average) other;
    this.sum += otherAvg.sum;
    this.noOfrows += otherAvg.noOfrows;
  }

  @Override
  public double getAggregateValue() {
    return sum / noOfrows;
  }

  @Override
  public String toString() {
    return String.valueOf(Aggregates.FORMAT.format(sum / noOfrows));
  }

}
