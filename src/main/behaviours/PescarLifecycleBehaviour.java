package main.behaviours;

import jade.core.behaviours.TickerBehaviour;
import main.agents.PescarAgent;

public class PescarLifecycleBehaviour extends TickerBehaviour {
    private PescarAgent pescar;

    public PescarLifecycleBehaviour(PescarAgent agent, long period) {
        super(agent, period);
        this.pescar = agent;
    }

    @Override
    protected void onTick() {
        // Daca e mort, nu face nimic
        if (pescar.isDead()) return;

        // Verificam locatia folosind getterul
        if (pescar.getLocatie().equals("Lac")) {
            pescar.pescuieste();
        } else {
            pescar.odihneste();
        }

        // Trimitem update doar daca mai traim
        if (!pescar.isDead()) {
            pescar.trimiteStatusLiderului();
        }
    }
}