package org.nrg.containers.jms.requests;


abstract public class ContainerRequest   {
	String workflowid;

	public String getWorkflowid() {
		return workflowid;
	}

	public void setWorkflowid(String workflowid) {
		this.workflowid = workflowid;
	}
}
