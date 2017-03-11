package org.workflowsim.scheduling.CPSO;

import java.util.Random;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.awt.GraphicsConfigTemplate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.DatacenterBroker;
import org.cloudbus.cloudsim.core.CloudSimTags;
import org.workflowsim.CondorVM;
import org.workflowsim.FileItem;
import org.workflowsim.GlobalStatic;
import org.workflowsim.Job;
import org.workflowsim.Task;
import org.workflowsim.WorkflowParser;
import org.workflowsim.WorkflowPlanner;
import org.workflowsim.WorkflowSimTags;
import org.workflowsim.utils.*;
import org.workflowsim.scheduling.*;


public class CPSOMain extends BaseSchedulingAlgorithm{
	static int c = 0;
	CParticle gbest, gbestCopy;	
	CParticle[] particle;
	int n;
	int dim,res,deadline=500,iterations=1;
	WorkflowPlanner planner;
	List<Task> taskList;
	List<CondorVM> vmList;
	TreeSet <CParticle> R;
	int gbestSame;
	int rSize;	

	@Override
	public void run(){

		
		try {
		if (c==1) return;
		c = 1; 
		taskList = GlobalStatic.wfEngine.getJobsList();
		vmList = getVmList();
		n = taskList.size();
		dim = n;
		res = getVmList().size();		
		gbest = new CParticle(dim, res);
		particle = new CParticle[n];
		Random rand=new Random();
		
		R = new TreeSet<CParticle>(new MyComp());
		
		//System.out.println("$$$$$$$$$$$$$$$$$$$$$$$$$$$$ N is : " + n + (taskList.get(0) instanceof org.workflowsim.Job));
		
		
		gbestSame = 0;
		rSize = (int) (0.1 * n);
		
		if (rSize <=1 ) rSize += 2;
		
		gbestCopy = gbest;
		CPSOScheduler s=new CPSOScheduler(this);
		
		
		for(int g=0;g<iterations;g++){
			for(int i=0;i<n;i++){
				particle[i]=new CParticle(dim, res);
				particle[i].id = i;
				//calculating fitness
				s.Schedulejob(particle[i].pos,i);
				//3.1
				
				if (i==0){
				particle[i].pbestTEC=particle[i].TEC;
				particle[i].pbestTET=particle[i].TET;
				for(int l=0;l<dim;l++)
					particle[i].pbestpos[l]=particle[i].pos[l];
				}
				else if(particle[i].pbestTEC > particle[i].TEC  && particle[i].TET<deadline && particle[i].pbestTET<deadline){
					particle[i].pbestTEC=particle[i].TEC;
					particle[i].pbestTET=particle[i].TET;
					for(int l=0;l<dim;l++)
						particle[i].pbestpos[l]=particle[i].pos[l];
					}
				else if(particle[i].pbestTET>deadline && particle[i].TET>deadline)
					{
					int pbest_violation=(int) (particle[i].pbestTET-100);
					int present_violation=(int) (particle[i].TET-100);
					
					if(pbest_violation>present_violation)
						{
						particle[i].pbestTEC=particle[i].TEC;
						particle[i].pbestTET=particle[i].TET;
						for(int l=0;l<dim;l++)
							particle[i].pbestpos[l]=particle[i].pos[l];
						}
					
					}
				else if(particle[i].pbestTET>deadline && particle[i].TET<deadline){
					particle[i].pbestTEC=particle[i].TEC;
					particle[i].pbestTET=particle[i].TET;
					for(int l=0;l<dim;l++)
						particle[i].pbestpos[l]=particle[i].pos[l];
				
				}
				
				//System.out.println("pbest="+particle[i].pbestTEC);
				//3.2
				if(gbest.TEC > particle[i].pbestTEC){
					//System.out.println("gbest changing");
					gbest=particle[i];
					gbestCopy = gbest;
					gbestSame = 0; 
					break;
					}

				
				//Check Pbest of the particle to see if it belongs tto set R
				//If So iterations <200 then add to R or else swap with min in R 
				
				//System.out.println("gbest.TEC="+gbest.TEC);
				
				//3.3
				//System.out.println("\n velocity is ");
				for(int k=0;k<dim;k++){
					int r1=((int)rand.nextInt((1-0)+1));
					int r2=((int)rand.nextInt((1-0)+1));
					//System.out.println("r1 and r2 is"+r1+r2);
					particle[i].vel[k]=(float) (0.1*(particle[i].vel[k]+2*r1*((particle[i].pbestpos[k]-particle[i].pos[k]))+2*r2*((gbest.pos[k]-particle[i].pos[k]))));
					//System.out.print(particle[i].vel[k]+"  ");
					}
				//System.out.println("\n Position is");
				for(int k=0;k<dim;k++){			
					particle[i].pos[k]=particle[i].pos[k]+(int)particle[i].vel[k];
					//System.out.print(particle[i].pos[k]+"  ");
					}
				
				}
			
				if (gbestCopy == gbest){
					//System.out.println("gbest is same !!!! " + gbest + gbestCopy + gbestSame);
					gbestSame++;
				}
				else {
					gbestSame = 0;
				}
				
				if (gbestSame == 5 && n!=0){
					//System.out.println("Gbest hasnt changed for 20 iterations");
					gbestSame = 0;
					for (int h=0; h<n; h++){
						if (R.size() < rSize){
							R.add(particle[h]);
						}
						else if (R.first().pbestTEC > particle[h].pbestTEC){
							R.remove(R.first());
							R.add(particle[h]);
						}
					}
					//System.out.println("Worst 10% of the population have values : " + R.size());
					for (CParticle cp : R){
						int currentId = cp.id;
						//System.out.println("Id : " + currentId + "TEC : " + cp.pbestTEC);
						//System.out.println("Pos vector for particle with id "  + currentId);
						for(int f=0;f<dim;f++){
							int val = (int) (100 * Math.random());
							cp.pos[f] = (val%2) * res;
							//System.out.println(f +  " : " + cp.pos[f]);
						}
						particle[currentId] = cp;
					}
				}
				
			}
		System.out.println("gbest.TEC="+gbest.TEC);
			for (int i=0; i<dim; i++){
				Cloudlet cloudlet = (Cloudlet) taskList.get(i);
				cloudlet.setVmId(gbest.pos[i]);
				//System.out.println(gbest.pos[i] + "#########");
				getScheduledList().add(cloudlet);
			}
		}
		catch(Exception e){
			e.printStackTrace();
		}
		
	}
}


class MyComp implements Comparator<CParticle>{
	
	
	@Override
	public int compare(CParticle cp1, CParticle cp2){
		return cp1.pbestTEC - cp2.pbestTEC;
	}
	
}




		