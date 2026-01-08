# AgentCity - Simulare Urbană Multi-Agent (JADE)

**AgentCity** este un proiect de simulare a unui ecosistem de cetățeni a unei comunități mici, implementat în Java folosind framework-ul **JADE** (Java Agent DEvelopment Framework). Proiectul demonstrează interacțiuni complexe între agenți autonomi, optimizarea protocoalelor de comunicare și integrarea cu modele de limbaj (LLM) pentru analiză narativă.

## 1. Descrierea Problemei

Simularea modelează un sat mic condus de un **Lider** (Primar), populat de cetățeni cu roluri specifice. Obiectivul este menținerea echilibrului economic și social prin cooperare și schimb de servicii.

### Dinamica Sistemului:
* **Ciclu Zi/Noapte:** Liderul dictează ritmul de viață (Muncă vs. Odihnă) prin mesaje de tip *Broadcast*.
* **Economie:** Pescarii produc resurse (pește) pe care le vând Negustorilor pentru a obține bani.
* **Sănătate:** Munca consumă energie. Dacă energia scade critic, cetățenii caută Medicul pentru tratament (contra cost).
* **Fiscalitate:** Un Colector de taxe intervine periodic pentru a strânge fonduri la bugetul local, necesare pentru menținerea orașului.
* **Inteligență Artificială:** Un agent "Creier" (bazat pe Google Gemini) analizează periodic datele agregate ale orașului și oferă rapoarte narative despre starea populației.

### Arhitectura Agenților:
1.  **LiderAgent:** Coordonatorul central, gestionează timpul și GUI-ul.
2.  **PescarAgent:** Muncitor cu mașină de stări internă (Energie, Bani, Stare Spirit).
3.  **ColectorAgent:** Agent fiscal reactiv.
4.  **MedicAgent & NegustorAgent:** Furnizori de servicii.
5.  **LlmAgent:** Interfață către API-ul AI (Python Flask server).

---

## 2. Cerințe de Sistem (Prerechizite)

Pentru a rula aplicația, asigurați-vă că aveți instalate următoarele:

* **Java Development Kit (JDK) 1.8**: Necesar pentru compatibilitatea cu JADE.
* **Python 3.10+**: Necesar pentru rularea micro-serviciului LLM.
* **Google Gemini API Key**: O cheie validă (va fi disponibila în formular)

---

## 3. Instalare și Configurare

Proiectul vine cu un lansator automatizat (`StartAgentCity.bat`) care se ocupă de instalarea dependențelor.
