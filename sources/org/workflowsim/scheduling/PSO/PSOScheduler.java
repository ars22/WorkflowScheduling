package org.workflowsim.scheduling.PSO;

import java.io.BufferedWriter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.Consts;
import org.workflowsim.CondorVM;
import org.workflowsim.FileItem;
import org.workflowsim.Job;
import org.workflowsim.Task;
import org.workflowsim.WorkflowSimTags;
import org.workflowsim.utils.Parameters;
import org.workflowsim.utils.ReplicaCatalog;
import org.workflowsim.scheduling.*;




public class PSOScheduler {

	PSOMain pso;
	
	public PSOScheduler(PSOMain pso){
		this.pso = pso;
		this.averageBandwidth = calculateAverageBandwidth();
		computationCosts = new HashMap<>();
        transferCosts = new HashMap<>();
		calculateComputationCosts();
		calculateTransferCosts();
	}
	
	int dim;
	int res;
	
	int n;

	int[][] exeTime;
	
	
	int[][] transferTime;
	
	int[][] graph;
	
	int[] LET;
	int[] ET;
	int[] LST;
	int[] ST;
	int[][] m;//mapping of tasks to resources
	int[] C;//change this
	int tou;
	int beta;
	
	int TEC=0,TET=0;
	
	List<Integer> R;
	List<Integer> M;
	List<Integer> parents;
	List<Integer> children;
	
double averageBandwidth;
	
	private Map<Task, Map<CondorVM, Double>> computationCosts;
    private Map<Task, Map<Task, Double>> transferCosts;
	
    private void calculateComputationCosts() {
    	//System.out.println("@@@@@@@@"+pso.taskList.size());
        for (Task task : pso.taskList) {
            Map<CondorVM, Double> costsVm = new HashMap<>();
            for (Object vmObject : pso.vmList) {
                CondorVM vm = (CondorVM) vmObject;
                if (vm.getNumberOfPes() < task.getNumberOfPes()) {
                    costsVm.put(vm, Double.MAX_VALUE);
                } else {
                    costsVm.put(vm,
                            task.getCloudletTotalLength() / vm.getMips());
                }
            }
            //System.out.println("id : "+task.getCloudletId()+" "+task.copyVal+"  "+costsVm);
            computationCosts.put(task, costsVm);
        }
    }

    /**
     * Populates the transferCosts map with the time in seconds to transfer all
     * files from each parent to each child
     */
    private void calculateTransferCosts() {
        // Initializing the matrix
        for (Task task1 : pso.taskList) {
            Map<Task, Double> taskTransferCosts = new HashMap<>();
            for (Task task2 : pso.taskList) {
                taskTransferCosts.put(task2, 0.0);
            }
            transferCosts.put(task1, taskTransferCosts);
        }

        // Calculating the actual values
        for (Task parent : pso.taskList) {
            for (Task child : parent.getChildList()) {
                transferCosts.get(parent).put(child,
                        calculateTransferCost(parent, child));
            }
        }
    }

    /**
     * Accounts the time in seconds necessary to transfer all files described
     * between parent and child
     *
     * @param parent
     * @param child
     * @return Transfer cost in seconds
     */
    private double calculateAverageBandwidth() {
        double avg = 0.0;
        for (Object vmObject : pso.vmList) {
            CondorVM vm = (CondorVM) vmObject;
            avg += vm.getBw();
        }
        return avg / pso.vmList.size();
    }
    
    private double calculateTransferCost(Task parent, Task child) {
        List<FileItem> parentFiles = parent.getFileList();
        List<FileItem> childFiles = child.getFileList();

        double acc = 0.0;

        for (FileItem parentFile : parentFiles) {
            if (parentFile.getType() != Parameters.FileType.OUTPUT) {
                continue;
            }

            for (FileItem childFile : childFiles) {
                if (childFile.getType() == Parameters.FileType.INPUT
                        && childFile.getName().equals(parentFile.getName())) {
                    acc += childFile.getSize();
                    break;
                }
            }
        }

        //file Size is in Bytes, acc in MB
        acc = acc / Consts.MILLION;
        // acc in MB, averageBandwidth in Mb/s
        return acc * 8 / averageBandwidth;
    }

    
	public int hasParent(int k,int[] ET){
		int flag=0;
		int maxet=0;
		int j=k;
		for(int i=0;i<n;i++)
		{if(graph[i][j]!=0)
			{
			flag++;
			parents.add(i);
			if(maxet<ET[i])
				maxet=ET[i];
			}			
		}
		if(flag!=0)
			return maxet;
		else 		
			return 0;
			
	}
	public void child(int k){
		int flag=0;
		int i=k;
		for(int j=0;j<n;j++){
			if(graph[i][j]!=0){
				flag++;
				children.add(j);
			}
		
		}
		
	}
	public int max(int[] ET){
		int max=ET[0];
		for(int i=0;i<ET.length;i++)
			if(max<ET[i])
				max=ET[i];
		return max;
	}
		
	public void Schedulejob(int[] pos,int particle_no){

		try {
			dim = pso.taskList.size();
			res = pso.vmList.size();
			
			n = dim;
	
			exeTime = new int[dim][res];
			
			
			transferTime=new int[dim][dim];
			
			graph=new int[dim][dim];
			
			LET=new int[res];
			ET=new int[dim];
			LST=new int[res];
			ST=new int[dim];
			m=new int[dim][res];//mapping of tasks to resources
			C=new int[res];//change this
			tou=1;
			beta = 1000000;
			
			TEC=0;TET=0;
			
			R=new ArrayList<Integer>();
			M=new ArrayList<Integer>();
			parents=new ArrayList<Integer>();
			children=new ArrayList<Integer>();
	
			
			for (int i=0; i<res; i++){
				C[i] = 1; 
			}
			
			for (int i=0; i<dim; i++){
				for (int j=0; j<res; j++){
					Task T = (Task) pso.taskList.get(i);
					CondorVM R = (CondorVM) pso.vmList.get(j);
					exeTime[i][j] = (int)Math.ceil(computationCosts.get(T).get(R)); 
				}
			}
			
			
			for (int i=0; i<dim; i++){
				for (int j=0; j<dim; j++){
					Task T1 = (Task) pso.taskList.get(i);
					Task T2 = (Task) pso.taskList.get(j);
					transferTime[i][j] = (int)Math.ceil(transferCosts.get(T1).get(T2)); 
				}
			}		
			for (int i=0; i<dim; i++){
				for (int j=0; j<dim; j++){
					graph[i][j] = transferTime[i][j];
				}
			}
			
			int exe=0,transfer,t=0,h,f,r;
			int[] PT=new int[dim];
			int[] ET=new int[dim];
		
			
			
			for(int i=0;i<n;i++){
			transfer=0;f=0;
			//4.1
			
			t=i;
			r=(int)pos[i];
			//4.2
			
			if(hasParent(t,ET)==0){
				//System.out.println(" i = " + i + "r = " + r + " dim = " + dim + " res = " + res);
				ST[i]=LET[r];
				}
			else{
				ST[i]=Math.max(hasParent(t,ET),LET[r]);
				}
			
			//4.3
			
			exe=exeTime[i][pos[i]];
			//4.4
			
			child(i);
			
			for(int l=0;l<children.size();l++){
				int d=children.get(l);
				int c=(int)pos[d];//children's resource
				if(c!=r){
						transfer+=transferTime[i][d];
				}
			}
			children.clear();
			//4.5
			
			PT[i]=exe+transfer;
			//4.6
			
			ET[i]=PT[i]+ST[i];
		
			//4.7
			
			m[i][0]=r;
			m[i][1]=ST[i];
			m[i][2]=ET[i];
			pso.particle[particle_no].m[i][0]=r;
			pso.particle[particle_no].m[i][1]=ST[i];
			pso.particle[particle_no].m[i][2]=ET[i];
		
			//4.9
		
			if(!R.contains((Integer)r)){
				f=1;
				LST[r]=Math.max(ST[i], 0);
				R.add((Integer)r);
			}
		
			//4.10
		
			if(f==1)
				LET[r]=PT[i]+LST[r];
			else
				LET[r]=ET[i];
				
			}
		
		
			for(int i=0;i<res;i++){
				pso.particle[particle_no].LET[i]=LET[i];
				pso.particle[particle_no].LST[i]=LST[i];
				TEC+=C[i]*((LET[i]-LST[i])/tou);
			}
		
			pso.particle[particle_no].TEC=TEC;
			
			TET=max(ET);
			pso.particle[particle_no].TET=TET;
		}
	
		catch(Exception e){
			e.printStackTrace();
		}
	
	}
	
}



