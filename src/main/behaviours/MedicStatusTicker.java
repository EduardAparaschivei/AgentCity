package main.behaviours;

import jade.core.behaviours.TickerBehaviour;
import main.agents.MedicAgent;

public class MedicStatusTicker extends TickerBehaviour {
    private MedicAgent medic;

    public MedicStatusTicker(MedicAgent agent, long period) {
        super(agent, period);
        this.medic = agent;
    }

    @Override
    protected void onTick() {
        medic.trimiteStatus();
    }
}