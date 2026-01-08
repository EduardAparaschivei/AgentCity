package main.agents;

import jade.core.AID;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.WakerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.proto.SubscriptionInitiator;
import jade.wrapper.AgentController;
import jade.wrapper.ContainerController;
import main.behaviours.LiderTaxListener;
import main.behaviours.LiderTimeTicker;
import main.behaviours.LiderUpdateListener;
import main.gui.LiderGui;

import javax.swing.*;
import java.io.*;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Properties;
import java.util.Set;

public class LiderAgent extends BaseAgent {
    private LiderGui myGui;

    // --- STATE VARIABLES ---
    private int buget = 0;
    private int contorPescari = 1;

    // Configurare Timp
    private boolean regimMuncaDublu = false;
    private int fazaZilei = 0;
    private int valoareTaxa = 10;

    // --- CACHE PENTRU AGENTI ---
    // Set pentru a evita duplicatele. Aici tinem minte pescarii (food-producer)
    private Set<AID> agentiProductori = new HashSet<>();
    // Aici tinem minte colectorul (pentru a nu-l cauta mereu) si agentul LLM
    private AID agentColector = null;
    private AID agentLLM = null;
    private AID agentMedic = null;
    private AID agentNegustor = null;

    @Override
    protected void setup() {
        // 1. Meniul de Start
        int choice = meniuStartJoc();

        // 2. CREARE AGENTI AUXILIARI (Medic, Negustor, Colector)
        // Liderul îi pornește programatic acum!
        creeazaAgentiAuxiliari();

        populeazaCacheInitial();

        pornesteMonitorizareAgenti();

        // 3. LOGICA JOC NOU vs CONTINUARE
        if (choice == 1) {
            incarcaSalvare();
            restaureazaPescarii();
        } else {
            buget = 100;
            contorPescari = 1;
            creeazaAgent("pescar1", "main.agents.PescarAgent");
            System.out.println("--- JOC NOU INITIAT ---");
        }

        // 4. GUI INIT
        myGui = new LiderGui(this);
        myGui.updateLiderStats(buget);

        // 5. ATAȘARE COMPORTAMENTE EXTERNE (Decuplare)
        addBehaviour(new LiderTimeTicker(this, 2500));
        addBehaviour(new LiderUpdateListener(this));
        addBehaviour(new LiderTaxListener(this));
    }

    // --- LOGICA DE CREARE AGENTI (Factory) ---

    private void populeazaCacheInitial() {
        System.out.println("Lider: Scanez reteaua pentru agenti existenti...");
        DFAgentDescription template = new DFAgentDescription();

        try {
            DFAgentDescription[] result = DFService.search(this, template);
            if (result.length > 0) {
                for (DFAgentDescription dfd : result) {
                    processAgentDiscovery(dfd.getName(), dfd.getAllServices());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Aceasta metoda este apelata SI din Search initial, SI din Subscribe
    private void processAgentDiscovery(AID agentAID, Iterator services) {
        while (services.hasNext()) {
            ServiceDescription sd = (ServiceDescription) services.next();
            String tip = sd.getType();

            if (tip.equalsIgnoreCase("food-producer")) {
                agentiProductori.add(agentAID);
                // System.out.println("-> Cache: Pescar gasit: " + agentAID.getLocalName());
            }
            else if (tip.equalsIgnoreCase("tax-collector")) {
                agentColector = agentAID;
            }
            else if (tip.equalsIgnoreCase("medic") || tip.equalsIgnoreCase("doctor")) {
                agentMedic = agentAID;
            }
            else if (tip.equalsIgnoreCase("negustor")) {
                agentNegustor = agentAID;
            }
            // ---> AICI IL PRINDE PE CREIER <---
            else if (tip.equalsIgnoreCase("llm-service")) {
                agentLLM = agentAID;
                System.out.println("✅ Lider: Am conectat serviciul AI: " + agentAID.getLocalName());
            }
        }
    }

    private void creeazaAgentiAuxiliari() {
        System.out.println("Lider: Pornesc serviciile publice...");
        creeazaAgent("medic", "main.agents.MedicAgent");
        creeazaAgent("negustor", "main.agents.NegustorAgent");
        creeazaAgent("colector", "main.agents.ColectorAgent");
    }

    private void restaureazaPescarii() {
        System.out.println("Restaurare sesiune: Aveam " + contorPescari + " pescari.");
        for (int i = 1; i <= contorPescari; i++) {
            creeazaAgent("pescar" + i, "main.agents.PescarAgent");
        }
    }

    // Metoda generica pentru a porni orice agent
    private void creeazaAgent(String nickname, String className) {
        try {
            ContainerController container = getContainerController();
            AgentController ac = container.createNewAgent(nickname, className, null);
            ac.start();
            System.out.println(" -> Creat agent: " + nickname);
        } catch (Exception e) {
            System.err.println("Eroare creare " + nickname + ": " + e.getMessage());
        }
    }

    public void angajeazaPescar() {
        // --- 1. LIMITA MAXIMA (Cerinta ta: Max 4) ---
        if (contorPescari >= 4) {
            System.out.println("Lider: Nu mai fac angajari. Maxim 4 pescari atins.");
            // Optional: Afisam si in GUI un mesaj
            if (myGui != null) {
                javax.swing.JOptionPane.showMessageDialog(myGui, "Capacitate maximă atinsă (4 Pescari)!");
            }
            return; // Iesim din functie, nu mai cream nimic
        }

        // --- 2. LOGICA DE ANGAJARE ---
        if (buget >= 100) {
            buget -= 100;
            if(myGui != null) myGui.updateLiderStats(buget);

            contorPescari++;
            String numeNou = "pescar" + contorPescari;

            // Il cream fizic in container
            creeazaAgent(numeNou, "main.agents.PescarAgent");

            // --- 3. [FIX CRITIC] ADAUGARE INSTANTANEE IN CACHE ---
            // Nu asteptam notificarea DF. Stim ca l-am creat, il bagam in lista direct.
            // Fiind un Set, daca vine si notificarea de la DF mai tarziu, nu se intampla nimic rau (nu se dubleaza).
            AID agentNouAID = new AID(numeNou, AID.ISLOCALNAME);
            agentiProductori.add(agentNouAID);
            System.out.println("Lider: L-am adaugat manual in lista pe " + numeNou + " (nu astept DF).");
            // ----------------------------------------------------

            // 4. Sincronizare imediata (Welcome Packet)
            addBehaviour(new OneShotBehaviour() {
                @Override
                public void action() {
                    try { Thread.sleep(500); } catch (Exception e) {}

                    // Ii spunem imediat ce sa faca (daca e zi, la munca!)
                    String comanda = (fazaZilei == 0 || (fazaZilei == 1 && regimMuncaDublu)) ? "START_WORK" : "END_WORK";
                    ACLMessage msg = new ACLMessage(ACLMessage.PROPAGATE);
                    msg.addReceiver(agentNouAID);
                    msg.setContent(comanda);
                    send(msg);
                }
            });
        } else {
            System.out.println("Lider: Nu am buget pentru angajari!");
            if (myGui != null) {
                javax.swing.JOptionPane.showMessageDialog(myGui, "Nu ai destui bani (100g)!");
            }
        }
    }

    // --- METODE PUBLICE (Apelate din Behaviours) ---

    public void avanseazaTimpul() {
        fazaZilei++;
        if (fazaZilei > 3) fazaZilei = 0;

        String comanda = "";
        String textTimp = "";
        boolean isDay = false;

        switch (fazaZilei) {
            case 0:
                comanda = "START_WORK"; textTimp = "DIMINEAȚA (Muncă)"; isDay = true; break;
            case 1:
                if (regimMuncaDublu) { comanda = "CONTINUE_WORK"; textTimp = "DUPĂ-AMIAZĂ (Muncă Extra!)"; }
                else { comanda = "END_WORK"; textTimp = "DUPĂ-AMIAZĂ (Liber)"; }
                isDay = true; break;
            case 2:
                comanda = "END_WORK"; textTimp = "SEARA (Colectare Taxe)"; isDay = false;
                addBehaviour(new OneShotBehaviour() { @Override public void action() { startColectareAutomata(); }});
                break;
            case 3:
                comanda = "SLEEP"; textTimp = "NOAPTEA (Somn)"; isDay = false; break;
        }

        if (myGui != null) myGui.setTimeText(textTimp, isDay);
        if (!comanda.equals("SLEEP") && !comanda.equals("CONTINUE_WORK")) {
            trimiteSemnalTuturor(comanda);
        }
    }

    public void gestioneazaDeces(String numeMort) {
        if (myGui != null) {
            myGui.removeAgent(numeMort);
            System.out.println("✝️ " + numeMort + " a decedat.");
        }
    }

    public void gestioneazaUpdate(String jsonContent) {
        if (myGui != null) myGui.updateFishermanData(jsonContent);
    }

    public void adaugaLaBuget(int suma) {
        this.buget += suma;
        if (myGui != null) {
            myGui.updateLiderStats(buget);
            System.out.println("Am primit " + suma + " galbeni taxe.");
        }
    }

    public LiderGui getGui() { return myGui; }

    // --- METODE PENTRU LLM / UI ---

    public void cereAnalizaAI() {
        if (myGui != null) myGui.setAnalizaButtonEnabled(false);

        addBehaviour(new OneShotBehaviour() {
            @Override
            public void action() {
                String dateAgenti = (myGui != null) ? myGui.getRaportDate() : "Fara date.";
                String contextGlobal = "Buget: " + buget + ". Taxa: " + valoareTaxa + ". Date: " + dateAgenti;
                String instructiune = "Analizeaza situatia orasului scurt si amuzant.";

                String raspuns = cereRaspunsDirect(contextGlobal, instructiune);
                if (myGui != null) myGui.afiseazaRaportAI(raspuns);
            }
        });

        addBehaviour(new WakerBehaviour(this, 10000) {
            @Override
            protected void onWake() { if (myGui != null) myGui.setAnalizaButtonEnabled(true); }
        });
    }

    // --- HELPERE PRIVATE ---

    private int meniuStartJoc() {
        String[] options = {"Joc Nou (Reset)", "Continuă Salvarea"};
        return JOptionPane.showOptionDialog(null, "Bine ai venit în CitySim!", "Startup Menu",
                JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
    }

    private void startColectareAutomata() {
        // Verificam si fortam actualizarea daca e null
        agentColector = getAgentFunctional("tax-collector", agentColector);

        if (agentColector != null) {
            ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
            msg.addReceiver(agentColector);
            msg.setContent("COLECTEAZA_AUTOMAT:" + valoareTaxa);
            send(msg);
        } else {
            System.out.println("Lider: Nu stiu cine e colectorul (nici dupa cautare manuala)!");
        }
    }

    private void trimiteSemnalTuturor(String comanda) {
        // FALLBACK: Daca lista din Cache e goala, facem un Search rapid
        if (agentiProductori.isEmpty()) {
            System.out.println("⚠️ Lider: Lista cache goală. Caut pescarii manual...");
            try {
                DFAgentDescription template = new DFAgentDescription();
                ServiceDescription sd = new ServiceDescription();
                sd.setType("food-producer"); // <--- Verifica sa fie acelasi tip ca in Pescar
                template.addServices(sd);

                DFAgentDescription[] result = DFService.search(this, template);
                for (DFAgentDescription dfd : result) {
                    agentiProductori.add(dfd.getName());
                }
            } catch (Exception e) { e.printStackTrace(); }
        }

        // Daca tot goala e, nu avem cui trimite
        if (agentiProductori.isEmpty()) {
            System.out.println("❌ Lider: Nu am cui sa trimit comanda " + comanda + " (0 pescari gasiti).");
            return;
        }

        ACLMessage msg = new ACLMessage(ACLMessage.PROPAGATE);
        msg.setContent(comanda);

        for (AID agent : agentiProductori) {
            msg.addReceiver(agent);
        }
        send(msg);
        System.out.println("📢 Lider: Am trimis " + comanda + " catre " + agentiProductori.size() + " pescari.");
    }

    private void trimiteOrdin(String serviceType, String content, int performative) {
        AID destinatar = null;

        switch (serviceType) {
            case "tax-collector":
                // Folosim metoda noua cu Fallback
                agentColector = getAgentFunctional("tax-collector", agentColector);
                destinatar = agentColector;
                break;
            case "medic":
                agentMedic = getAgentFunctional("medic", agentMedic);
                destinatar = agentMedic;
                break;
            case "negustor":
                agentNegustor = getAgentFunctional("negustor", agentNegustor);
                destinatar = agentNegustor;
                break;
            case "food-producer":
                if (!agentiProductori.isEmpty()) {
                    destinatar = agentiProductori.iterator().next();
                }
                break;
        }

        if (destinatar != null) {
            ACLMessage msg = new ACLMessage(performative);
            msg.addReceiver(destinatar);
            msg.setContent(content);
            send(msg);
        } else {
            System.out.println("❌ Eroare CRITICA: Agentul [" + serviceType + "] nu poate fi gasit nici in Cache, nici in DF.");
        }
    }

    private String cereRaspunsDirect(String context, String instructiune) {
        // Fallback: Daca e null, cauta-l acum pe loc
        if (agentLLM == null) {
            System.out.println("⚠️ Lider: Agentul LLM nu e in cache. Incerc cautare de urgenta...");
            try {
                DFAgentDescription template = new DFAgentDescription();
                ServiceDescription sd = new ServiceDescription();
                sd.setType("llm-service");
                template.addServices(sd);
                DFAgentDescription[] res = DFService.search(this, template);
                if (res.length > 0) agentLLM = res[0].getName();
            } catch (Exception e) {}
        }

        if (agentLLM == null) return "Eroare: Serviciul AI nu este online.";

        // ... trimiterea mesajului (codul existent) ...
        try {
            ACLMessage req = new ACLMessage(ACLMessage.REQUEST);
            req.addReceiver(agentLLM);
            req.setContent("Analist|||" + context + "|||" + instructiune);
            req.setConversationId(String.valueOf(System.currentTimeMillis()));
            send(req);
            ACLMessage reply = blockingReceive(MessageTemplate.MatchSender(agentLLM), 8000);
            return (reply != null) ? reply.getContent() : "Timeout LLM";
        } catch (Exception e) { return "Eroare comunicare."; }
    }

    // --- SAVE / LOAD / UI CONTROLS ---

    private void incarcaSalvare() {
        Properties p = new Properties();
        try (InputStream input = new FileInputStream("oras_data.properties")) {
            p.load(input);
            buget = Integer.parseInt(p.getProperty("buget", "100"));
            contorPescari = Integer.parseInt(p.getProperty("nr_pescari", "1"));
        } catch (IOException e) { buget = 100; contorPescari = 1; }
    }

    private void salveazaJoc() {
        Properties p = new Properties();
        p.setProperty("buget", String.valueOf(buget));
        p.setProperty("nr_pescari", String.valueOf(contorPescari));
        try (OutputStream output = new FileOutputStream("oras_data.properties")) { p.store(output, "Save"); } catch (IOException e) {}
    }

    public void setTaxa(int val) { this.valoareTaxa = val; }
    public void comandaMunca() { regimMuncaDublu = !regimMuncaDublu; if(myGui != null) myGui.updateWorkMode(regimMuncaDublu); }
    public void comandaColectareTaxe() { addBehaviour(new OneShotBehaviour() { @Override public void action() { trimiteOrdin("tax-collector", "colecteaza", ACLMessage.REQUEST); }}); }
    public void inchideTot() {
        trimiteSemnalTuturor("DIE");
        new Thread(() -> { try { Thread.sleep(1000); getContainerController().kill(); } catch (Exception e) {} }).start();
    }

    private void pornesteMonitorizareAgenti() {
        DFAgentDescription template = new DFAgentDescription();
        try {
            ACLMessage subscribeMsg = DFService.createSubscriptionMessage(this, getDefaultDF(), template, null);
            addBehaviour(new SubscriptionInitiator(this, subscribeMsg) {
                @Override
                protected void handleInform(ACLMessage inform) {
                    try {
                        DFAgentDescription[] results = DFService.decodeNotification(inform.getContent());
                        for (DFAgentDescription dfd : results) {
                            // Apelam metoda comuna
                            processAgentDiscovery(dfd.getName(), dfd.getAllServices());
                        }
                    } catch (Exception e) { e.printStackTrace(); }
                }
            });
        } catch (Exception e) { e.printStackTrace(); }
    }

    private AID getAgentFunctional(String tipServiciu, AID agentCache) {
        // 1. Daca il stim deja, il returnam
        if (agentCache != null) {
            return agentCache;
        }

        // 2. Daca nu, il cautam manual (Fallback)
        // Acest cod se executa DOAR la inceput, pana cand sistemul de Subscribe intra in ritm
        System.out.println("⚠️ Cache miss pentru " + tipServiciu + ". Caut manual in DF...");
        DFAgentDescription template = new DFAgentDescription();
        ServiceDescription sd = new ServiceDescription();
        sd.setType(tipServiciu);
        template.addServices(sd);

        try {
            DFAgentDescription[] result = DFService.search(this, template);
            if (result.length > 0) {
                System.out.println("✅ L-am gasit manual pe: " + result[0].getName().getLocalName());
                return result[0].getName();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null; // Chiar nu exista
    }

    public void removeAgentFromCache(AID agent) {
        agentiProductori.remove(agent);
    }

    @Override protected void takeDown() { salveazaJoc(); if (myGui != null) myGui.dispose(); }
}