package imos2;

import java.util.ArrayList;

/**
 * A sensorimotor pattern of interaction of Ernest with its environment 
 * @author Olivier
 */
public interface IInteraction 
{
	public String getMoveLabel();
	public String getLabel();
	public boolean getPrimitive();
	public int getEnactionValue();
	public void setEnactionWeight(int enactionWeight);
	public int getEnactionWeight();
	public IInteraction getPreInteraction();
	public IInteraction getPostInteraction();
	public int getLength();
	
	/**
	 * @param step The current step during the enaction of this interaction.
	 */
	public void setStep(int step);
	
	/**
	 * @return The current step during the enaction of this interaction.
	 */
	public int getStep();
	
	/**
	 * @param prescriber The interaction that prescribes the enaction of this interaction.
	 */
	public void setPrescriber(IInteraction prescriber);
	
	/**
	 * @return The interaction that prescribes the enaction of this interaction.
	 */
	public IInteraction getPrescriber();
	
	/**
	 * Prescribe this interaction's preInteraction
	 * This method applies recursively to all this interaction's sub interactions.
	 * @return The primitive interaction at the bottom of the hierarchy
	 */
	public IInteraction prescribe();

	/**
	 * Update the prescriber if this interaction was enacted
	 * @return The next top level interaction to enact or null if the current enaction is over.
	 */
	public IInteraction updatePrescriber();
	
	/**
	 * Clear the prescriber hierarchy
	 */
	public void terminate();
	
	/**
	 * @param interaction The actually enacted interaction
	 * @return The number of time 
	 */
	public IInteraction addAlternateInteraction(IInteraction interaction);
	public ArrayList<IInteraction> getAlternateInteractions();
}