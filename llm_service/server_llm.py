from flask import Flask, request, jsonify
import google.generativeai as genai
from pydantic import BaseModel, Field

# --- CONFIGURARE ---
API_KEY = "AIzaSyDE5zUtXzZ8RcVAdl0rX-U0y3hRAvOEib0"

genai.configure(api_key=API_KEY)
model = genai.GenerativeModel('gemini-2.5-flash-lite')

app = Flask(__name__)


#Valideaza datele in mod PYDANTIC
class AgentRequest(BaseModel):
    agent_role: str = Field(..., description="Rolul agentului (ex: Pescar, Primar)")
    context: str = Field(..., description="Ce s-a intamplat in simulare")
    instruction: str = Field(..., description="Ce trebuie sa faca LLM-ul")

# --- ENDPOINT-UL ---
@app.route('/ask', methods=['POST'])
def ask_gemini():
    try:
        # 1. Primim datele JSON
        data = request.json

        # 2. Le validam folosind Pydantic
        # Daca datele sunt gresite, Pydantic va da eroare automat
        agent_req = AgentRequest(**data)

        # 3. Construim prompt-ul pentru RPG
        prompt = (
            f"Ești un personaj într-un joc de simulare a unui oraș mic.\n"
            f"Rolul tău: {agent_req.agent_role}.\n"
            f"Context situație: {agent_req.context}.\n"
            f"Sarcina ta: {agent_req.instruction}.\n\n"
            f"Răspunde scurt (maxim 2 fraze), în caracter, cu emoție și personalitate."
        )

        # 4. Trimitem la Gemini
        response = model.generate_content(prompt)

        # 5. Returnam raspunsul catre Java
        return jsonify({
            "status": "success",
            "reply": response.text.strip()
        })

    except Exception as e:
        print(f"Eroare: {e}")
        return jsonify({"status": "error", "message": str(e)}), 500

if __name__ == '__main__':
    print("Serverul LLM a pornit pe portul 5000...")
    app.run(host='0.0.0.0', port=5000)