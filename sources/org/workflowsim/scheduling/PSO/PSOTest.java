package org.workflowsim.scheduling.PSO;

import java.util.List;

import org.cloudbus.cloudsim.Cloudlet;
import org.workflowsim.Job;
import org.workflowsim.Task;
import org.workflowsim.WorkflowParser;
import org.workflowsim.WorkflowPlanner;
import org.workflowsim.scheduling.BaseSchedulingAlgorithm;

public class PSOTest extends BaseSchedulingAlgorithm{

	WorkflowPlanner P;
	
	public PSOTest(WorkflowPlanner P) {
		this.P = P; 
	}
	@Override
	public void run() throws Exception {

		
		P.getWorkflowParser().parse();
		
		List<Task> L = P.getWorkflowParser().getTaskList();
		int size = L.size();
		System.out.println(size);
		
		for (int i=0; i<size; i++){
			System.out.println("######"+ "id = "+ ((Cloudlet) L.get(i)).getCloudletId() + "######");
			System.out.println("time = " + ((Cloudlet) L.get(i)).getCloudletLength()/1000.0);
			System.out.println("######"+ ((Task) L.get(i)).getChildList().size() + "######");
			int cLen = ((Task) L.get(i)).getChildList().size();
			List<Task> cList = ((Task) L.get(i)).getChildList();
			for (int j=0; j<cLen; j++){
				System.out.println("  @@ Cid = " + ((Cloudlet)cList.toArray()[j]).getCloudletId());
			}
		}
	}

	
}
