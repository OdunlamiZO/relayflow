#!/bin/sh
set -eu

replace() {
  placeholder="$1"
  value="$2"

  if [ -n "${value:-}" ] && [ "$value" != "$placeholder" ]; then
    grep -rl -- "$placeholder" /app --include="*.js" --include="*.html" 2>/dev/null \
      | xargs -r sed -i "s|$placeholder|$value|g"
  fi
}

replace "__RELAYFLOW_RUNTIME_NEXT_PUBLIC_API_BASE_URL__" "${NEXT_PUBLIC_API_BASE_URL:-}"

exec "$@"
