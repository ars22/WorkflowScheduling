package org.workflowsim.planning;

import java.awt.color.CMMException;
import java.security.AllPermission;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.cloudbus.cloudsim.Consts;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.distributions.WeibullDistr;
import org.workflowsim.CondorVM;
import org.workflowsim.FileItem;
import org.workflowsim.Task;
import org.workflowsim.utils.Parameters;


public class CheckpointHEFT extends BasePlanningAlgorithm {

    private Map<Task, Map<CondorVM, Double>> computationCosts;
    private Map<Task, Map<Task, Double>> transferCosts;
    private Map<Task, Double> rank;
    private Map<CondorVM, List<Event>> schedules;
    private Map<Task, Double> earliestFinishTimes;
    private List<Integer> failedVmIds;
    private double averageBandwidth;
    private static int repCount = 2;
    private static int mtbf = 60;
    private static int mttr;
    private static int checkpointDelay = 5;
    private static int checkpointOverhead = 2;
    
    private class Event {

        public double start;
        public double finish;

        public Event(double start, double finish) {
            this.start = start;
            this.finish = finish;
        }
    }

    private class TaskRank implements Comparable<TaskRank> {

        public Task task;
        public Double rank;

        public TaskRank(Task task, Double rank) {
            this.task = task;
            this.rank = rank;
        }

        @Override
        public int compareTo(TaskRank o) {
            return o.rank.compareTo(rank);
        }
    }

    public CheckpointHEFT(){
        computationCosts = new HashMap<>();
        transferCosts = new HashMap<>();
        rank = new HashMap<>();
        earliestFinishTimes = new HashMap<>();
        schedules = new HashMap<>();
        failedVmIds =  new ArrayList<Integer>();
        double noOfFailedVms = new WeibullDistr(new Random(10L), 1.5,2.4).sample();
		System.out.println("noOfFailedVms "+noOfFailedVms);
		for (int i=0; i<Math.floor(noOfFailedVms); i++){
			int x=(int)Math.floor(Math.random()*Parameters.getVmNum());
			failedVmIds.add(x);
			System.out.println(" FailedVms "+x);
		}
		mttr = (int)Math.floor(new WeibullDistr(new Random(10L), 11.5,12.5).sample());
		System.out.println("failing vms with mtbf as" + mtbf + "sec and mttr as " + Math.floor(mttr));
		
		
		System.out.println("set of failing vms ");
		
		for (int h=0; h<failedVmIds.size(); h++){
			System.out.print(failedVmIds.get(h) + "  ");
		}
		
    }

    /**
     * The main function
     */
    @Override
    public void run() {

        averageBandwidth = calculateAverageBandwidth();

        for (Object vmObject : getVmList()) {
            CondorVM vm = (CondorVM) vmObject;
            schedules.put(vm, new ArrayList<>());
        }

        // Prioritization phase
        calculateComputationCosts();
        calculateTransferCosts();
        calculateRanks();

        
        //calculating chk pt overhead
        
        for (int i=0; i<getTaskList().size(); i++){
			
			double averageComputationCost = 0.0;

			Task task = getTaskList().get(i);
			
	        for (Double cost : computationCosts.get(task).values()) {
	            averageComputationCost += cost;
	        }

	        averageComputationCost /= computationCosts.get(task).size();
			
			double runTime = averageComputationCost;
			double checkpointsForTask = runTime/checkpointDelay;
			getTaskList().get(i).setCloudletLength((long)checkpointsForTask * checkpointOverhead);
		}
        
        
        // Selection phase
        allocateTasks();
        //calculateSLR();
    }
    
    private void calculateSLR(){
    	List <Task> T = getTaskList();
    	double max_val=0;
    	double finish_time=0;
    	for (Task t: T){
    		max_val=Math.max(rank.get(t).doubleValue(),max_val);
    		System.out.println(t.getCloudletId()+"  "+earliestFinishTimes.get(t)+"  "+rank.get(t)+" "+t.getVmId());
    		finish_time=Math.max(finish_time, earliestFinishTimes.get(t));
    	}
    	System.out.println("*********************"+finish_time);
    	System.out.println("SLR : " + finish_time/max_val);
    	System.out.println("*********************"+max_val);
    }
    /**
     * Calculates the average available bandwidth among all VMs in Mbit/s
     *
     * @return Average available bandwidth in Mbit/s
     */
    private double calculateAverageBandwidth() {
        double avg = 0.0;
        for (Object vmObject : getVmList()) {
            CondorVM vm = (CondorVM) vmObject;
            avg += vm.getBw();
        }
        return avg / getVmList().size();
    }

    /**
     * Populates the computationCosts field with the time in seconds to compute
     * a task in a vm.
     */
    private void calculateComputationCosts() {
    	System.out.println("@@@@@@@@"+getTaskList().size());
        for (Task task : getTaskList()) {
            Map<CondorVM, Double> costsVm = new HashMap<>();
            for (Object vmObject : getVmList()) {
                CondorVM vm = (CondorVM) vmObject;
                if (vm.getNumberOfPes() < task.getNumberOfPes()) {
                    costsVm.put(vm, Double.MAX_VALUE);
                } else {
                    costsVm.put(vm,
                            task.getCloudletTotalLength() / vm.getMips());
                }
            }
            System.out.println("id : "+task.getCloudletId()+" "+task.copyVal+"  "+costsVm);
            computationCosts.put(task, costsVm);
        }
    }

    /**
     * Populates the transferCosts map with the time in seconds to transfer all
     * files from each parent to each child
     */
    private void calculateTransferCosts() {
        // Initializing the matrix
        for (Task task1 : getTaskList()) {
            Map<Task, Double> taskTransferCosts = new HashMap<>();
            for (Task task2 : getTaskList()) {
                taskTransferCosts.put(task2, 0.0);
            }
            transferCosts.put(task1, taskTransferCosts);
        }

        // Calculating the actual values
        for (Task parent : getTaskList()) {
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

    /**
     * Invokes calculateRank for each task to be scheduled
     */
    private void calculateRanks() {
        for (Task task : getTaskList()) {
            calculateRank(task);
        }
    }

    /**
     * Populates rank.get(task) with the rank of task as defined in the HEFT
     * paper.
     *
     * @param task The task have the rank calculates
     * @return The rank
     */
    private double calculateRank(Task task) {
        if (rank.containsKey(task)) {
            return rank.get(task);
        }

        double averageComputationCost = 0.0;
        System.out.println(computationCosts.containsKey(task)+" "+task.getCloudletId()+" "+task.copyVal);
        for (Double cost : computationCosts.get(task).values()) {
            averageComputationCost += cost;
        }

        averageComputationCost /= computationCosts.get(task).size();

        double max = 0.0;
        for (Task child : task.getChildList()) {
            double childCost = transferCosts.get(task).get(child)
                    + calculateRank(child);
            max = Math.max(max, childCost);
        }

        rank.put(task, averageComputationCost + max);

        return rank.get(task);
    }

    /**
     * Allocates all tasks to be scheduled in non-ascending order of schedule.
     */
    private void allocateTasks() {
        List<TaskRank> taskRank = new ArrayList<>();
        for (Task task : rank.keySet()) {
            taskRank.add(new TaskRank(task, rank.get(task)));
        }

        // Sorting in non-ascending order of rank
        Collections.sort(taskRank);
        for (TaskRank rank : taskRank) {
        	if (!rank.task.doneScheduling){
                allocateTask(rank.task);
                rank.task.doneScheduling=true;
        	}
//        	//System.out.println("Allocated "+rank.task.getCloudletId()+" at "+rank.task.getVmId());
        }

    }

    /**
     * Schedules the task given in one of the VMs minimizing the earliest finish
     * time
     *
     * @param task The task to be scheduled
     * @pre All parent tasks are already scheduled
     */
    private void allocateTask(Task task) {
        CondorVM chosenVM = null;
        double earliestFinishTime = Double.MAX_VALUE;
        double bestReadyTime = 0.0;
        double finishTime;

        for (Object vmObject : getVmList()) {
            CondorVM vm = (CondorVM) vmObject;
            double minReadyTime = 0.0;

            for (Task parent : task.getParentList()) {
                double readyTime = earliestFinishTimes.get(parent);
                if (parent.getVmId() != vm.getId()) {
                    readyTime += transferCosts.get(parent).get(task);
                }
                minReadyTime = Math.max(minReadyTime, readyTime);
            }

            finishTime = findFinishTime(task, vm, minReadyTime, false);

            if (finishTime < earliestFinishTime) {
                bestReadyTime = minReadyTime;
                earliestFinishTime = finishTime;
                chosenVM = vm;
            }
        }

        findFinishTime(task, chosenVM, bestReadyTime, true);
        earliestFinishTimes.put(task, earliestFinishTime);
        if (!task.doneScheduling)
        	task.setVmId(chosenVM.getId());
    }

    /**
     * Finds the best time slot available to minimize the finish time of the
     * given task in the vm with the constraint of not scheduling it before
     * readyTime. If occupySlot is true, reserves the time slot in the schedule.
     *
     * @param task The task to have the time slot reserved
     * @param vm The vm that will execute the task
     * @param readyTime The first moment that the task is available to be
     * scheduled
     * @param occupySlot If true, reserves the time slot in the schedule.
     * @return The minimal finish time of the task in the vmn
     */
    private double findFinishTime(Task task, CondorVM vm, double readyTime,
            boolean occupySlot) {
        List<Event> sched = schedules.get(vm);
        double computationCost = computationCosts.get(task).get(vm);
        double start, finish;
        int pos;

        if (sched.isEmpty()) {
            if (occupySlot) {
                sched.add(new Event(readyTime, readyTime + computationCost));
            }
            return readyTime + computationCost;
        }

        if (sched.size() == 1) {
            if (readyTime >= sched.get(0).finish) {
                pos = 1;
                start = readyTime;
            } else if (readyTime + computationCost <= sched.get(0).start) {
                pos = 0;
                start = readyTime;
            } else {
                pos = 1;
                start = sched.get(0).finish;
            }

            if (occupySlot) {
                sched.add(pos, new Event(start, start + computationCost));
            }
            return start + computationCost;
        }

        // Trivial case: Start after the latest task scheduled
        start = Math.max(readyTime, sched.get(sched.size() - 1).finish);
        finish = start + computationCost;
        int i = sched.size() - 1;
        int j = sched.size() - 2;
        pos = i + 1;
        while (j >= 0) {
            Event current = sched.get(i);
            Event previous = sched.get(j);

            if (readyTime > previous.finish) {
                if (readyTime + computationCost <= current.start) {
                    start = readyTime;
                    finish = readyTime + computationCost;
                }

                break;
            }
            if (previous.finish + computationCost <= current.start) {
                start = previous.finish;
                finish = previous.finish + computationCost;
                pos = i;
            }
            i--;
            j--;
        }

        if (readyTime + computationCost <= sched.get(0).start) {
            pos = 0;
            start = readyTime;

            if (occupySlot) {
                sched.add(pos, new Event(start, start + computationCost));
            }
            return start + computationCost;
        }
        if (occupySlot) {
            sched.add(pos, new Event(start, finish));
        }

        
        ////////////////////////////////////////////////
        
        //Case :1 
        /*
         * 
         * T1 <= R1 
         * 			Calculate no of checkpoints done 
         * 			Then compare with EST of others
         */
        if (occupySlot){
        	
	        if (failedVmIds.contains(vm.getId())){
	        	
	        	System.out.println("%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%");
	        	System.out.println("failed vm" + vm.getId() + "task : " + task.getCloudletId());
	        	System.out.println("%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%");
	        		    		
	        	
	        	int x = ((int)Math.floor(start))%mtbf;
	        	int y = ((int)Math.floor(start))/mtbf;
	        	
	        	
	        	//CASE 1
	        	if (x>=mttr){
        			double rAlpha = (y+1)*mtbf;
    	        	double rBeta = (y+1)*mtbf + mttr;
	        		if (finish>rAlpha){
	        			//calculate no of checkpoints done
	        			double jobDone = rAlpha - start;
	        			task.noOfCheckpointsDone = (int) jobDone/checkpointDelay;
	        			double minEST = (1<<63)-1;
	    				CondorVM vmWithMinEST = vm; 
	        			for (int l=0; l<getVmList().size(); l++){
	        				CondorVM vmConsidered = (CondorVM) getVmList().get(l);
	        				if (!failedVmIds.contains(l)){
	        					double EST = findFinishTime(task, vmConsidered, readyTime, false) - computationCosts.get(task).get(vmConsidered);
	        					if (EST < minEST){
	        						minEST = EST;
	        						vmWithMinEST = vmConsidered;
	        					}
	        				}
	        			}
	        			//Schedule on a different resource
	        			if (minEST + task.noOfCheckpointsDone * checkpointDelay < rBeta){
	        				Task newTask = new Task(task);
	        				newTask.copyVal = task.copyVal + 1;
	        				
	        				getTaskList().add(newTask);
	        				setTaskList(getTaskList());
	        				
	        				calculateComputationCosts();
	        				calculateTransferCosts();

	        				findFinishTime(newTask, vmWithMinEST, readyTime, true);
	        				
	        				newTask.doneScheduling = true;
	        				
	        				newTask.setVmId(vmWithMinEST.getId());
	        				
	        				System.out.println("%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%");
	        	        	System.out.println("Case 1 : on different" + "vm : " + vmWithMinEST.getId() + "task : " + task.getCloudletId());
	        	        	System.out.println("%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%");
	        			}
	        			//Schedule on the same resource 

	        			else{
	        				Task newTask = new Task(task);
	        				newTask.setCloudletLength(task.getCloudletLength() - task.noOfCheckpointsDone * checkpointDelay);
	        				newTask.copyVal = task.copyVal + 1;
	        				
	        				getTaskList().add(newTask);
	        				setTaskList(getTaskList());
	        				
	        				calculateComputationCosts();
	        				calculateTransferCosts();

	        				findFinishTime(newTask, vm, rBeta, true);
	        				
	        				newTask.doneScheduling = true;
	        				
	        				newTask.setVmId(vm.getId());
	        				
	        				
	        				System.out.println("%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%");
	        				System.out.println("Case 1 : on same" + "vm : " + vm.getId() + "task : " + task.getCloudletId());
	        				System.out.println("%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%");
	        			}
	        		}
	        		else{
	        			System.out.println("%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%");
        				System.out.println("Case 1 : finish done before ralpha...cool!!" );
        				System.out.println("%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%");
	        		}
	        	}
	        	//CASE 2
	        	if (x<mttr){
	        		double rAlpha = y*mtbf;
    	        	double rBeta = y*mtbf + mttr;
	        		double minEST = (1<<63)-1;
    				CondorVM vmWithMinEST = vm; 
        			for (int l=0; l<getVmList().size(); l++){
        				if (!failedVmIds.contains(l)){
        					CondorVM vmConsidered = (CondorVM) getVmList().get(l);
	        				double EST = findFinishTime(task, (CondorVM)getVmList().get(l), readyTime, false) - computationCosts.get(task).get(vmConsidered);
        					if (EST < minEST){
        						minEST = EST;
        						vmWithMinEST = (CondorVM) getVmList().get(l);
        					}
        				}
        			}
        			//Check on same resource 
        			if (rBeta <= minEST){
        				task.setVmId(vm.getId());
        				findFinishTime(task, vm, rBeta, true);
        				task.doneScheduling=true;
        				System.out.println("%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%");
        				System.out.println("Case 2 : on same" + "vm : " + vm.getId() + "task : " + task.getCloudletId());
        				System.out.println("%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%");
        			}
        			//Otherwise schedule on the other 
        			else{
        				task.setVmId(vmWithMinEST.getId());
        				findFinishTime(task, vmWithMinEST, readyTime, true);
        				task.doneScheduling=true;
        				System.out.println("%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%");
        				System.out.println("Case 2 : on diff" + "vm : " + vmWithMinEST.getId() + "task : " + task.getCloudletId());
        				System.out.println("%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%");
        			}
	        	}
	        }
        }
        ///////////////////////////////////////////////
        
        return finish;
    }
}
