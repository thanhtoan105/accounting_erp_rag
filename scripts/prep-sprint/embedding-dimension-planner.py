#!/usr/bin/env python3
"""
Quick estimator for embedding dimension trade-offs.

Outputs Markdown table summarizing storage footprint and token costs
for candidate embedding models at the target Preparation Sprint scale.
"""

from __future__ import annotations

from dataclasses import dataclass
from typing import List, Tuple


@dataclass(frozen=True)
class EmbeddingOption:
    name: str
    provider: str
    dimension: int
    cost_per_1k_tokens_usd: float
    notes: str


AVG_TOKENS_PER_DOC = 650  # Assumption: masked Vietnamese accounting doc
TARGET_DOCUMENT_COUNTS = [100_000, 500_000, 1_000_000]
FLOAT32_BYTES = 4


def storage_mb_per_doc(dimension: int) -> float:
    """Return raw storage in MB for a single vector (no index overhead)."""
    return dimension * FLOAT32_BYTES / (1024 * 1024)


def estimate_storage(option: EmbeddingOption) -> List[Tuple[int, float]]:
    """Estimate storage footprint in GB for target document counts."""
    per_doc_mb = storage_mb_per_doc(option.dimension)
    estimates = []
    for count in TARGET_DOCUMENT_COUNTS:
        total_mb = per_doc_mb * count
        total_gb = total_mb / 1024
        estimates.append((count, total_gb))
    return estimates


def estimate_cost(option: EmbeddingOption) -> List[Tuple[int, float]]:
    """Estimate total embedding cost in USD for target document counts."""
    cost_per_doc = (AVG_TOKENS_PER_DOC / 1000) * option.cost_per_1k_tokens_usd
    return [(count, cost_per_doc * count) for count in TARGET_DOCUMENT_COUNTS]


def main() -> None:
    options: List[EmbeddingOption] = [
        EmbeddingOption(
            name="text-embedding-3-large",
            provider="Azure OpenAI",
            dimension=3072,
            cost_per_1k_tokens_usd=0.00013,
            notes="Highest accuracy; 2024-04 API; requires 3072-dim column.",
        ),
        EmbeddingOption(
            name="text-embedding-3-small",
            provider="Azure OpenAI",
            dimension=1536,
            cost_per_1k_tokens_usd=0.00002,
            notes="Balanced quality vs. cost; compatible with current schema.",
        ),
        EmbeddingOption(
            name="text-embedding-ada-002",
            provider="Azure OpenAI (legacy)",
            dimension=1536,
            cost_per_1k_tokens_usd=0.00010,
            notes="Legacy model; slower and lower recall; slated for deprecation.",
        ),
        EmbeddingOption(
            name="bge-base-vi-v1.5",
            provider="SentenceTransformers",
            dimension=768,
            cost_per_1k_tokens_usd=0.0,
            notes="Self-host; Vietnamese tuned; requires GPU for throughput.",
        ),
    ]

    header = (
        "| Model | Provider | Dim | Storage @100K (GB) | Storage @500K (GB) | "
        "Storage @1M (GB) | Cost @100K (USD) | Cost @500K (USD) | Cost @1M (USD) | Notes |\n"
        "|-------|----------|-----|--------------------|--------------------|"
        "------------------|-----------------|-----------------|----------------|-------|"
    )
    print(header)
    for option in options:
        storage_estimates = estimate_storage(option)
        cost_estimates = estimate_cost(option)
        row = (
            f"| {option.name} | {option.provider} | {option.dimension} | "
            f"{storage_estimates[0][1]:.2f} | {storage_estimates[1][1]:.2f} | "
            f"{storage_estimates[2][1]:.2f} | "
            f"{cost_estimates[0][1]:.2f} | {cost_estimates[1][1]:.2f} | "
            f"{cost_estimates[2][1]:.2f} | {option.notes} |"
        )
        print(row)


if __name__ == "__main__":
    main()
