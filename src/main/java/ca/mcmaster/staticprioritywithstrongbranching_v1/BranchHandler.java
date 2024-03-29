/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.mcmaster.staticprioritywithstrongbranching_v1;

import static ca.mcmaster.staticprioritywithstrongbranching_v1.Constants.ZERO;
import ilog.concert.IloException;
import ilog.concert.IloNumVar;
import ilog.cplex.IloCplex.BranchCallback;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author tamvadss
 */
public class BranchHandler extends BranchCallback{
    
    private Collection<IloNumVar> variableList ;
    public Map < String, Double > pseudoCostMapPrimary= new HashMap < String, Double > ( );
    public Map < String, Double > pseudoCostMapSecondary= new HashMap < String, Double > ( );
            
    public BranchHandler ( Collection<IloNumVar> vars) {
        variableList= vars;
    }
 
    protected void main() throws IloException {
        if (getNbranches() > ZERO) {
         
            for (IloNumVar var : variableList ){
                double downPseudoCost = Math.abs ( getDownPseudoCost(  var));
                double upPseudoCost =  Math.abs ( getUpPseudoCost(  var));
                double pseudoCostMax = Math.max (downPseudoCost,upPseudoCost );
                double pseudoCostMin = Math.min (downPseudoCost,upPseudoCost );
                pseudoCostMapPrimary.put (var.getName(), pseudoCostMax) ;
                pseudoCostMapSecondary.put (var.getName(), pseudoCostMin) ;
                
            }
            
            //done
            abort ();
        }
    }
    
}
