package spas;

import java.util.ArrayList;
import java.util.List;
import ernest.Ernest;
import ernest.ITracer;

/**
 * A bundle of sensory stimulations. 
 * So far, a bundle is defined by its visual and its tactile stimulation. 
 * Other stimulations are optional.
 * A bundle may correspond to a physical object according to Hume's bundle theory (http://en.wikipedia.org/wiki/Bundle_theory)
 * @author Olivier
 */
public class Bundle implements IBundle {

	ISalience m_visualSalience;
	IStimulation m_visualStimulation;
	IStimulation m_tactileStimulation;
	IStimulation m_kinematicStimulation;
	IStimulation m_gustatoryStimulation;

	int m_lastTimeBundled;
	
	Bundle(IStimulation visualStimulation, IStimulation tactileStimulation, IStimulation kinematicStimulation, IStimulation gustatoryStimulation)
	{
		m_visualStimulation = visualStimulation;
		m_tactileStimulation = tactileStimulation;
		m_kinematicStimulation = kinematicStimulation;//Ernest.STIMULATION_KINEMATIC_FORWARD;
		m_gustatoryStimulation = gustatoryStimulation; //Ernest.STIMULATION_GUSTATORY_NOTHING;
	}
	
	public int getValue()
	{
		int value = 0;
		if (m_visualStimulation.equals(Ernest.STIMULATION_VISUAL_UNSEEN))
			value = m_tactileStimulation.getValue();
		else 
			value = m_visualStimulation.getValue();
		
		return value;
	}
	
	public String getHexColor() 
	{
		String s = "";
		if (m_visualStimulation.equals(Ernest.STIMULATION_VISUAL_UNSEEN))
			s = m_tactileStimulation.getHexColor();
		else 
			s = m_visualStimulation.getHexColor();
			
		return s;
	}
	
	public IStimulation getVisualStimulation() 
	{
		return m_visualStimulation;
	}
	
	public void setVisualStimulation(IStimulation stimulation) 
	{
		m_visualStimulation = stimulation;
	}
	
	public IStimulation getTactileStimulation() 
	{
		return m_tactileStimulation;
	}
	public void setGustatoryStimulation(IStimulation  gustatoryStimulation) 
	{
		m_gustatoryStimulation = gustatoryStimulation;
	}
	
	public IStimulation getGustatoryStimulation() 
	{
		return m_gustatoryStimulation;
	}
	
	public void setKinematicStimulation(IStimulation kinematicStimulation)
	{
		m_kinematicStimulation = kinematicStimulation;
	}
	
	public IStimulation getKinematicStimulation()
	{
		return m_kinematicStimulation;
	}
	
	public void setLastTimeBundled(int clock)
	{
		m_lastTimeBundled = clock;
	}

	/**
	 * ATTRACTIVENESS_OF_FISH (400) if this bundle's gustatory stimulation is STIMULATION_TASTE_FISH.
	 * ATTRACTIVENESS_OF_FISH + 10 (410) if the fish is touched.
	 * Otherwise ATTRACTIVENESS_OF_UNKNOWN (200) if this bundle has been forgotten,
	 * or 0 if this bundle has just been visited.
	 * @param clock Ernest's current clock value.
	 * @return This bundle's attractiveness at the given time.
	 */
	public int getAttractiveness(int clock) 
	{
		// If the bundle has a kinematic stimulation of bump.
		if (Ernest.STIMULATION_KINEMATIC_BUMP.equals(m_kinematicStimulation))
			return Ernest.ATTRACTIVENESS_OF_BUMP;

		// If the bundle has a tactile stimulation of hard.
		if (Ernest.STIMULATION_TOUCH_WALL.equals(m_tactileStimulation))
			return Ernest.ATTRACTIVENESS_OF_HARD - 10; // prefer a bundle salience than a mere touch salience.

		// The bundle of touching a fish
		//if (m_visualStimulation.equals(PersistenceSystem.BUNDLE_GRAY_FISH.getVisualStimulation()))
		//	return Ernest.ATTRACTIVENESS_OF_FISH + 10;
		
		// If the bundle has a gustatory stimulation of fish 
		else if (Ernest.STIMULATION_GUSTATORY_FISH.equals(m_gustatoryStimulation))
		{
			if (m_visualStimulation.equals(Ernest.STIMULATION_VISUAL_UNSEEN))
				// Fish that are touched are more attractive 
				return Ernest.ATTRACTIVENESS_OF_FISH + 10;
			else
				return Ernest.ATTRACTIVENESS_OF_FISH;
		}
		
		else if (clock - m_lastTimeBundled > Ernest.PERSISTENCE)// && !m_visualStimulation.getColor().equals(Ernest.COLOR_WALL))
			return Ernest.ATTRACTIVENESS_OF_UNKNOWN ;
		
		else
			return 0;
	}

	/**
	 * Bundles are equal if they have the same visual and tactile stimulations. 
	 * TODO also test other stimulations.
	 */
	public boolean equals(Object o)
	{
		boolean ret = false;
		
		if (o == this)
			ret = true;
		else if (o == null)
			ret = false;
		else if (!o.getClass().equals(this.getClass()))
			ret = false;
		else
		{
			IBundle other = (IBundle)o;
			ret = other.getVisualStimulation().equals(m_visualStimulation) && 	
				  other.getTactileStimulation().equals(m_tactileStimulation);
		}
		return ret;
	}

	/**
	 * Traces the bundle 
	 */
	public void trace(ITracer tracer, String label)
	{
		Object element = tracer.addEventElement(label);
		
		// Visual stimulation
		tracer.addSubelement(element, "visual", m_visualStimulation.getHexColor());
		
		// Only trace fish gustatory stimulations.
		if (Ernest.STIMULATION_GUSTATORY_FISH.equals(m_gustatoryStimulation))
			tracer.addSubelement(element, "gustatory", m_gustatoryStimulation.getHexColor());
		else
			tracer.addSubelement(element, "gustatory", m_visualStimulation.getHexColor());
		
		// Tactile stimulation
		tracer.addSubelement(element, "tactile", m_tactileStimulation.getHexColor());

		// Only trace bump kinematic stimulation.
		if (Ernest.STIMULATION_KINEMATIC_BUMP.equals(m_kinematicStimulation))
			tracer.addSubelement(element, "kinematic", m_kinematicStimulation.getHexColor());
		else
			tracer.addSubelement(element, "kinematic", m_tactileStimulation.getHexColor());
		
		//tracer.addSubelement(element, "attractiveness", getAttractiveness(m_clock) + "");
	}

}