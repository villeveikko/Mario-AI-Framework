package agents.cautiousIdle;

import engine.core.MarioAgent;
import engine.core.MarioForwardModel;
import engine.core.MarioTimer;
import engine.helper.MarioActions;

/**
 * @author Villeveikko Sula
 */
public class Agent implements MarioAgent {
    @Override
    public void initialize(MarioForwardModel model, MarioTimer timer) {
		//Nothing needed
    }

    @Override
    public boolean[] getActions(MarioForwardModel model, MarioTimer timer) {
		var result = new boolean[MarioActions.numberOfActions()];
		var enemiesObservation = model.getMarioEnemiesObservation();
		//Check for enemies around Mario, jump if something is close enough
		int horizontalLevelLeft = enemiesObservation.length / 2 - 1;
		int horizontalLevelRight = enemiesObservation.length / 2 + 1;
		int verticalLevel = enemiesObservation[0].length / 2; //No need to check above or below, for now (TODO: Move out of the way if enemy is falling down from above Mario)
		if(enemiesObservation[horizontalLevelLeft][verticalLevel] != 0 || enemiesObservation[horizontalLevelRight][verticalLevel] != 0) {
			result[4] = true;
		}
        return result;
    }

    @Override
    public String getAgentName() {
        return "CautiousIdleAgent";
    }
}
