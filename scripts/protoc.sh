#!/bin/sh
set -eu

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
ROOT_DIR=$(CDPATH= cd -- "${SCRIPT_DIR}/.." && pwd)
TOOLS_DIR="${ROOT_DIR}/.tools"
PROTOC_VERSION="${PROTOC_VERSION:-29.3}"
PROTOC_DIR="${TOOLS_DIR}/protoc-${PROTOC_VERSION}"
PROTOC_BIN="${PROTOC_DIR}/bin/protoc"

if [ ! -x "${PROTOC_BIN}" ]; then
  mkdir -p "${TOOLS_DIR}"
  TMP_DIR=$(mktemp -d)
  trap 'rm -rf "${TMP_DIR}"' EXIT INT TERM
  ARCHIVE="protoc-${PROTOC_VERSION}-linux-x86_64.zip"
  URL="https://github.com/protocolbuffers/protobuf/releases/download/v${PROTOC_VERSION}/${ARCHIVE}"
  curl -L --fail --silent --show-error "${URL}" -o "${TMP_DIR}/protoc.zip"
  unzip -qo "${TMP_DIR}/protoc.zip" -d "${PROTOC_DIR}"
fi

exec "${PROTOC_BIN}" "$@"
