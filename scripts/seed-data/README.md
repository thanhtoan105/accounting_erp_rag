# Synthetic Test Data Generation

This directory contains scripts for generating synthetic test data for embedding performance validation.

## Story 1.4 - AC4: Embedding Performance Validation

**Requirement**: Process 10K documents in <30 minutes with throughput ≥200 documents/minute.

## Scripts

### `generate-embeddings-test-data.sh`

Generates 10,000 synthetic ERP documents with Vietnamese accounting terminology for performance benchmarking.

**Usage:**
```bash
./generate-embeddings-test-data.sh [output_dir]
```

**Output:**
- File: `test-docs.json`
- Size: ~2-3 MB
- Format: JSON with metadata and document array

**Document Distribution:**
- Invoices: 5,000 (50%)
- Payments: 3,000 (30%)
- Journal Entries: 2,000 (20%)

**Data Characteristics:**
- Vietnamese company names using Faker library
- Realistic accounting amounts (100K - 50M VND)
- Circular 200 compliant account codes (111, 112, 131, etc.)
- Multi-tenant: 5 different company_ids
- Fiscal periods: 2024-01 through 2024-10
- Vietnamese descriptions with diacritics (ó, ơ, á, ế, etc.)

## Requirements

- Python 3.8+
- faker library: `pip install faker`

## Vietnamese Locale

The script uses Vietnamese locale (`vi_VN`) to generate:
- Vietnamese company names
- Realistic Vietnamese addresses
- UTF-8 encoded text with diacritics

## Performance Testing

Use the generated data for:

1. **Throughput Benchmarking**:
   ```bash
   # Run embedding worker with test data
   # Expected: ≥200 docs/min throughput
   ```

2. **Latency Testing**:
   - Measure P95 latency for 10K document batch
   - Target: <30 minutes total (average 180ms per document)

3. **UTF-8 Validation**:
   - Verify Vietnamese diacritics preserved through pipeline
   - Check: ó, ơ, á, ế, ư, etc.

## Data Model

Each document includes:
- `id`: UUID
- `company_id`: UUID (multi-tenant)
- `document_type`: "invoice" | "payment" | "journal_entry"
- `source_table`: Source table name
- `fiscal_period`: YYYY-MM format
- `description`: Vietnamese text with diacritics
- `module`: "ar" | "gl"
- Type-specific fields (invoice_number, amount, etc.)

## Regeneration

To regenerate test data with new seed:
```bash
# Edit script and change Faker.seed(42) to a new value
# Then run:
./generate-embeddings-test-data.sh
```

## Future Enhancements

Planned for Story 2.12:
- Bill/vendor documents (AP module)
- Customer master records
- Bank transactions
- Edge cases (null fields, special characters)
- Performance data at 50K, 100K scales

