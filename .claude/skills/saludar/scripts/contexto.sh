#!/usr/bin/env bash
# Imprime el contexto actual para el saludo: fecha, hora, franja del día y clima.
# Uso: contexto.sh [zona_horaria] [ciudad]
# Por defecto: America/Santiago y "Santiago,Chile".
set -u

TZ_CIUDAD="${1:-America/Santiago}"
CIUDAD="${2:-Santiago,Chile}"

export TZ="$TZ_CIUDAD"
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
clima=$(curl -fsS -m 6 "https://wttr.in/${CIUDAD}?format=%t+%C&lang=es" 2>/dev/null | tr -d '\n')
case "$clima" in
  *Unknown*|*unknown*|*Sorry*|*rror*) clima="" ;;
esac

# Día de la semana con mayúscula inicial (date lo entrega en minúscula).
dia="$(printf '%s' "${dia:0:1}" | tr '[:lower:]' '[:upper:]')${dia:1}"

echo "franja=$franja"
echo "dia=$dia"
echo "fecha=$fecha"
echo "hora=$hora"
echo "zona=$TZ_CIUDAD"
echo "clima=${clima:-NO_DISPONIBLE}"
