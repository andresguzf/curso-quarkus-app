#!/usr/bin/env bash
# Imprime el contexto actual para el saludo: fecha, hora, franja del día y clima.
# No contiene datos personales: la zona horaria sale del sistema y la ciudad
# del clima se deriva de ella. Se pueden sobrescribir con variables de entorno
# locales (no versionadas):
#   SALUDAR_TZ       p. ej. "America/Mexico_City"
#   SALUDAR_CIUDAD   p. ej. "Ciudad de Mexico" (más precisa para el clima)
set -u

zona_sistema() {
  if [ -L /etc/localtime ]; then
    readlink /etc/localtime | sed 's#.*/zoneinfo/##'
  elif [ -f /etc/timezone ]; then
    cat /etc/timezone
  else
    echo "UTC"
  fi
}

ZONA="${SALUDAR_TZ:-$(zona_sistema)}"
# "America/Mexico_City" -> "Mexico City"; "America/Argentina/Buenos_Aires" -> "Buenos Aires"
CIUDAD="${SALUDAR_CIUDAD:-$(printf '%s' "${ZONA##*/}" | tr '_' ' ')}"

export TZ="$ZONA"
export LC_ALL=es_ES.UTF-8

dia=$(date "+%A")
fecha=$(date "+%-d de %B de %Y")
hora=$(date "+%H:%M")
h=$((10#$(date "+%H")))

if   [ "$h" -ge 6 ]  && [ "$h" -lt 12 ]; then franja="Buenos días"
elif [ "$h" -ge 12 ] && [ "$h" -lt 20 ]; then franja="Buenas tardes"
else franja="Buenas noches"
fi

# Clima desde wttr.in (temperatura + condición en español). Vacío si falla.
ciudad_url=$(printf '%s' "$CIUDAD" | sed 's/ /+/g')
clima=$(curl -fsS -m 6 "https://wttr.in/${ciudad_url}?format=%t+%C&lang=es" 2>/dev/null | tr -d '\n')
case "$clima" in
  *Unknown*|*unknown*|*Sorry*|*rror*) clima="" ;;
esac

# Día de la semana con mayúscula inicial (date lo entrega en minúscula).
dia="$(printf '%s' "${dia:0:1}" | tr '[:lower:]' '[:upper:]')${dia:1}"

echo "franja=$franja"
echo "dia=$dia"
echo "fecha=$fecha"
echo "hora=$hora"
echo "clima=${clima:-NO_DISPONIBLE}"
