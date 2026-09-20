export interface VaultEntry {
  id: string;
  title: string;
  username: string;
  password: string;
  url: string;
  notes: string;
  folder: string;
  tags: string[];
  isFavorite: boolean;
  createdAt: number;
  updatedAt: number;
}

export type PasswordStrengthLevel = 'Very Weak' | 'Weak' | 'Fair' | 'Strong' | 'Very Strong';

export interface AuditResult {
  entryId: string;
  title: string;
  username: string;
  strength: PasswordStrengthLevel;
  score: number;
  isReused: boolean;
  reuseCount: number;
  issues: string[];
}

export interface AuditSummary {
  totalEntries: number;
  weakCount: number;
  reusedCount: number;
  strongCount: number;
  results: AuditResult[];
}

export interface AndroidFileItem {
  path: string;
  category: 'core' | 'data' | 'ui' | 'test' | 'config';
  description: string;
  content: string;
}
