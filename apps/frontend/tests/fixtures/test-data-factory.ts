import { faker } from '@faker-js/faker';

export class TestDataFactory {
  private static readonly DEFAULT_COMPANY_ID = 'test-company-id';

  static createValidQueryRequest(overrides: Partial<any> = {}): any {
    return {
      companyId: overrides.companyId || this.DEFAULT_COMPANY_ID,
      query: overrides.query || this.generateRandomQuery(),
      language: overrides.language || 'en',
      filters: overrides.filters || {},
    };
  }

  static createValidQueryRequestVietnamese(overrides: Partial<any> = {}): any {
    return this.createValidQueryRequest({
      query: overrides.query || this.generateRandomVietnameseQuery(),
      language: 'vi',
      ...overrides,
    });
  }

  static createQueryWithFilters(filters: any): any {
    return this.createValidQueryRequest({
      filters,
    });
  }

  static createInvalidQueryRequest(type: 'empty' | 'too-long' | 'invalid-language'): any {
    switch (type) {
      case 'empty':
        return this.createValidQueryRequest({ query: '' });
      case 'too-long':
        return this.createValidQueryRequest({ query: 'a'.repeat(501) });
      case 'invalid-language':
        return this.createValidQueryRequest({ language: 'invalid' });
      default:
        throw new Error(`Invalid query type: ${type}`);
    }
  }

  private static generateRandomQuery(): string {
    const queries = [
      'What is the current accounts receivable balance?',
      'Show me unpaid invoices from this month',
      'What are our total expenses for Q4?',
      'List all customers with overdue payments',
      'What is our cash flow forecast?',
      'Show me journal entries for yesterday',
      'What is the current tax liability?',
      'List all vendor bills pending payment',
      'What are our year-to-date revenues?',
      'Show me bank reconciliation status',
    ];

    return faker.helpers.arrayElement(queries);
  }

  private static generateRandomVietnameseQuery(): string {
    const queries = [
      'Tổng công nợ phải thu hiện tại là bao nhiêu?',
      'Hiển thị các hóa đơn chưa thanh toán trong tháng này',
      'Tổng chi phí quý 4 là bao nhiêu?',
      'Danh sách khách hàng nợ quá hạn',
      'Dự báo dòng tiền của chúng ta là gì?',
      'Hiển thị bút toán kế toán hôm qua',
      'Nghĩa vụ thuế hiện tại là bao nhiêu?',
      'Các hóa đơn nhà cung cấp đang chờ thanh toán',
      'Doanh thu từ đầu năm đến nay là bao nhiêu?',
      'Trạng thái đối soát ngân hàng',
    ];

    return faker.helpers.arrayElement(queries);
  }

  static createTestUser(role: 'ADMIN' | 'ACCOUNTANT' | 'VIEWER' = 'VIEWER'): any {
    return {
      id: faker.string.uuid(),
      email: faker.internet.email(),
      name: faker.person.fullName(),
      role,
      companyId: this.DEFAULT_COMPANY_ID,
      createdAt: faker.date.past().toISOString(),
    };
  }

  static createTestFilters(overrides: Partial<any> = {}): any {
    return {
      module: faker.helpers.arrayElement(['ar', 'ap', 'gl', 'bank']),
      fiscalPeriod: '2024-10',
      documentType: faker.helpers.arrayElement(['invoice', 'payment', 'journal', 'bill']),
      status: faker.helpers.arrayElement(['draft', 'posted', 'paid']),
      ...overrides,
    };
  }
}