"""Формат Anthropic Messages API: разбор SSE-стрима, сборка сообщения, обратная сериализация.

Нужен Output Guard: ответ проверяется целиком до отдачи клиенту, поэтому стрим сначала
собирается в обычное сообщение, а при блокировке/маскировании пересобирается в SSE заново.
"""

from __future__ import annotations

import json
from typing import Any, Optional


def parse_sse(raw: bytes) -> list[dict]:
    """Список data-событий SSE (ping и нераспознанное пропускаются)."""
    events: list[dict] = []
    text = raw.decode("utf-8", errors="replace").replace("\r\n", "\n")
    for chunk in text.split("\n\n"):
        data_lines = [ln[5:].lstrip() for ln in chunk.split("\n") if ln.startswith("data:")]
        if not data_lines:
            continue
        try:
            ev = json.loads("\n".join(data_lines))
        except json.JSONDecodeError:
            continue
        if isinstance(ev, dict) and ev.get("type") != "ping":
            events.append(ev)
    return events


def message_from_events(events: list[dict]) -> Optional[dict]:
    """Собирает итоговое сообщение из событий стрима. None — если стрим не распознан."""
    message: Optional[dict] = None
    blocks: dict[int, dict] = {}
    partial_json: dict[int, str] = {}
    for ev in events:
        etype = ev.get("type")
        if etype == "message_start":
            message = json.loads(json.dumps(ev.get("message", {})))
            message.setdefault("usage", {})
        elif etype == "content_block_start":
            blocks[ev["index"]] = dict(ev.get("content_block", {}))
            partial_json[ev["index"]] = ""
        elif etype == "content_block_delta":
            blk = blocks.get(ev.get("index"))
            delta = ev.get("delta", {})
            if blk is None:
                continue
            dtype = delta.get("type")
            if dtype == "text_delta":
                blk["text"] = blk.get("text", "") + delta.get("text", "")
            elif dtype == "input_json_delta":
                partial_json[ev["index"]] += delta.get("partial_json", "")
            elif dtype == "thinking_delta":
                blk["thinking"] = blk.get("thinking", "") + delta.get("thinking", "")
            elif dtype == "signature_delta":
                blk["signature"] = delta.get("signature", "")
        elif etype == "content_block_stop":
            idx = ev.get("index")
            blk = blocks.get(idx)
            if blk is not None and blk.get("type") in ("tool_use", "server_tool_use") and partial_json.get(idx):
                try:
                    blk["input"] = json.loads(partial_json[idx])
                except json.JSONDecodeError:
                    blk["input"] = {"_raw_partial_json": partial_json[idx]}
        elif etype == "message_delta" and message is not None:
            delta = ev.get("delta", {})
            for k in ("stop_reason", "stop_sequence"):
                if k in delta:
                    message[k] = delta[k]
            message["usage"].update(ev.get("usage", {}))
    if message is None:
        return None
    message["content"] = [blocks[i] for i in sorted(blocks)]
    return message


def _sse(event: str, data: dict) -> bytes:
    return f"event: {event}\ndata: {json.dumps(data, ensure_ascii=False)}\n\n".encode("utf-8")


def message_to_sse(message: dict) -> bytes:
    """Сообщение → корректный SSE-стрим (для подменённых/замаскированных ответов)."""
    usage = message.get("usage", {})
    start = {k: v for k, v in message.items() if k not in ("content", "stop_reason", "stop_sequence")}
    start.update({"content": [], "stop_reason": None, "stop_sequence": None})
    start["usage"] = {**usage, "output_tokens": 1}
    out = [_sse("message_start", {"type": "message_start", "message": start})]
    for i, block in enumerate(message.get("content", [])):
        btype = block.get("type")
        if btype == "text":
            head = {"type": "text", "text": ""}
            delta = {"type": "text_delta", "text": block.get("text", "")}
        elif btype in ("tool_use", "server_tool_use"):
            head = {**{k: v for k, v in block.items() if k != "input"}, "input": {}}
            delta = {"type": "input_json_delta", "partial_json": json.dumps(block.get("input", {}), ensure_ascii=False)}
        elif btype == "thinking":
            head = {"type": "thinking", "thinking": ""}
            delta = {"type": "thinking_delta", "thinking": block.get("thinking", "")}
        else:
            head, delta = block, None
        out.append(_sse("content_block_start", {"type": "content_block_start", "index": i, "content_block": head}))
        if delta is not None:
            out.append(_sse("content_block_delta", {"type": "content_block_delta", "index": i, "delta": delta}))
        if btype == "thinking" and block.get("signature"):
            out.append(
                _sse(
                    "content_block_delta",
                    {
                        "type": "content_block_delta",
                        "index": i,
                        "delta": {"type": "signature_delta", "signature": block["signature"]},
                    },
                )
            )
        out.append(_sse("content_block_stop", {"type": "content_block_stop", "index": i}))
    out.append(
        _sse(
            "message_delta",
            {
                "type": "message_delta",
                "delta": {"stop_reason": message.get("stop_reason"), "stop_sequence": message.get("stop_sequence")},
                "usage": {"output_tokens": usage.get("output_tokens", 0)},
            },
        )
    )
    out.append(_sse("message_stop", {"type": "message_stop"}))
    return b"".join(out)


def text_message(original: dict, text: str) -> dict:
    """Одно текстовое сообщение с тем же id/model/usage (стоимость уже понесена)."""
    return {
        "id": original.get("id", "msg_gateway"),
        "type": "message",
        "role": "assistant",
        "model": original.get("model", ""),
        "content": [{"type": "text", "text": text}],
        "stop_reason": "end_turn",
        "stop_sequence": None,
        "usage": original.get("usage", {"input_tokens": 0, "output_tokens": 0}),
    }


def iter_strings(node: Any):
    """Все строковые значения вложенной структуры (для сканирования tool_use.input)."""
    if isinstance(node, str):
        yield node
    elif isinstance(node, dict):
        for v in node.values():
            yield from iter_strings(v)
    elif isinstance(node, list):
        for v in node:
            yield from iter_strings(v)
