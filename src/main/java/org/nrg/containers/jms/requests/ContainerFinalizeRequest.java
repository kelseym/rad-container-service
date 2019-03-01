package org.nrg.containers.jms.requests;

import java.io.Serializable;


public class ContainerFinalizeRequest extends ContainerRequest implements Serializable {
   

	final private String destination="containerFinalizeRequest"; 
	
	private static final long serialVersionUID = 1388953760707461670L;
	private String exitCodeString;
	private boolean isSuccessful; 
	private String id;
	private String username;
	
	public ContainerFinalizeRequest(String exitCodeString, boolean isSuccessful, String id, String username,String workflowid) {
		 this.setExitCodeString(exitCodeString);
		 this.setSuccessful(isSuccessful); 
		 this.setId(id);
		 this.setUsername(username);
		 this.setWorkflowid(workflowid);
	}

	public String getExitCodeString() {
		return exitCodeString;
	}

	public void setExitCodeString(String exitCodeString) {
		this.exitCodeString = exitCodeString;
	}

	public boolean isSuccessful() {
		return isSuccessful;
	}

	public void setSuccessful(boolean isSuccessful) {
		this.isSuccessful = isSuccessful;
	}

	

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}
	
	public String getDestination() {
			return destination;
	}
}
