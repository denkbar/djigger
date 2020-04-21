package io.djigger.monitoring.java.mbeans;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MBeanCollectorConfiguration implements Serializable {

    private static final long serialVersionUID = -7905632277382912658L;

    private List<String> mBeanAttributes = new ArrayList<String>();
    
    private List<String> mBeanExclusionAttributes = new ArrayList<String>();

    private List<MBeanOperation> mBeanOperations = new ArrayList<MBeanOperation>();

    public void addMBeanAttribute(String mBeanAttribute) {
        mBeanAttributes.add(mBeanAttribute);
    }
    
    public void addMBeanAttributes(String[] attributes) {
		mBeanAttributes.addAll(Arrays.asList(attributes));
	}

    public void addMBeanOperation(MBeanOperation mBeanOperation) {
        mBeanOperations.add(mBeanOperation);
    }
    
    public List<String> getmBeanAttributes() {
        return mBeanAttributes;
    }

    public List<MBeanOperation> getmBeanOperations() {
        return mBeanOperations;
    }

	public List<String> getmBeanExclusionAttributes() {
		return mBeanExclusionAttributes;
	}

	public void setmBeanExclusionAttributes(List<String> mBeanExclusionAttributes) {
		this.mBeanExclusionAttributes = mBeanExclusionAttributes;
	}

	public void addMBeanExclusionAttributes(String[] attributes) {
		mBeanExclusionAttributes.addAll(Arrays.asList(attributes));
	}

}
