/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.mcmaster.staticprioritywithstrongbranching_v1;

import static ca.mcmaster.staticprioritywithstrongbranching_v1.Constants.*; 
import static ca.mcmaster.staticprioritywithstrongbranching_v1.Parameters.MIP_FILENAME;
import static ca.mcmaster.staticprioritywithstrongbranching_v1.Parameters.PERF_VARIABILITY_RANDOM_GENERATOR;
import static ca.mcmaster.staticprioritywithstrongbranching_v1.Parameters.USE_VARIABLE_PRIORITY_LIST;
import ilog.concert.IloException;
import ilog.concert.IloLPMatrix;
import ilog.concert.IloNumVar;
import ilog.cplex.IloCplex;
import java.io.File;
import static java.lang.System.exit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.RollingFileAppender;
import static ca.mcmaster.staticprioritywithstrongbranching_v1.Parameters.USE_PRIORITY_LIST_FOR_HOURS;

/**
 *
 * @author tamvadss
 */
public class SPSB_Driver {
        
    private static Logger logger = Logger.getLogger(SPSB_Driver.class);
    private  static  IloCplex cplex  ;

    static {
        logger.setLevel(LOGGING_LEVEL );
        PatternLayout layout = new PatternLayout("%5p  %d  %F  %L  %m%n");     
        try {
            RollingFileAppender rfa =new  RollingFileAppender(layout,LOG_FOLDER+SPSB_Driver.class.getSimpleName()+ LOG_FILE_EXTENSION);
            rfa.setMaxBackupIndex(SIXTY);
            logger.addAppender(rfa);
            logger.setAdditivity(false);
        } catch (Exception ex) {
            ///
            System.err.println("Exit: unable to initialize logging"+ex);       
            exit(ONE);
        }
    } 
    
    public static void main(String[] args) throws Exception{
        //read in the MIP
        cplex =  new IloCplex();
        cplex.importModel(  MIP_FILENAME);
                
        //strong branch till ready to branch root node, use single thread for this "ramp-up"
        cplex.setParam( IloCplex.Param.Threads,  ONE);
        cplex.setParam( IloCplex.Param.MIP.Strategy.VariableSelect  ,  THREE);
        cplex.setParam( IloCplex.Param.MIP.Limits.StrongCand  , BILLION );
        cplex.setParam( IloCplex.Param.MIP.Limits.StrongIt ,  BILLION );
        //using callback , find the largest absolute value pseudo-cost for every variable
        //Note that we only consider the larger of the up or down pseudo cost
        BranchHandler branchHandler = new BranchHandler (getVariables(cplex).values());
        cplex.use( branchHandler);        ;
        logger.info ("Starting strong branching ...") ;
        cplex.solve ( );
        logger.info ("Completed strong branching.") ;
        
        Map < String, Double > pseudoCostMapPrimary = branchHandler.pseudoCostMapPrimary;
        Map < String, Double > pseudoCostMapSecondary = branchHandler.pseudoCostMapSecondary;
        Map < String, Integer > priorityMap =  initializePriorities (  pseudoCostMapPrimary, pseudoCostMapSecondary);
        
        
        //close this cplex
        cplex.end();
        
        //read in the MIP again, and this time set static priority list for branching        
        cplex =  new IloCplex();
        cplex.importModel(  MIP_FILENAME);
        Map<String, IloNumVar> newVars = getVariables (  cplex);
        
        if (USE_VARIABLE_PRIORITY_LIST){
            for ( IloNumVar newVar : newVars.values()) {
                cplex.setPriority(  newVar , priorityMap.get (newVar.getName()) );
            }
        }
        
        
        //solve MIP to completion using dynamic search
        cplex.setParam(IloCplex.Param.MIP.Strategy.File,  FILE_STRATEGY);  
        cplex.setParam( IloCplex.Param.Threads,  FOUR*FOUR*TWO);
        for (int hours=ZERO; ; hours ++){            
            if (isHaltFilePresent()) break;
            cplex.setParam( IloCplex.Param.TimeLimit, SIXTY *SIXTY);
            cplex.solve ( );
            logger.info ("," + cplex.getBestObjValue() +
                         ","  + cplex.getObjValue() + 
                         ","  +cplex.getNnodes64() + 
                         ","  +cplex.getNnodesLeft64()) ;
            
            if (cplex.getStatus().equals( IloCplex.Status.Infeasible)) break;
            if (cplex.getStatus().equals( IloCplex.Status.Optimal)) break;
            
            if (USE_PRIORITY_LIST_FOR_HOURS==hours && Parameters.USE_VARIABLE_PRIORITY_LIST) {
                final IloNumVar[] emptyVarArray = new IloNumVar[]{};
                cplex.delPriorities(  newVars.values().toArray(emptyVarArray));
            }
        }
        
        logger.info("Solution status : "+ cplex.getStatus()) ;
        System.out.println("Solution status : "+ cplex.getStatus()) ;
        
    }
    
    
    
        
    private static Map<String, IloNumVar> getVariables (IloCplex cplex) throws IloException{
        Map<String, IloNumVar> result = new HashMap<String, IloNumVar>();
        IloLPMatrix lpMatrix = (IloLPMatrix)cplex.LPMatrixIterator().next();
        IloNumVar[] variables  =lpMatrix.getNumVars();
        for (IloNumVar var :variables){
            result.put(var.getName(),var ) ;
        }
        return result;
    }
    
    private static Map < String, Integer > initializePriorities ( 
            Map < String, Double > pseudoCostMapPrimary,
            Map < String, Double > pseudoCostMapSecondary) {
        Map < String, Integer > result = new HashMap < String, Integer > ();
        
        TreeMap <  Double, List<String> > invertedPseudoCostMap = new TreeMap <  Double, List<String> >();
        for (Map.Entry < String, Double > entry :pseudoCostMapPrimary.entrySet()){
            double thisVal = - entry.getValue();
            List<String> currentList = invertedPseudoCostMap.get (thisVal) ;
            if (currentList==null)  currentList = new ArrayList();
            currentList.add (entry.getKey());
            invertedPseudoCostMap.put (thisVal, currentList) ;
        }
        
        int currentPriority = ONE;
        for (Map.Entry<  Double, List<String> > entry : invertedPseudoCostMap.entrySet()){
            List<String> vars = entry.getValue() ;
             
            //arange vars by decreasing order of secondary psedudo costs
            List<String> reArrangedVars = oredrBySecondaryPseduoCost(vars,   pseudoCostMapSecondary) ;
            
            for (String varName : reArrangedVars ){
                result.put (varName, currentPriority) ;
                currentPriority++;
            }        
           
        }
        
        return result;
    }
    
    private static List<String>   oredrBySecondaryPseduoCost( List<String> vars,  Map < String, Double > pseudoCostMapSecondary) {
        List<String> reArrangedVars = new ArrayList<String> ();
        
        TreeMap <  Double, List<String> > invertedPseudoCostMap = new TreeMap <  Double, List<String> >();
        for (Map.Entry < String, Double > entry :pseudoCostMapSecondary.entrySet()){
            
            if (! vars.contains (entry.getKey()) ) continue;
            
            double thisVal = - entry.getValue();
            List<String> currentList = invertedPseudoCostMap.get (thisVal) ;
            if (currentList==null)  currentList = new ArrayList();
            currentList.add (entry.getKey());
            invertedPseudoCostMap.put (thisVal, currentList) ;
        }
        
        for (List < String > orderedVars :invertedPseudoCostMap.values()){
            
            Collections.shuffle(orderedVars ,  PERF_VARIABILITY_RANDOM_GENERATOR);
            
            reArrangedVars.addAll(orderedVars );
        }
        
        return  reArrangedVars;
    }
    
    private static boolean isHaltFilePresent (){
        File file = new File("haltfile.txt");         
        return file.exists();
    }
        
}
