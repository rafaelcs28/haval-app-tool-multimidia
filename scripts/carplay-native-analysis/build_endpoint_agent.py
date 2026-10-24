#!/usr/bin/env python3
"""Bundle the complete Frida Java endpoint offline; never contact the head unit."""
import argparse
import json
from pathlib import Path
import subprocess
import tempfile

from cold_passthrough_controller import (
    REMOTE_BRIDGE, endpoint_agent_entry, read_endpoint_bundle, sha256_bytes,
)

ROOT = Path(__file__).resolve().parent
AGENT = ROOT / "agent"


def build(output: Path) -> dict:
    manifest_path = output.with_suffix(output.suffix + ".manifest.json")
    if output.exists() or manifest_path.exists():
        raise ValueError("Use a new output path; existing bundles are never overwritten")
    package = json.loads((AGENT / "package.json").read_text())
    versions = {}
    for name in ("frida-java-bridge", "frida-compile"):
        actual = json.loads((AGENT / "node_modules" / name / "package.json").read_text())["version"]
        expected = (package.get("dependencies", {}) | package.get("devDependencies", {}))[name]
        if actual != expected:
            raise ValueError("Installed dependency differs from pinned version: " + name)
        versions[name] = actual
    compiler = AGENT / "node_modules/.bin/frida-compile"
    source = (ROOT / "carplay_clean_map_endpoint.js").read_text(encoding="utf-8")
    entry = endpoint_agent_entry(source)
    output.parent.mkdir(parents=True, exist_ok=True)
    with tempfile.TemporaryDirectory(prefix=".bundle-", dir=AGENT) as temporary:
        (Path(temporary) / "node_modules").symlink_to(AGENT / "node_modules", target_is_directory=True)
        entry_path = Path(temporary) / "agent.js"
        entry_path.write_text(entry, encoding="utf-8")
        subprocess.run([str(compiler), str(entry_path), "-S", "-o", str(output)], check=True, cwd=temporary)
    payload = output.read_bytes()
    manifest = {
        "kind": "impulse-clean-map-frida-agent-v1",
        "bundleSha256": sha256_bytes(payload),
        "bundleBytes": len(payload),
        "entrySha256": sha256_bytes(entry.encode()),
        "packageLockSha256": sha256_bytes((AGENT / "package-lock.json").read_bytes()),
        "javaBridgeVersion": versions["frida-java-bridge"],
        "compilerVersion": versions["frida-compile"],
        "bridgePath": REMOTE_BRIDGE,
        "androidRuntimeValidated": False,
    }
    manifest_path.write_text(json.dumps(manifest, indent=2) + "\n", encoding="utf-8")
    read_endpoint_bundle(output)
    return manifest


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--output", type=Path, default=AGENT / "dist/clean-map-agent.js")
    args = parser.parse_args()
    print(json.dumps(build(args.output.resolve()), indent=2))


if __name__ == "__main__":
    main()
