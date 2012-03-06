package spas;

import imos.IAct;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.vecmath.Vector3f;

import utils.ErnestUtils;

import ernest.Ernest;
import ernest.ITracer;

/**
 * The spatial system.
 * Maintains the local space map and the persistence memory.
 * @author Olivier
 */
public class Spas implements ISpas 
{
	
	/** The Tracer. */
	private ITracer m_tracer = null; 
	
	public static int PLACE_BACKGROUND = -1;
	public static int PLACE_SEE = 0;
	public static int PLACE_TOUCH = 1;
	public static int PLACE_FOCUS = 10;
	public static int PLACE_BUMP = 11;
	public static int PLACE_EAT  = 12;
	public static int PLACE_CUDDLE = 13;
	public static int PLACE_PRIMITIVE = 14;
	public static int PLACE_COMPOSITE = 15;
	public static int PLACE_INTERMEDIARY = 16;
	
	public static int SHAPE_CIRCLE = 0;
	public static int SHAPE_TRIANGLE = 1;
	public static int SHAPE_PIE = 2;

	/** Ernest's persistence momory  */
	private PersistenceMemory m_persistenceMemory = new PersistenceMemory();
	
	/** Ernest's local space memory  */
	private LocalSpaceMemory m_localSpaceMemory;
	
	/** The list of saliences generated by Ernest's sensory system  */
	List<IPlace> m_placeList = new ArrayList<IPlace>();
	
	ArrayList<ISegment> m_segmentList = new ArrayList<ISegment>();

	IObservation m_observation;
	
	/** The color of attention for display in the environment.  */
	private int mAttention = Ernest.UNANIMATED_COLOR;
	
	/** Temporary places.  */
	ArrayList<IPlace> m_places = new ArrayList<IPlace>();

	public void setTracer(ITracer tracer) 
	{
		m_tracer = tracer;
		m_persistenceMemory.setTracer(tracer);
		m_localSpaceMemory = new LocalSpaceMemory(m_persistenceMemory, m_tracer);
	}

	public void step(IPlace interactionPlace, IObservation observation) 
	{		
		m_persistenceMemory.updateCount();
		m_observation = observation;
		
		// update the local space memory;
		
		Vector3f memoryTranslation = new Vector3f(observation.getTranslation());
		memoryTranslation.scale(-1);
		m_localSpaceMemory.update(memoryTranslation, - observation.getRotation());

		// Construct the list of primitive bundles and places. 
		
		clear();
		m_localSpaceMemory.clear();
		
		addSegmentPlaces(m_segmentList);
		addTactilePlaces(observation.getTactileStimuli());
		
		// Create new bundles and place them in the local space memory.
		
		//addKinematicPlace(observation.getKinematicValue());
		//addGustatoryPlace(observation);
		
		// Clean up the local space memory according to the tactile simulations.
		
		//adjustLocalSpaceMemory(tactileStimulations);
		
		// Confirm or create persistent places in local space memory 
		
		for (IPlace place : m_places)
		{
			if (place.attractFocus(m_persistenceMemory.getUpdateCount()))
			{
				boolean newPlace = true;
				// Look for a corresponding persistent place in local space memory.
				for (IPlace p : m_localSpaceMemory.getPlaceList())
				{
//					if (p.attractFocus(m_persistenceMemory.getUpdateCount()-1) 
//							&& p.getBundle().equals(place.getBundle())
//							&& place.getType() == p.getType() && place.from(p.getPosition()))
					if (p.attractFocus(m_persistenceMemory.getUpdateCount()-1) 
							&& p.getBundle().equals(place.getBundle())
							&& place.from(p))
					{
						p.setPosition(place.getPosition());
						p.setFirstPosition(place.getFirstPosition());
						p.setSecondPosition(place.getSecondPosition());
						p.setSpeed(place.getSpeed());
						p.setSpan(place.getSpan());
						p.setOrientation(place.getOrientation());
						p.setUpdateCount(m_persistenceMemory.getUpdateCount());
						newPlace = false;
					}
				}
				if (newPlace)
				{
					// Add a new persistent place
					IPlace k = m_localSpaceMemory.addPlace(place.getBundle(),place.getPosition()); 
					k.setSpeed(place.getSpeed());
					k.setSpan(place.getSpan());
					k.setFirstPosition(place.getFirstPosition()); // somehow inverted
					k.setSecondPosition(place.getSecondPosition());
					k.setOrientation(place.getOrientation());
					k.setUpdateCount(m_persistenceMemory.getUpdateCount());
					k.setType(place.getType());
				}
			}
		}
		
		// The most attractive place in local space memory gets the focus (abs value) 
		
		int maxAttractiveness = 0;
		IPlace focusPlace = null;
		boolean newFocus = false;
		for (IPlace place : m_localSpaceMemory.getPlaceList())
		{
			if (place.attractFocus(m_persistenceMemory.getUpdateCount()) && place.getType() != Spas.PLACE_BACKGROUND)
			{
				int attractiveness =  place.getAttractiveness(m_persistenceMemory.getClock());
				if (Math.abs(attractiveness) >= Math.abs(maxAttractiveness))
				{
					maxAttractiveness = attractiveness;
					focusPlace = place;
				}				
			}
		}
		
		// Test if the focus has changed
		
		if (focusPlace != null && focusPlace != m_localSpaceMemory.getFocusPlace())
		{
			// Reset the previous stick
			if (m_localSpaceMemory.getFocusPlace() != null) m_localSpaceMemory.getFocusPlace().setStick(0);
			// Set the new stick
			focusPlace.setStick(20);
			m_localSpaceMemory.setFocusPlace(focusPlace);
			newFocus = true;
			
			//try { Thread.sleep(500);
			//} catch (InterruptedException e) {e.printStackTrace();}
		}
		
		// The new observation.
		
		observation.setFocusPlace(m_localSpaceMemory.getFocusPlace());
		observation.setAttractiveness(maxAttractiveness);
		observation.setNewFocus(newFocus);
		
		if (focusPlace == null)
		{
			mAttention = Ernest.UNANIMATED_COLOR;
			//observation.setBundle(m_persistenceMemory.addBundle(Ernest.STIMULATION_VISUAL_UNSEEN, Ernest.STIMULATION_TOUCH_EMPTY, Ernest.STIMULATION_KINEMATIC_FORWARD, Ernest.STIMULATION_GUSTATORY_NOTHING));
			observation.setBundle(m_persistenceMemory.addBundle(Ernest.STIMULATION_VISUAL_UNSEEN, Ernest.STIMULATION_TOUCH_EMPTY));
			observation.setPosition(new Vector3f(1,0,0));
			observation.setSpan(0);
			observation.setSpeed(new Vector3f());
			observation.setUpdateCount(-1);
		}
		else
		{
			mAttention = focusPlace.getBundle().getValue();
			observation.setBundle(focusPlace.getBundle());
			observation.setPosition(focusPlace.getPosition());
			observation.setSpan(focusPlace.getSpan());
			observation.setSpeed(focusPlace.getSpeed());
			observation.setType(focusPlace.getType());
			observation.setUpdateCount(focusPlace.getUpdateCount());
		}		
		
		addKinematicPlace(interactionPlace, observation);
		addGustatoryPlace(interactionPlace, observation);

	}
	
//	public IStimulation addStimulation(int type, int value) 
//	{
//		return m_persistenceMemory.addStimulation(type, value);
//	}

	public int getValue(int i, int j)
	{
//		if (i == 1 && j == 0 && m_kinematicStimulation == Ernest.STIMULATION_KINEMATIC_BUMP)
//			return Ernest.STIMULATION_KINEMATIC_BUMP;
//		else if (i == 1 && j == 1 && m_gustatoryStimulation == Ernest.STIMULATION_GUSTATORY_FISH)
//			return Ernest.STIMULATION_GUSTATORY_FISH;
//		else
//		{
			Vector3f position = new Vector3f(1 - j, 1 - i, 0);
			if (m_localSpaceMemory != null)
				return m_localSpaceMemory.getValue(position);
			else
				return 0xFFFFFF;
//		}
	}

	public int getAttention()
	{
		return mAttention;
	}
	
	/**
	 * Set the list of saliences from the list provided by VacuumSG.
	 * @param salienceList The list of saliences provided by VacuumSG.
	 */
	public void setPlaceList(List<IPlace> placeList)
	{
		m_placeList = placeList;
	}
		
	public ArrayList<IPlace> getPlaceList()
	{
		//return m_places;
		return m_localSpaceMemory.getPlaceList();
	}

	public void count() 
	{
		// Update the decay counter in persistence memory
		m_persistenceMemory.count();
	}

	public void traceLocalSpace() 
	{
		m_localSpaceMemory.Trace();
	}

	public void setSegmentList(ArrayList<ISegment> segmentList) 
	{
		m_segmentList = segmentList;
	}
	
	/**
	 * Add places from segments provided by Vacuum_SG.
	 * Create or recognize the associated bundle.
	 * @param segmentList The list of segments.
	 */
	public void addSegmentPlaces(ArrayList<ISegment> segmentList)
	{
		for (ISegment segment : segmentList)
		{
			IBundle b = m_persistenceMemory.seeBundle(segment.getValue());
			if (b == null)
				//b = m_persistenceMemory.addBundle(segment.getValue(), Ernest.STIMULATION_TOUCH_EMPTY, Ernest.STIMULATION_KINEMATIC_FORWARD, Ernest.STIMULATION_GUSTATORY_NOTHING);
				b = m_persistenceMemory.addBundle(segment.getValue(), Ernest.STIMULATION_TOUCH_EMPTY);
			IPlace place = new Place(b,segment.getPosition());
			place.setSpeed(segment.getSpeed());
			place.setSpan(segment.getSpan());
			//place.setFirstPosition(segment.getFirstPosition()); // First and second are in the trigonometric direction (counterclockwise). 
			//place.setSecondPosition(segment.getSecondPosition());
			place.setFirstPosition(segment.getSecondPosition()); 
			place.setSecondPosition(segment.getFirstPosition());
			if (segment.getRelativeOrientation() == Ernest.INFINITE)
			{
				Vector3f relativeOrientation = new Vector3f(segment.getFirstPosition());
				relativeOrientation.sub(segment.getSecondPosition());
				place.setOrientation(ErnestUtils.polarAngle(relativeOrientation));
			}
			else				
				place.setOrientation(segment.getRelativeOrientation());
			place.setUpdateCount(m_persistenceMemory.getUpdateCount());
			// Long segments are processed only for display (background).
			if (segment.getWidth() < 1.1f)
				place.setType(Spas.PLACE_SEE);
			else 
				place.setType(Spas.PLACE_BACKGROUND);
			m_places.add(place);			
		}
	}

	/**
	 * Add places in the peripersonal space associated with tactile bundles.
	 * TODO Handle a tactile place behind the agent (last place connected to first place).
	 * @param tactileStimulations The list of visual stimulations.
	 */
	public void addTactilePlaces(int[] tactileStimulations)
	{

		int tactileStimulation = tactileStimulations[0];
		int span = 1;
		float theta = - 3 * (float)Math.PI / 4; 
		float sumDirection = theta;
		float spanf = (float)Math.PI / 4;
		
		for (int i = 1 ; i <= 7; i++)
		{
			theta += (float)Math.PI / 4;
			if ((i < 7) && tactileStimulations[i] == tactileStimulation)
			{
				// measure the salience span and average direction
				span++;
                sumDirection += theta;
                spanf += (float)Math.PI / 4;
			}
			else 
			{	
				if (tactileStimulation != Ernest.STIMULATION_TOUCH_EMPTY)
				{
					// Create a tactile bundle.
					float direction = sumDirection / span;
					Vector3f position = new Vector3f((float)(Ernest.TACTILE_RADIUS * Math.cos((double)direction)), (float)(Ernest.TACTILE_RADIUS * Math.sin((double)direction)), 0f);
					float firstDirection = direction - spanf/ 2;
					Vector3f firstPosition = new Vector3f((float)(Ernest.TACTILE_RADIUS * Math.cos((double)firstDirection)), (float)(Ernest.TACTILE_RADIUS * Math.sin((double)firstDirection)), 0f);
					float secondDirection = direction + spanf/ 2;
					Vector3f secondPosition = new Vector3f((float)(Ernest.TACTILE_RADIUS * Math.cos((double)secondDirection)), (float)(Ernest.TACTILE_RADIUS * Math.sin((double)secondDirection)), 0f);
					
					// See in that direction ====
					IPlace place = seePlace(direction);
					
					if (place == null)
					{
						// Nothing seen: create a tactile bundle and place it here.
						//IBundle b = m_persistenceMemory.addBundle(Ernest.STIMULATION_VISUAL_UNSEEN, tactileStimulation, Ernest.STIMULATION_KINEMATIC_FORWARD, Ernest.STIMULATION_GUSTATORY_NOTHING);
						IBundle b = m_persistenceMemory.addBundle(Ernest.STIMULATION_VISUAL_UNSEEN, tactileStimulation);
						place = addOrReplacePlace(b, position);
						place.setFirstPosition(firstPosition);
						place.setSecondPosition(secondPosition);
						place.setSpan(spanf);
						place.setSpeed(new Vector3f(0,0,.01f)); // (Keeping the speed "null" generates errors in the Local Space Memory display).
						place.setUpdateCount(m_persistenceMemory.getUpdateCount());
						place.setType(Spas.PLACE_TOUCH);
					}
					else
					{
						if (place.getBundle().getTactileValue() == tactileStimulation )//&&
							//place.getFrontDistance() < Ernest.TACTILE_RADIUS + .1f) // vision now provides distance
						{
							// A bundle is seen with the same tactile value: This is it!
							place.getBundle().setLastTimeBundled(m_persistenceMemory.getClock());
							// move the visual place to the tactile radius.
							place.setPosition(position); // Position is more precise with tactile perception, especially for long walls.
							place.setFirstPosition(firstPosition);
							place.setSecondPosition(secondPosition);
							place.setSpan(spanf);
							place.setType(Spas.PLACE_TOUCH);
							//place.setUpdateCount(m_persistenceMemory.getUpdateCount());
						}
						else if (place.getBundle().getTactileValue() == Ernest.STIMULATION_TOUCH_EMPTY )//&& 
								//place.getFrontDistance() < Ernest.TACTILE_RADIUS + .1f)
						{
							// A bundle is seen in the same position with no tactile value.
							
							// Update the place and the bundle
							//IBundle b = m_persistenceMemory.addBundle(place.getBundle().getVisualValue(), tactileStimulation, Ernest.STIMULATION_KINEMATIC_FORWARD, Ernest.STIMULATION_GUSTATORY_NOTHING);
							IBundle b = m_persistenceMemory.addBundle(place.getBundle().getVisualValue(), tactileStimulation);
							place.setBundle(b);
							
							//place.getBundle().setTactileValue(tactileStimulation.getValue());
							//place.getBundle().setLastTimeBundled(m_persistenceMemory.getClock());
							place.setPosition(position);							
							place.setFirstPosition(firstPosition);
							place.setSecondPosition(secondPosition);
							place.setSpan(spanf);
							place.setType(Spas.PLACE_TOUCH);
							//place.setUpdateCount(m_persistenceMemory.getUpdateCount());
						}
					}
				}
				// look for the next bundle
				if (i < 7)
				{
					tactileStimulation = tactileStimulations[i];
					span = 1;
					spanf = (float)Math.PI / 4;
					sumDirection = theta;
				}
			}
		}
	}
	/**
	 * Find the closest place whose span overlaps this direction.
	 * @param direction The direction in which to look at.
	 * @return The place.
	 */
	private IPlace seePlace(float direction)
	{
		IPlace place = null;

		for (IPlace p : m_places)
		{
//			if (p.getDirection() - p.getSpan() / 2 < direction - Math.PI/12 + 0.1 && 
//				p.getDirection() + p.getSpan() / 2 > direction + Math.PI/12 - 0.1 &&
//				p.getBundle().getVisualValue() != Ernest.STIMULATION_VISUAL_UNSEEN &&
//				p.attractFocus(m_persistenceMemory.getUpdateCount()))
//				if (place == null || p.getDistance() < place.getDistance())
//					place = p;

			float firstAngle = ErnestUtils.polarAngle(p.getFirstPosition());
			float secondAngle = ErnestUtils.polarAngle(p.getSecondPosition());
			if (firstAngle < secondAngle)
			{
				// Does not overlap direction -PI
				if (direction > firstAngle + 0.1f && direction < secondAngle - .05f && 
					p.getBundle().getVisualValue() != Ernest.STIMULATION_VISUAL_UNSEEN &&
					p.attractFocus(m_persistenceMemory.getUpdateCount()))
						if (place == null || p.getDistance() < place.getDistance())
							place = p;
			}
			else
			{
				// Overlaps direction -PI
				if (direction > firstAngle + .1f || direction < secondAngle - .1f &&
					p.getBundle().getVisualValue() != Ernest.STIMULATION_VISUAL_UNSEEN &&
					p.attractFocus(m_persistenceMemory.getUpdateCount()))
						if (place == null || p.getDistance() < place.getDistance())
							place = p;				
			}
		}
		return place;
	}
	
	public IPlace addOrReplacePlace(IBundle bundle, Vector3f position)
	{
		// The initial position must be cloned so that 
		// the position can be moved without changing the position used for intialization.
		Vector3f pos = new Vector3f(position);
		
		IPlace p = new Place(bundle, pos);
		p.setUpdateCount(m_persistenceMemory.getUpdateCount());
		
		int i = m_places.indexOf(p);
		if (i == -1)
			// The place does not exist
			m_places.add(p);
		else 
		{
			// The place already exists: return a pointer to it.
			p =  m_places.get(i);
			p.setBundle(bundle);
		}
		return p;
	}
	
	private void addKinematicPlace(IPlace interactionPlace, IObservation observation)
	{
		int kinematicValue= observation.getKinematicValue();

		// Find the place in front of Ernest.
		IPlace focusPlace = null;
		for (IPlace place : m_places)
			if (place.isFrontal() && 
				place.getBundle().getVisualValue() != Ernest.STIMULATION_VISUAL_UNSEEN && 
				place.attractFocus(m_persistenceMemory.getUpdateCount()) && 
				place.getType() != Spas.PLACE_BACKGROUND && 
				place.getFrontDistance() < 1)
				
				focusPlace = place;
		
		// Associate kinematic stimulation to the front bundle.

		if (kinematicValue == Ernest.STIMULATION_KINEMATIC_BUMP)
		{
			if (focusPlace != null)
			{
				// Add bump interaction to the bundle at this place.
				//m_persistenceMemory.addKinematicValue(frontPlace.getBundle(), kinematicValue);
				IBundle focusBunble = focusPlace.getBundle();
				//focusBunble.setKinematicValue(kinematicValue);
				
				// Add the affordance to the bundle
				Vector3f relativePosition = new Vector3f(interactionPlace.getPosition());
				//relativePosition.sub(focusPlace.getPosition());
				relativePosition.sub(new Vector3f(.4f, 0,0));
				ErnestUtils.rotate(relativePosition, - focusPlace.getOrientation());
				focusBunble.addAffordance(observation.getPrimitiveAct(), interactionPlace, relativePosition, focusPlace.getOrientation(), Ernest.ATTRACTIVENESS_OF_BUMP, kinematicValue);
				
				// Add a Bump place.
				Vector3f pos = new Vector3f(LocalSpaceMemory.DIRECTION_AHEAD);
				pos.scale(Ernest.BOUNDING_RADIUS);
				//IPlace k = new Place(frontPlace.getBundle(), pos);
				IPlace k = m_localSpaceMemory.addPlace(focusPlace.getBundle(), pos);
				k.setType(Spas.PLACE_BUMP);
				k.setFirstPosition(pos);
				k.setSecondPosition(pos);
				k.setUpdateCount(m_persistenceMemory.getUpdateCount());
				//m_places.add(k);
			}
		}
	}

	private void addGustatoryPlace(IPlace interactionPlace, IObservation observation)
	{
		int gustatoryValue= observation.getGustatoryValue();
		//IPlace focusPlace = observation.getFocusPlace();
		
		if (gustatoryValue != Ernest.STIMULATION_GUSTATORY_NOTHING)
		{
			IPlace focusPlace = getPlace(LocalSpaceMemory.DIRECTION_AHEAD);
			IBundle focusBundle = null;
			if (focusPlace != null) focusBundle = focusPlace.getBundle();
	
			if (focusBundle == null)// || focusBundle.getGustatoryValue() == Ernest.STIMULATION_GUSTATORY_NOTHING) //&& //observation.getPosition().length() <= 1 &&
			{
				// Create a gustatory bundle
				//focusBundle = m_persistenceMemory.addBundle(Ernest.STIMULATION_VISUAL_UNSEEN, Ernest.STIMULATION_TOUCH_EMPTY, Ernest.STIMULATION_KINEMATIC_FORWARD, gustatoryValue);				
				focusBundle = m_persistenceMemory.addBundle(Ernest.STIMULATION_VISUAL_UNSEEN, Ernest.STIMULATION_TOUCH_EMPTY);				
				focusPlace = addOrReplacePlace(focusBundle, new Vector3f(.4f, 0,0));
				focusPlace.setFirstPosition(new Vector3f(.4f, 0,0));
				focusPlace.setSecondPosition(new Vector3f(.4f, 0,0));
				focusPlace.setSpan(0);
				focusPlace.setSpeed(new Vector3f(0,0,.01f)); // (Keeping the speed "null" generates errors in the Local Space Memory display).
				focusPlace.setUpdateCount(m_persistenceMemory.getUpdateCount());
				focusPlace.setType(Spas.PLACE_TOUCH);
			}
			
			else
			{
				// Append the gustatory stimulation
//				if	((gustatoryValue == Ernest.STIMULATION_GUSTATORY_FISH) && (focusBundle.getTactileValue() == Ernest.STIMULATION_TOUCH_FISH) ||
//					(gustatoryValue == Ernest.STIMULATION_GUSTATORY_CUDDLE) && (focusBundle.getTactileValue() == Ernest.STIMULATION_TOUCH_AGENT))
				{
					// Add the gustatory value to the bundle
//					focusBundle.setGustatoryValue(gustatoryValue);
				}
			}
			// Add the affordance to the bundle
			Vector3f relativePosition = new Vector3f(interactionPlace.getPosition());
			relativePosition.sub(new Vector3f(.4f, 0,0));
			ErnestUtils.rotate(relativePosition, - focusPlace.getOrientation());
			int attractiveness = Ernest.ATTRACTIVENESS_OF_FISH;
			if (gustatoryValue == Ernest.STIMULATION_GUSTATORY_CUDDLE) attractiveness = Ernest.ATTRACTIVENESS_OF_CUDDLE;
			focusBundle.addAffordance(observation.getPrimitiveAct(), interactionPlace, relativePosition, focusPlace.getOrientation(), attractiveness, gustatoryValue);

			// Add an interaction place.
			Vector3f pos = new Vector3f(LocalSpaceMemory.DIRECTION_AHEAD);
			pos.scale(Ernest.BOUNDING_RADIUS);
			IPlace k = m_localSpaceMemory.addPlace(focusBundle, pos);
			k.setFirstPosition(pos);
			k.setSecondPosition(pos);
			k.setType(Spas.PLACE_EAT);
			k.setUpdateCount(m_persistenceMemory.getUpdateCount());
		}
	}
		
	/**
	 * Get the first place found at a given position.
	 * @param position The position of the location.
	 * @return The place.
	 */
	public IPlace getPlace(Vector3f position)
	{
		IPlace place = null;
		for (IPlace p : m_places)
		{
			if (p.attractFocus(m_persistenceMemory.getUpdateCount()))
			{
				//if (p.isInCell(position) && p.attractFocus(m_persistenceMemory.getUpdateCount()))
				Vector3f compare = new Vector3f(p.getPosition());
				compare.sub(position);
				if (compare.length() < 1f)
					place = p;
			}
		}
		return place;
	}

	/**
	 * Clear a location in the local space memory.
	 * @param position The position to clear.
	 */
	public void clearPlace(Vector3f position)
	{
		for (Iterator it = m_places.iterator(); it.hasNext();)
		{
			IPlace l = (IPlace)it.next();
			if (l.isInCell(position))
				it.remove();
		}		
	}
		
	public void clear()
	{
//		for (Iterator it = m_places.iterator(); it.hasNext();)
//		{
//			IPlace p = (IPlace)it.next();
//			if (p.getType() == Spas.PLACE_FOCUS) p.setType(Spas.PLACE_SEE);
//			if (p.getUpdateCount() < m_persistenceMemory.getUpdateCount() - 10)
//				it.remove();
//		}

		m_places.clear();
	}

	public IPlace getFocusPlace() 
	{
		return m_localSpaceMemory.getFocusPlace();
	}

	public IPlace addPlace(Vector3f position, int type, int shape) 
	{
		IPlace place = m_localSpaceMemory.addPlace(null, position);
		place.setFirstPosition(position);
		place.setSecondPosition(position);
		place.setType(type);
		place.setShape(shape);
		place.setUpdateCount(m_persistenceMemory.getUpdateCount());
		
		return place;
	}	
}
