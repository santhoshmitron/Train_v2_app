package com.jsinfotech.Domain;

import java.util.List;

/**
 * Wrapper response for API that includes failsafe flag
 */
public class APIResponse {
	
	private List<API> apiList;
	private Boolean failsafe;
	
	public List<API> getApiList() {
		return apiList;
	}
	
	public void setApiList(List<API> apiList) {
		this.apiList = apiList;
	}
	
	public Boolean getFailsafe() {
		return failsafe;
	}
	
	public void setFailsafe(Boolean failsafe) {
		this.failsafe = failsafe;
	}
}

