package org.workflowsim.scheduling.CPSO;

import java.util.Random;

public class CParticle {
	int id;
	int dim,res;	
	int TEC,TET,pbestTEC,pbestTET;
	int[] pos,pbestpos;
	float[] vel;
	int[] LET,LST;
	static int[][] m;

	public CParticle(int dim,int res){
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
		for(int j=0;j<dim;j++){
			pos[j]= (int) Math.floor(Math.random()*res);
		}
	}
}
