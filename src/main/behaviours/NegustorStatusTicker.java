package main.behaviours;

import jade.core.behaviours.TickerBehaviour;
import main.agents.NegustorAgent;

public class NegustorStatusTicker extends TickerBehaviour {
    private NegustorAgent negustor;

    public NegustorStatusTicker(NegustorAgent agent, long period) {
        super(agent, period);
        this.negustor = agent;
    }

    @Override
    protected void onTick() {
        negustor.trimiteStatus();
    }
}