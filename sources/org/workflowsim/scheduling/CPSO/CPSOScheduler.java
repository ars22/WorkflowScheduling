package org.workflowsim.scheduling.CPSO;

import java.io.BufferedWriter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.cloudbus.cloudsim.Cloudlet;
import org.workflowsim.CondorVM;
import org.workflowsim.FileItem;
import org.workflowsim.Job;
import org.workflowsim.Task;
import org.workflowsim.WorkflowSimTags;
import org.workflowsim.utils.ReplicaCatalog;
import org.workflowsim.scheduling.*;




public class CPSOScheduler {

	CPSOMain pso;
	
	public CPSOScheduler(CPSOMain pso){
		this.pso = pso;
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
			res = pso.getVmList().size();
			
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
					exeTime[i][j] = (int)((Cloudlet) pso.taskList.get(i)).getCloudletLength(); 
				}
			}
			
			
			for (int i=0; i<dim; i++){
				for (int j=0; j<dim; j++){
					transferTime[i][j] = 0; 
				}
			}	
			
			for (int i=0; i<dim; i++){
				Task task = (Task) pso.taskList.get(i); 
				int taskLen = task.getChildList().size();
				for (int j=0; j<taskLen; j++){
					Task tChild = task.getChildList().get(j);
					int k = tChild.getCloudletId();
					k = (k-1+n)%n;
					transferTime[i][k] = (int) (Math.random()*10);
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



