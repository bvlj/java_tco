#!/usr/bin/env bash

BASE_DIR="$(dirname "$0")"
OUT_DIR="${BASE_DIR}/out"
INTERMEDIATE_DIR="${OUT_DIR}/intermediate"

if ! command -v dot &> /dev/null; then
  echo -e "\033[0;31mError: missing \"dot\" command. Please install graphviz: https://graphviz.org/download/\033[0m" 1>&2
  exit 1
fi

./gradlew run --args "${BASE_DIR}/src/test/resources/ExampleClass.class ${OUT_DIR}"

[ ! -d "${INTERMEDIATE_DIR}" ] && mkdir -p "${INTERMEDIATE_DIR}"

for f in "${BASE_DIR}"/out/*.dot; do
  dot -T pdf -o "${INTERMEDIATE_DIR}/$(basename "${f}").pdf" "${f}"
done

JOIN_CMD="/System/Library/Automator/Combine PDF Pages.action/Contents/MacOS/join"
if [ ! -f "${JOIN_CMD}" ]; then
  JOIN_CMD="/System/Library/Automator/Combine PDF Pages.action/Contents/Resources/join.py"
fi
if [ ! -f "${JOIN_CMD}" ]; then
  echo -e "\033[0;31mError: missing \033[1m/System/Library/Automator/Combine PDF Pages.action?\033[0m" 1>&2
  echo -e "Please join the PDF files in ${INTERMEDIATE_DIR} manually..." 1>&2
  exit 1
fi

"$JOIN_CMD" -o "${OUT_DIR}/all.pdf" "${INTERMEDIATE_DIR}"/*.pdf && \
  rm -r "${INTERMEDIATE_DIR}"
