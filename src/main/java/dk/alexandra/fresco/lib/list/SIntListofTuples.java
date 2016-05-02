/**
 * 
 */
package dk.alexandra.fresco.lib.list;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import dk.alexandra.fresco.framework.value.SInt;

/**
 * @author mortenvchristiansen
 *
 */
public class SIntListofTuples {
  private List<SInt[]> theData = new ArrayList<SInt[]>();
	/**
	 * id is first element in tuple
	 */
  	public final int rowWidth;
  	
  	public SIntListofTuples add(SInt[] row){
  	    if (row.length!=rowWidth)
  	    	throw new RuntimeException("Row width wrong. Should be "+rowWidth);
  	    theData.add(row);	
  		return this;
  	}
  	
  	public SIntListofTuples remove(int index){
  	    theData.remove(index);	
  		return this;
  	}
  	
  	public SInt[] get(int index){
  		return theData.get(index);
  	}
  	
  	public List<SInt[]> getReadOnlyList(){
  		return Collections.unmodifiableList(theData);
  	}
  	
	public SIntListofTuples(int rowWidth) {
		this.rowWidth=rowWidth;
	}

}
