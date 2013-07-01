package eca.construct.experiment;

import eca.ss.enaction.Act;

/**
 * An experiment is an Action performed on a Phenomenon.
 * Experiments record the resulting acts, 
 * so as to predict what act will result from an action performed an a phenomenon. 
 * @author Olivier
 */
public interface Experiment {
	
	/**
	 * @return This experiment's label.
	 */
	public String getLabel();
	
	/**
	 * @param act the act to add to this experiment 
	 */
	public void addAct(Act act);
	
	/**
	 * @return The act most probably resulting from this experiment.
	 */
	public Act predictAct();

}