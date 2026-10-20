---
name: saludar
description: Saluda al usuario de forma cálida y personal con el día, la fecha, la hora y el clima actual de su ciudad (Santiago de Chile), más un pensamiento célebre (p. ej. un proverbio chino) para motivar su día. Úsalo siempre que el usuario te salude, en cualquier forma o registro - "hola", "holaa", "hola, ¿qué tal?", "¿cómo estás?", "buenas", "buen día", "buenos días", "buenas tardes/noches", "hey", "qué onda", "wena", "saludos" - tanto si el saludo viene solo como si abre un mensaje con otra petición (en ese caso saluda primero y luego atiende la petición). No lo uses cuando "hola" o "saludo" sean parte del trabajo técnico (un endpoint /hola, un "Hola mundo", una clase SaludoResource) y no un saludo dirigido a ti.
---

# Saludar

El usuario empieza su jornada o su conversación saludándote. Este skill convierte ese momento en un saludo breve que le dé tres cosas útiles: dónde está en el tiempo (día, fecha, hora), cómo está el día afuera (clima) y un empujón de ánimo (un pensamiento célebre). Todo debe sentirse cercano y natural, no como un informe.

## 1. Obtener el contexto real

Ejecuta el script incluido; entrega todo lo que necesitas en una sola llamada:

```bash
bash .claude/skills/saludar/scripts/contexto.sh
```

Salida de ejemplo:

```
franja=Buenas tardes
dia=Martes
fecha=20 de octubre de 2026
hora=15:27
zona=America/Santiago
clima=+21°C Aguacero ligero
```

Usa estos valores tal cual. La fecha, la hora y el clima cambian en cada conversación, y un dato inventado o tomado de memoria es justo el tipo de error que el usuario notaría de inmediato.

Si `clima=NO_DISPONIBLE`, intenta con la herramienta MCP `mcp__spring-ai-weather__getCurrentWeatherTemperature` (`city: "Santiago"`; si es diferida, cárgala antes con ToolSearch). Si tampoco responde, omite la línea del clima en lugar de adivinarla.

## 2. Escribir el saludo

Usa esta estructura:

```
¡<franja>, Andrés! 👋 <una frase breve y cálida, que responda si te preguntó cómo estás>

📅 <dia> <fecha>
🕐 <hora> en Santiago
🌦️ <clima, p. ej. 21 °C, aguacero ligero>

💭 «<pensamiento célebre>»
— <autor u origen>

<cierre corto: buen deseo para el día o "¿en qué te ayudo hoy?">
```

- **Tono**: amable, cercano y en español neutro con toque chileno solo si el usuario lo usa. Breve: el saludo no debe robar protagonismo al trabajo que viene.
- **Si te pregunta "¿cómo estás?"**, respóndele de verdad en una frase antes de seguir; ignorarlo sonaría robótico.
- **Emoji de clima** acorde a la condición: ☀️ despejado, ⛅ nublado parcial, ☁️ nublado, 🌧️/🌦️ lluvia, ⛈️ tormenta, 🌫️ niebla, ❄️ nieve. Puedes añadir un consejo breve si el clima lo amerita (paraguas, abrigo, protector solar).
- **Si el mensaje trae otra petición** (por ejemplo, "hola, ¿me ayudas con el endpoint de productos?"), da el saludo completo primero, en pocas líneas, y luego continúa con la petición en el mismo turno. El saludo no reemplaza el trabajo.

## 3. Elegir el pensamiento célebre

El objetivo es motivar, así que el pensamiento debe ser auténtico y caer bien al empezar o retomar el día.

- Prefiere proverbios chinos; de vez en cuando usa otros pensadores (Confucio, Lao Tse, Sun Tzu, Séneca, Marco Aurelio) para variar.
- Debe ser real y estar bien atribuido. Muchas frases circulan con autores falsos; si dudas de la autoría, atribúyelo como "Proverbio chino" o "Proverbio popular" en vez de a una persona.
- No repitas un pensamiento ya usado en la misma conversación.
- Conéctalo con el momento cuando sea natural: constancia por la mañana, perseverancia por la tarde, calma por la noche.

Ejemplos del tipo de pensamiento buscado:

- «El mejor momento para plantar un árbol fue hace veinte años. El segundo mejor momento es ahora.» — Proverbio chino
- «No temas avanzar lentamente; teme solo quedarte quieto.» — Proverbio chino
- «Un viaje de mil millas comienza con un solo paso.» — Lao Tse (Tao Te Ching)
- «Saber que se sabe lo que se sabe y que no se sabe lo que no se sabe: he aquí el verdadero saber.» — Confucio (Analectas)

Ojo con «No importa lo lento que vayas, siempre y cuando no te detengas»: suele atribuirse a Confucio pero no aparece en las Analectas, así que preséntalo como "Proverbio popular".

## Ejemplo completo

Usuario: "hola, ¿cómo estás?"

```
¡Buenas tardes, Andrés! 👋 Muy bien, gracias por preguntar, y con ganas de ayudarte.

📅 Martes 20 de octubre de 2026
🕐 15:27 en Santiago
🌦️ 21 °C, aguacero ligero (lleva paraguas si sales)

💭 «No temas avanzar lentamente; teme solo quedarte quieto.»
— Proverbio chino

¡Que tengas una tarde muy productiva! ¿En qué te ayudo hoy?
```
