package com.imaginea.qctree.measures;

import java.util.List;

public interface Aggregable {
	
  public void aggregate(List<Double> measures);

  public double getAggregateValue();
  
  public void accumalate(Aggregable other);
}
