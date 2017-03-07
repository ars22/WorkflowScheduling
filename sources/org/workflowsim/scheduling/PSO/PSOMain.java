package org.workflowsim.scheduling.PSO;

import java.util.Random;
import java.util.ArrayList;
import java.util.List;
import org.cloudbus.cloudsim.Cloudlet;
import org.workflowsim.CondorVM;
import org.workflowsim.FileItem;
import org.workflowsim.GlobalStatic;
import org.workflowsim.Job;
import org.workflowsim.Task;
import org.workflowsim.WorkflowPlanner;
import org.workflowsim.WorkflowSimTags;
import org.workflowsim.utils.ReplicaCatalog;
import org.workflowsim.scheduling.*;


public class PSOMain extends BaseSchedulingAlgorithm{

	Particle gbest;	
	Particle[] particle;
	int n;
	int dim,res,deadline=500,iterations=100;
	WorkflowPlanner planner;
	List<Task> taskList;
	List<CondorVM> vmList;
	
	
	
	public PSOMain(WorkflowPlanner P){
		super();
		this.planner = P;
	}
	

	@Override
	public void run(){

		
		try {
	
		taskList = getCloudletList();
		vmList = getVmList();
		n = taskList.size();
		System.out.println("Task size :" + n);
		
		dim = n;
		res = getVmList().size();		
		gbest = new Particle(dim, res);
		particle = new Particle[n];
		Random rand=new Random();
		PSOScheduler s=new PSOScheduler(this);
		
		for(int g=0;g<iterations;g++){
			for(int i=0;i<n;i++){
				particle[i]=new Particle(dim, res);
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
					break;
					}
				
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
		