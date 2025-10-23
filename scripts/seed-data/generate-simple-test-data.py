#!/usr/bin/env python3
"""
Generate Simple Synthetic Test Data for Story 1.4
No external dependencies required
"""

import json
import random
from datetime import datetime, timedelta
from uuid import uuid4

def generate_test_data():
    """Generate 10K test documents without faker library"""
    
    # Vietnamese company names and customer names (simple examples)
    company_names = [
        "Công ty TNHH Thương mại Hà Nội",
        "Công ty Cổ phần Xây dựng Việt Nam", 
        "Công ty TNHH Sản xuất Thành Phố Hồ Chí Minh",
        "Công ty Cổ phần Tiết kiệm Điện lực",
        "Công ty TNHH Dịch vụ Kế toán",
    ]
    
    descriptions = [
        "Hóa đơn bán hàng điện tử",
        "Phiếu thu tiền mặt",
        "Chứng từ kế toán",
        "Hóa đơn GTGT số",
        "Phiếu chi tiết kiệm",
    ]
    
    # Base company ID
    company_id = str(uuid4())
    
    documents = []
    start_date = datetime(2024, 1, 1)
    
    print(f"Generating 10,000 test documents...")
    
    # Generate 5000 invoices
    for i in range(5000):
        doc = {
            "id": str(uuid4()),
            "company_id": company_id,
            "document_type": "invoice",
            "source_table": "invoices",
            "invoice_number": f"INV-{i+1:06d}",
            "customer_name": random.choice(company_names),
            "description": random.choice(descriptions),
            "total_amount": round(random.uniform(100000, 50000000), 2),
            "issue_date": (start_date + timedelta(days=random.randint(0, 300))).isoformat(),
            "status": random.choice(["draft", "posted", "paid"]),
            "fiscal_period": "2024-" + str(random.randint(1, 12)).zfill(2),
        }
        documents.append(doc)
        
        if (i + 1) % 1000 == 0:
            print(f"  Generated {i+1}/5000 invoices")
    
    # Generate 3000 payments
    for i in range(3000):
        doc = {
            "id": str(uuid4()),
            "company_id": company_id,
            "document_type": "payment",
            "source_table": "payments",
            "payment_number": f"PMT-{i+1:06d}",
            "customer_name": random.choice(company_names),
            "description": f"Thanh toán {random.choice(['tiền mặt', 'chuyển khoản', 'séc'])}",
            "amount": round(random.uniform(50000, 100000000), 2),
            "payment_date": (start_date + timedelta(days=random.randint(0, 300))).isoformat(),
            "status": random.choice(["pending", "completed", "cancelled"]),
            "fiscal_period": "2024-" + str(random.randint(1, 12)).zfill(2),
        }
        documents.append(doc)
        
        if (i + 1) % 1000 == 0:
            print(f"  Generated {i+1}/3000 payments")
    
    # Generate 2000 journal entries
    for i in range(2000):
        doc = {
            "id": str(uuid4()),
            "company_id": company_id,
            "document_type": "journal_entry",
            "source_table": "journal_entries",
            "entry_number": f"JE-{i+1:06d}",
            "description": f"Bút toán kế toán - {random.choice(['Chi phí', 'Doanh thu', 'Tài sản', 'Nợ phải trả'])}",
            "total_debit": round(random.uniform(1000000, 500000000), 2),
            "total_credit": round(random.uniform(1000000, 500000000), 2),
            "entry_date": (start_date + timedelta(days=random.randint(0, 300))).isoformat(),
            "status": random.choice(["draft", "posted"]),
            "fiscal_period": "2024-" + str(random.randint(1, 12)).zfill(2),
        }
        documents.append(doc)
        
        if (i + 1) % 1000 == 0:
            print(f"  Generated {i+1}/2000 journal entries")
    
    result = {
        "metadata": {
            "total_documents": len(documents),
            "generated_at": datetime.now().isoformat(),
            "distribution": {
                "invoices": 5000,
                "payments": 3000,
                "journal_entries": 2000
            },
            "company_id": company_id
        },
        "documents": documents
    }
    
    return result

if __name__ == "__main__":
    print("=" * 70)
    print("Generating Synthetic Test Data (No External Dependencies)")
    print("=" * 70)
    print()
    
    data = generate_test_data()
    
    output_file = "test-docs.json"
    print(f"\nWriting to {output_file}...")
    with open(output_file, 'w', encoding='utf-8') as f:
        json.dump(data, f, ensure_ascii=False, indent=2)
    
    print(f"\n✓ Successfully generated {data['metadata']['total_documents']} documents")
    print(f"  - Invoices: {data['metadata']['distribution']['invoices']}")
    print(f"  - Payments: {data['metadata']['distribution']['payments']}")
    print(f"  - Journal Entries: {data['metadata']['distribution']['journal_entries']}")
    print(f"\nOutput file: {output_file}")
    print()

