# Vector Database Backup & Restore Runbook
## Story 1.3 - AC5: Vector Data Protection & Recovery

**Document Version**: 1.0  
**Last Updated**: 2025-10-21  
**Maintained By**: Platform Engineering Team

---

## Overview

This runbook provides procedures for backing up and restoring vector document data in the `accounting.vector_documents` table, ensuring data protection and business continuity for the RAG platform.

### Critical Data Assets

| Asset | Table | Size (Est.) | RPO | RTO |
|-------|-------|-------------|-----|-----|
| Vector Embeddings | `accounting.vector_documents` | 100K docs = ~600MB | 1 hour | 4 hours |
| Metadata | `metadata` JSONB column | Included above | 1 hour | 4 hours |
| HNSW Index | `idx_vector_documents_embedding_hnsw` | ~200MB @ 100K docs | Can rebuild | 2 hours |

**Recovery Point Objective (RPO)**: 1 hour - Maximum acceptable data loss  
**Recovery Time Objective (RTO)**: 4 hours - Maximum acceptable downtime

---

## Backup Strategies

### 1. Supabase Point-in-Time Recovery (PITR) - **Recommended**

**Supabase Dashboard**
