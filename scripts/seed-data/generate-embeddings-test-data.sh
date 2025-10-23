#!/bin/bash

################################################################################
# Generate Synthetic Test Data for Embedding Performance Validation
#
# Story 1.4 - AC4: Creates 10K test documents with Vietnamese accounting terms
# using realistic data distribution for performance benchmarking.
#
# Usage: ./generate-embeddings-test-data.sh [output_dir]
#
# Output: test-docs.json with 10K documents:
#   - invoices: 5000
#   - payments: 3000
#   - journal entries: 2000
#
# Requirements: Python 3.8+, faker library
################################################################################

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
OUTPUT_DIR="${1:-$SCRIPT_DIR}"
OUTPUT_FILE="$OUTPUT_DIR/test-docs.json"
PYTHON_SCRIPT="$SCRIPT_DIR/generate_test_docs.py"

echo "==================================================================="
echo "Generating 10K Synthetic Test Documents for Embedding Performance"
echo "==================================================================="
echo ""
echo "Output directory: $OUTPUT_DIR"
echo "Output file: $OUTPUT_FILE"
echo ""

# Check if Python 3 is available
if ! command -v python3 &> /dev/null; then
    echo "ERROR: Python 3 is required but not found"
    exit 1
fi

# Check/install faker library
echo "Checking dependencies..."
if ! python3 -c "import faker" 2>/dev/null; then
    echo "Installing faker library..."
    pip3 install faker --break-system-packages 2>/dev/null || pip3 install faker --user
fi

# Create Python script
cat > "$PYTHON_SCRIPT" << 'PYTHON_SCRIPT_EOF'
#!/usr/bin/env python3
"""
Generate synthetic test data for embedding performance validation.
Story 1.4 - AC4: 10K documents with Vietnamese accounting terminology.
"""

import json
import random
import uuid
from datetime import datetime, timedelta
from faker import Faker

# Initialize Faker with Vietnamese locale
fake = Faker('vi_VN')
Faker.seed(42)  # Reproducible results

# Vietnamese accounting terms (Circular 200 compliant)
ACCOUNT_CODES = ['111', '112', '131', '331', '511', '621', '33311', '13311']
FISCAL_PERIODS = ['2024-01', '2024-02', '2024-03', '2024-04', '2024-05', 
                  '2024-06', '2024-07', '2024-08', '2024-09', '2024-10']

# Company IDs for multi-tenant testing
COMPANY_IDS = [str(uuid.uuid4()) for _ in range(5)]

def generate_invoice(invoice_num):
    """Generate synthetic invoice document."""
    company_id = random.choice(COMPANY_IDS)
    customer_name = fake.company()
    invoice_date = fake.date_between(start_date='-1y', end_date='today')
    amount = round(random.uniform(100000, 50000000), 2)
    
    return {
        "id": str(uuid.uuid4()),
        "company_id": company_id,
        "document_type": "invoice",
        "source_table": "invoices",
        "invoice_number": f"INV-{invoice_num:06d}",
        "customer_name": customer_name,
        "invoice_date": invoice_date.isoformat(),
        "total_amount": amount,
        "status": random.choice(["SENT", "PAID", "PARTIAL", "OVERDUE"]),
        "fiscal_period": random.choice(FISCAL_PERIODS),
        "description": f"Hóa đơn bán hàng cho {customer_name} - " + 
                      fake.sentence(nb_words=6),
        "module": "ar"
    }

def generate_payment(payment_num):
    """Generate synthetic payment document."""
    company_id = random.choice(COMPANY_IDS)
    customer_name = fake.company()
    payment_date = fake.date_between(start_date='-1y', end_date='today')
    amount = round(random.uniform(50000, 10000000), 2)
    
    return {
        "id": str(uuid.uuid4()),
        "company_id": company_id,
        "document_type": "payment",
        "source_table": "payments",
        "payment_number": f"PAY-{payment_num:06d}",
        "customer_name": customer_name,
        "payment_date": payment_date.isoformat(),
        "amount": amount,
        "payment_method": random.choice(["BANK_TRANSFER", "CASH", "CHECK"]),
        "fiscal_period": random.choice(FISCAL_PERIODS),
        "description": f"Thanh toán từ {customer_name}",
        "module": "ar"
    }

def generate_journal_entry(entry_num):
    """Generate synthetic journal entry document."""
    company_id = random.choice(COMPANY_IDS)
    entry_date = fake.date_between(start_date='-1y', end_date='today')
    debit_amount = round(random.uniform(100000, 20000000), 2)
    
    return {
        "id": str(uuid.uuid4()),
        "company_id": company_id,
        "document_type": "journal_entry",
        "source_table": "journal_entries",
        "entry_number": f"JE-{entry_num:06d}",
        "entry_date": entry_date.isoformat(),
        "total_debit": debit_amount,
        "total_credit": debit_amount,
        "status": "POSTED",
        "fiscal_period": random.choice(FISCAL_PERIODS),
        "description": "Bút toán điều chỉnh - " + fake.sentence(nb_words=8),
        "account_codes": ", ".join(random.sample(ACCOUNT_CODES, 3)),
        "module": "gl"
    }

def main():
    print("Generating 10,000 synthetic test documents...")
    print("Distribution: invoices=5000, payments=3000, journal_entries=2000")
    
    documents = []
    
    # Generate invoices (5000)
    print("Generating 5000 invoices...")
    for i in range(5000):
        documents.append(generate_invoice(i + 1))
    
    # Generate payments (3000)
    print("Generating 3000 payments...")
    for i in range(3000):
        documents.append(generate_payment(i + 1))
    
    # Generate journal entries (2000)
    print("Generating 2000 journal entries...")
    for i in range(2000):
        documents.append(generate_journal_entry(i + 1))
    
    # Shuffle for realistic distribution
    random.shuffle(documents)
    
    # Write to JSON file
    output = {
        "metadata": {
            "total_documents": len(documents),
            "generated_at": datetime.now().isoformat(),
            "distribution": {
                "invoices": 5000,
                "payments": 3000,
                "journal_entries": 2000
            }
        },
        "documents": documents
    }
    
    with open('test-docs.json', 'w', encoding='utf-8') as f:
        json.dump(output, f, ensure_ascii=False, indent=2)
    
    print(f"\n✅ Successfully generated {len(documents)} test documents")
    print(f"📄 Output file: test-docs.json")
    print(f"📊 File size: {len(json.dumps(output)) / 1024 / 1024:.2f} MB")

if __name__ == '__main__':
    main()
PYTHON_SCRIPT_EOF

# Make Python script executable
chmod +x "$PYTHON_SCRIPT"

# Run Python script
echo "Running data generation..."
cd "$OUTPUT_DIR"
python3 "$PYTHON_SCRIPT"

echo ""
echo "✅ Test data generation complete!"
echo ""
echo "Next steps:"
echo "  1. Use test-docs.json for performance benchmarking"
echo "  2. Run: ./gradlew test --tests *PerformanceTest"
echo "  3. Verify throughput ≥200 docs/min (AC4 requirement)"
echo ""

# Cleanup
rm -f "$PYTHON_SCRIPT"

