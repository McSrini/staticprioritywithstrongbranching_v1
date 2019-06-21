/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.mcmaster.staticprioritywithstrongbranching_v1;

/**
 *
 * @author tamvadss
 */
public class Parameters {
     
    public static final boolean USE_VARIABLE_PRIORITY_LIST = false;
    
    public static final int  USE_PRIORITY_LIST_FOR_HOURS = 1;
    
    public static final long PERF_VARIABILITY_RANDOM_SEED = 0;
    public static final java.util.Random  PERF_VARIABILITY_RANDOM_GENERATOR = new  java.util.Random  (PERF_VARIABILITY_RANDOM_SEED);
  
    
    //public static final String MIP_FILENAME = "2club200v.pre.lp";
    //public static final String MIP_FILENAME = "F:\\temporary files here\\2club200v.pre.lp";
    
    //public static final String MIP_FILENAME = "p6b.pre.lp";
    //public static final String MIP_FILENAME = "F:\\temporary files here\\p6b.pre.lp";
    
    
    //public static final String MIP_FILENAME = "ds.pre.lp";
    //public static final String MIP_FILENAME = "F:\\temporary files here\\ds.pre.lp";
     
    public static final String MIP_FILENAME = "probportfolio.pre.lp";
    //public static final String MIP_FILENAME = "F:\\temporary files here\\probportfolio.pre.lp";
     
     
}
