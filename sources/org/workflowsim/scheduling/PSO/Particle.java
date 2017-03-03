package org.workflowsim.scheduling.PSO;

import java.util.Random;

public class Particle {
	int dim,res;	
	int TEC,TET,pbestTEC,pbestTET;
	int[] pos,pbestpos;
	float[] vel;
	int[] LET,LST;
	static int[][] m;

	public Particle(int dim,int res){
		this.dim = dim;
		this.res = res;
		pbestTEC=99999999;
		TEC=99999999;
		pos=new int[dim];
		vel=new float[dim];
		pbestpos=new int[dim];
		LET=new int[res];
		LST=new int[res];
		m=new int[dim][3];
		System.out.println("\n");
		for(int j=0;j<dim;j++){
			Random rand=new Random();
			pos[j]= (int) Math.floor(Math.random()*res);
		}
	}
}
