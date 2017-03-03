package org.workflowsim;

import java.util.List;

public final class GlobalStatic {

	private GlobalStatic() {
	}
	
	public static List<Task> taskList;
	
	public static WorkflowEngine wfEngine;
	
	public static List<Task> jobsDone;
}
