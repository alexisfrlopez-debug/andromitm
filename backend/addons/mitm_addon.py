#!/usr/bin/env python3
"""
AndroMITM — addon mitmproxy (mitmdump)
Transmet chaque flux HTTP/HTTPS au backend Node via POST /api/internal/flow.
Le sink est défini par la variable d'environnement MITMADDON_SINK.

Usage (géré par le module proxy) :
  mitmdump -s mitm_addon.py [--mode transparent | --listen-port 8080]

Silencieux en cas d'échec réseau du sink : l'interception ne doit pas casser.
"""
import json
import os
import threading

from mitmproxy import http

SINK = os.environ.get("MITMADDON_SINK", "http://127.0.0.1:8443/api/internal/flow")

try:
    import urllib.request
except ImportError:  # pragma: no cover
    urllib = None

_lock = threading.Lock()


def _post(payload: dict):
    if urllib is None:
        return
    data = json.dumps(payload).encode("utf-8")
    req = urllib.request.Request(
        SINK, data=data, headers={"Content-Type": "application/json"}, method="POST"
    )
    try:
        with _lock:
            urllib.request.urlopen(req, timeout=2)
    except Exception:
        pass  # jamais casser le flux pour un problème de sink


def response(flow: http.HTTPFlow) -> None:
    """Hook appelé à la réception de la réponse (mode non-streaming)."""
    try:
        req, resp = flow.request, flow.response
        content_type = (resp.headers.get("content-type") or "")[:80]
        payload = {
            "ts": __import__("datetime").datetime.now().isoformat(timespec="seconds"),
            "method": req.method,
            "scheme": req.scheme,
            "host": req.host,
            "port": req.port,
            "path": req.path[:512],
            "status": resp.status_code,
            "content_type": content_type,
            "client_ip": flow.client_conn.peername[0] if flow.client_conn.peername else None,
            "request_size": len(req.raw_content or b""),
            "response_size": len(resp.raw_content or b""),
            # métadonnées uniquement : jamais de corps complet ni de secrets dumpés
            "auth_present": bool(req.headers.get("authorization") or req.headers.get("cookie")),
        }
        _post(payload)
    except Exception:
        pass
