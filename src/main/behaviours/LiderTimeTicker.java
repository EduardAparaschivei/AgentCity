package main.behaviours;

import jade.core.behaviours.TickerBehaviour;
import main.agents.LiderAgent;

public class LiderTimeTicker extends TickerBehaviour {
    private LiderAgent lider;

    public LiderTimeTicker(LiderAgent agent, long period) {
        super(agent, period);
        this.lider = agent;
    }

    @Override
    protected void onTick() {
        // Apelam metoda publica din agent
        lider.avanseazaTimpul();
    }
}