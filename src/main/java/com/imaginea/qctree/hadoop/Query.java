package com.imaginea.qctree.hadoop;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.imaginea.qctree.Cell;
import com.imaginea.qctree.Property;
import com.imaginea.qctree.hadoop.QCTree.QCNode;

public class Query {

	private static final Log LOG = LogFactory.getLog(Query.class);
	private Cell cell;
	
	public void execute(QCTree tree, String query){
	
		String[] split = query.split(Property.QUERY_FILE_SEPERATOR);
	    this.cell = new Cell(split);
	    
	    QCNode node = tree.getRoot();

	    for (int idx = 0; idx < cell.getDimensions().length; ++idx) {
	      if (!cell.getDimensionAt(idx).equals(Cell.DIMENSION_VALUE_ANY)) {
	        node = searchRoute(node, idx, cell.getDimensionAt(idx));
	        if (node == null) {
	          break;
	        }
	      }
	    }

	    if (node == null) {
	      LOG.info("No results.");
	    } else {
	      while (node.getAggregates() == null) {
	        node = node.getLastChild();
	      }
	      System.out.println(node.getAggregates());
	      LOG.info(query);
	      LOG.info(node.getAggregates());
	      
	    }
	}
	
    
	private QCNode searchRoute(QCNode node, int dimIdx, String dimVal) {

	   if (node.getDimIdx() == dimIdx && node.getDimValue().equals(dimVal)) {
	      return node;
	   }

	   // node has a child with given dim name and value.
	   for (QCNode c : node.getChildren()) {
		   if (c.getDimIdx() == dimIdx && c.getDimValue().equals(dimVal)) {
			   return c;
		      }
	  }

	  // node has a ddlink with given dim name and value.
	  for (QCNode c : node.getDDLinks()) {
		  if (c.getDimIdx() == dimIdx && c.getDimValue().equals(dimVal)) {
		      return c;
		  }
	  }

	  QCNode lastChild = node.getLastChild();
	  if (lastChild != null && lastChild.getDimIdx() < dimIdx) {
	      return searchRoute(lastChild, dimIdx, dimVal);
	  } else {
	    return null;
	  }
	  
	}
}
