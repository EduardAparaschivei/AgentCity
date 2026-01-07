package main;

import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.core.Runtime;
import jade.wrapper.AgentController;
import jade.wrapper.ContainerController;

public class Main {
    public static void main(String[] args) {
        // 1. Configuram JADE sa porneasca GUI-ul
        Profile myProfile = new ProfileImpl();
        myProfile.setParameter(Profile.GUI, "true");

        // 2. Obtinem instanta JADE Runtime
        Runtime myRuntime = Runtime.instance();

        try {
            // 3. Cream containerul principal (Main Container)
            ContainerController mainContainer = myRuntime.createMainContainer(myProfile);

            // 4. Pornim Liderul
            AgentController lider = mainContainer.createNewAgent("lider", "main.agents.LiderAgent", null);
            lider.start();

            // 5. Pornim Creierul (LLM)
            AgentController creier = mainContainer.createNewAgent("creier", "main.agents.LlmAgent", null);
            creier.start();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}