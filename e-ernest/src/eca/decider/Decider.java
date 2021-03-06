package eca.decider;

import tracing.ITracer;
import eca.ss.enaction.Enaction;

/**
 * A decider decides what interaction to try to enact next
 * when the previous decision cycle is over
 * based on the current state of sequential and spatial memory
 * and based on autotelic and interactional motivation
 * @author Olivier
 */
public interface Decider 
{
	/**
	 * @param tracer The tracer.
	 */
	public void setTracer(ITracer tracer);

	/**
	 * @param regularityThreshold The regularity sensibility threshold.
	 */
	public void setRegularityThreshold(int regularityThreshold);
	
	/**
	 * @param maxSchemaLength The maximum length of acts
	 */
	public void setMaxSchemaLength(int maxSchemaLength);

	/**
	 * @param enaction The current enaction.
	 * @return The next enaction.
	 */
	public Enaction decide(Enaction enaction);
	
	/**
	 * @param enaction The current enaction.
	 */
	public void carry(Enaction enaction);
}
