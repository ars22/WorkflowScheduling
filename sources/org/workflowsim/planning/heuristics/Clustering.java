package org.workflowsim.planning.heuristics;

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

import org.workflowsim.CondorVM;
import org.workflowsim.FileItem;
import org.workflowsim.Task;
import org.workflowsim.utils.Parameters;

import java.util.Map;

import org.cloudbus.cloudsim.Consts;
import org.encog.ml.MLCluster;
import org.encog.ml.data.MLData;
import org.encog.ml.data.MLDataPair;
import org.encog.ml.data.MLDataSet;
import org.encog.ml.data.basic.BasicMLData;
import org.encog.ml.data.basic.BasicMLDataPair;
import org.encog.ml.data.basic.BasicMLDataSet;
import org.encog.ml.kmeans.KMeansClustering;

import org.workflowsim.planning.heuristics.SHA1;

public class Clustering {

	private List<Task> taskList;
	private List<CondorVM> vmList;
	private Map<String, List<Task>> featureToId;
	private Map<Task, Double> computationCosts;
	private Map<Task, Double> transferCosts;
	private double averageBandwidth;
	
	public Clustering(List<Task> taskList, List<CondorVM> vmList){
		this.taskList = taskList;
		this.vmList = vmList;
		System.out.println("^^^^^^^^^^^" + (taskList == null) + (vmList == null));
		computationCosts = new HashMap<Task, Double>();
		transferCosts = new HashMap<Task, Double>();
		featureToId = new HashMap<String, List<Task>>();
		this.averageBandwidth = calculateAverageBandwidth();
		calculateComputationCosts();
		calculateTransferCosts();
	}
	
	public void generateClusters(){
		
		BasicMLDataSet set = new BasicMLDataSet();
		
		double[] L = new double[5];
		
		for (Task task : taskList){
			
			
			L[0] = (double)computationCosts.get(task);
			L[1] = (double)task.getParentList().size();
			L[2] = (double)task.getChildList().size();
			L[3] = (double)task.getPriority();
			L[4] = (double)transferCosts.get(task)*100;
			
			set.add(new BasicMLData(L));
			System.out.println("##: " + calcHash(L) + "  " +L[0]);
			String hashVal = calcHash(L);
			List <Task> chainedTasks = featureToId.get(hashVal);
			if (chainedTasks == null){
				chainedTasks = new ArrayList<Task>();
				chainedTasks.add(task);
			}
			else{
				chainedTasks.add(task);
			}
			featureToId.put(hashVal,chainedTasks);
		}
		
		
		KMeansClustering kmeans = new KMeansClustering(taskList.size()/3, set);
		
		kmeans.iteration(100);
		
		int k = 0,k_max=0;
		Map <Integer, List<MLCluster> > map =  new HashMap<Integer, List<MLCluster> >();
		
		for (MLCluster cluster : kmeans.getClusters()){
			System.out.println("cluster : " + k + " size : " + cluster.size());
			k_max = Math.max(cluster.size(),k_max);
			
			int sz = cluster.size();
			
			if (sz>0){
				if (map.containsKey(sz)){
					map.get(sz).add(cluster);
				}
				else{
					map.put(sz, new ArrayList<MLCluster>());
					map.get(sz).add(cluster);					
				}
			}
			
			/*
			final MLDataSet ds = cluster.createDataSet();
	        final MLDataPair pair = BasicMLDataPair.createPair(
	                ds.getInputSize(), ds.getIdealSize());
	        
	        for (int j = 0; j < ds.getRecordCount(); j++) {
	            ds.getRecord(j, pair);
	            System.out.println(Arrays.toString(pair.getInputArray()));

	        }*/
			for (int i=0; i<cluster.size(); i++){
				MLData D = cluster.get(i);
				
				double[] featureVector = D.getData();
				for (double l: featureVector){
					System.out.println(l+" ");
				}
		
				List<Task> revHash = featureToId.get(calcHash(featureVector));
				for (Task T: revHash)
					System.out.println("&&  " + T.getCloudletId() );
			}
			k++;
		}
		
		
		System.out.println("k_max : " + k_max);
		
		List<Task> list1 = new ArrayList<>();
		List<Task> list2 = new ArrayList<>();
		List<Task> list3 = new ArrayList<>();
		
		
		BasicMLDataSet set2 = new BasicMLDataSet();
		
		int h=0;
		for (int i: map.keySet()){
			double[] L2 = new double[map.keySet().size()];
			L2[h++] = (double)i;
			set2.add(new BasicMLData(L2));
		}
		
			
		
		KMeansClustering kmeans2 = new KMeansClustering(3, set2);
		
		kmeans2.iteration(100);
		
		int p=0,p_max=0;
		for (MLCluster cluster : kmeans2.getClusters()){
			System.out.println("cluster : " + p + " size : " + cluster.size());
			p_max = Math.max(cluster.size(),p_max);
			
			
			for (int i=0; i<cluster.size(); i++){
				MLData D = cluster.get(i);
				
				double[] sizeVector = D.getData();

				for (double y: sizeVector){
					int x = (int)(y);
					if (x==0) continue;
					List<MLCluster> cList = map.get(x);
					for (MLCluster cluster_in : cList){
						
							for (int j=0; j<cluster_in.size(); j++){
								MLData data = cluster_in.get(j);
							
								double[] featureVector = data.getData();
								List <Task> revHash = featureToId.get(calcHash(featureVector));
								if (revHash.size() == 0){
									System.out.println("@@@@@@@@@@@@@@@@@@@ Size : 0 " + calcHash(featureVector) + featureToId.containsKey(calcHash(featureVector)) );
								}
								Task task = revHash.get(0);
								revHash.remove(task);
								if (p==0) list1.add(task);
								else if (p==1) list2.add(task);
								else if (p==2) list3.add(task);
							}
						
					}

				}
				
			}
			p++;
		}
		System.out.println("p_max : "+p_max);
		List <List <Task> > all = new ArrayList< List<Task> >();
		all.add(list1);
		all.add(list2);
		all.add(list3);
		
		Collections.sort(all, new Comparator<  List<Task> >(){
				
				@Override
				public int compare(List<Task> L1, List<Task> L2){
					return L1.size() < L2.size() ? 1 : -1;
				}
				
		});
	
		int rep=0;
		for (List<Task> list : all){
			System.out.println("LIST SZ : " + list.size() + "REPCOUNT : " + rep);
			for (Task T: list){
				T.repCount = rep;
				System.out.println("$$  " + T.getCloudletId() + " : " + T.repCount);
			}
			rep++;
		}
		
	}
	
	private String calcHash(double[] L){
		String S = "";
		for (int u=0; u<5; u++){
			int K = (int) Math.floor(L[u]);
			S = S + Integer.toString(K);
		}
		String hash = "";
		try {
			hash = SHA1.SHA1(S);
		} catch (NoSuchAlgorithmException | UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return hash;
	}
	
	private void calculateComputationCosts() {
        for (Task task : taskList) {
        	double avgCost = 0.0;
        	int cnt = 0;
        	for (Object vmObject : vmList) {
                CondorVM vm = (CondorVM) vmObject;
                avgCost += task.getCloudletTotalLength() / vm.getMips();
                cnt++;
        		}
        	if (cnt!=0) avgCost /= cnt;
        	else avgCost = Double.MAX_VALUE;
        	computationCosts.put(task, avgCost);
        	}
        }

	    /**
	     * Populates the transferCosts map with the time in seconds to transfer all
	     * files from each parent to each child
	     */
	    private void calculateTransferCosts() {
	        for (Task task1 : taskList) {
	            transferCosts.put(task1, 0.0);
	        }

	        // Calculating the actual values
	        for (Task child : taskList) {
	        	double avgCost = 0.0;
	        	int k = child.getParentList().size();
	            for (Task parent : child.getParentList()) {
                avgCost += calculateTransferCost(parent, child);
	            }
	           if (k!=0) avgCost /= k;
		       transferCosts.put(child, avgCost);
	        }
	    }
	    
	    private double calculateAverageBandwidth() {
	        double avg = 0.0;
	        for (Object vmObject : vmList) {
	            CondorVM vm = (CondorVM) vmObject;
	            avg += vm.getBw();
	        }
	        return avg / vmList.size();
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
	
}
